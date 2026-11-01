# Voice Doctor AI — Phase 2 Implementation Report

## 1. Executive Summary

Phase 2 transforms the Voice Doctor AI prototype from a simulated UI into a genuinely functional, completely offline **English speech-to-text-to-speech application**. 

All recognition and synthesis occur 100% on-device with zero network requests:
1. The user presses and holds the microphone button.
2. `AudioRecord` captures raw 16 kHz Mono PCM audio.
3. Upon release, the audio is converted to normalized floats and transcribed by `Whisper.cpp` using the quantized `ggml-tiny-q5_1.bin` model (~31 MiB) compiled with ARM NEON / x86 CPU optimizations.
4. The recognized English text is posted to the conversation as both the user's transcript and the doctor's echo response.
5. Android `TextToSpeech` speaks the recognized sentence using an offline English voice (preferring Indian English `en-IN` offline).
6. The interface returns to idle, ready for the next interaction.

---

## 2. Architecture Overview

```
[ User Interaction ]
         |
         v
 [ VoiceInputBar ] (Compose stable pointerInput handler)
         |
         v
 [ AudioRecorder ] (16 kHz Mono PCM 16-bit, in-memory buffer, max 30s)
         |
         | Normalized FloatArray [-1.0f, 1.0f]
         v
[ WhisperSpeechToTextEngine ] (Single-threaded Dispatcher)
         |
         v (JNI Bridge)
[ libvoice_whisper.so ] (whisper.cpp v1.7.4 + GGML CPU backend)
         |
         | Recognized English Text
         v
[ VoiceDoctorViewModel ] (State machine & Conversation StateFlow)
         |
   +-----+-----+
   |           |
   v           v
[ Chat UI ]  [ TextToSpeechManager ]
(Compose)    (android.speech.tts.TextToSpeech, offline en-IN preference)
               |
               v
             [ Doctor Speaking Animation ]
```

---

## 3. Implemented Components & Features

### A. Real Audio Input (`voice/AudioRecorder.kt`)
- **API**: `android.media.AudioRecord` configured for `VOICE_RECOGNITION`.
- **Audio Format**: 16,000 Hz, Mono, 16-bit PCM.
- **Conversion**: Converts signed PCM16 shorts to normalized floats in `[-1.0f, 1.0f]`.
- **Duration Cap**: Automatically caps recording at 30 seconds (480,000 samples).
- **Amplitude Feed**: Computes real-time RMS amplitude normalized to `[0.0, 1.0]` and feeds it to `RecordingSheet.kt` for live waveform pulsation.
- **Cancellation Safety**: Background coroutine with clean cancellation, instant hardware release, and memory buffer discard.
- **Runtime Permission**: Handled via `ActivityResultContracts.RequestPermission()` for `android.permission.RECORD_AUDIO`.

### B. Compose Gesture Fix (`components/VoiceInputBar.kt`)
- Fixed pointer handler by decoupling pointer observation key from `isRecording` (`pointerInput(Unit)`).
- Bound callbacks with `rememberUpdatedState` to prevent coroutine restarts and premature cancellation during touch press-hold-release cycles.

### C. Native Whisper.cpp Integration (`cpp/` & `voice/WhisperNative.kt`)
- **Engine**: Official `whisper.cpp` v1.7.4 with GGML CPU backend.
- **Compilation**: Built via CMake 3.22.1 and NDK r26c (26.3.11579264).
- **Target ABIs**: `arm64-v8a` (with `__ARM_NEON` and `-O3`) and `x86_64` (for emulator compatibility).
- **JNI Bridge**: `whisper_jni.cpp` providing:
  - `init(modelPath: String): Long`
  - `initFromAsset(assetManager, assetPath): Long`
  - `transcribe(contextHandle, samples, language, numThreads): String`
  - `release(contextHandle: Long)`
  - `getSystemInfo(): String`
- **Concurrency Protection**: Dedicated single-thread coroutine dispatcher (`WhisperSpeechToTextEngine`) ensuring thread safety and preventing concurrent native execution collisions.
- **Silence & Noise Gate**: Filters audio with low energy (< 0.01 peak) and audio under 0.3 seconds to avoid hallucinating text on background silence.

### D. Model Packaging & Verification
- **Model**: `ggml-tiny-q5_1.bin`
- **Parameters**: Tiny, Multilingual, Q5_1 quantization.
- **File Size**: 32,152,673 bytes (~30.66 MiB).
- **SHA256**: `818710568da3ca15689e31a743197b520007872ff9576237bda97bd1b469c3d7`
- **Location**: Packaged in Android assets at `models/ggml-tiny-q5_1.bin`.
- **Build Task**: Added `ensureWhisperModel` Gradle task in `app/build.gradle.kts` that verifies model existence and checksum during `preBuild`, downloading it if absent.

### E. Offline Android Text-To-Speech (`voice/TextToSpeechManager.kt`)
- **API**: Standard `android.speech.tts.TextToSpeech`.
- **Offline Voice Detection**: Queries `engine.voices` and prioritizes non-network voices (`!voice.isNetworkConnectionRequired`).
- **Locale Preference**: `en-IN` (English India) offline voice preferred, followed by offline fallback English voices (`en-US`, `en-GB`), then standard locale fallback.
- **Lifecycle Tracking**: `UtteranceProgressListener` detects real speech playback start and end, driving `DoctorUiState.Speaking` to `DoctorUiState.Idle` without artificial timers.
- **Mute Integration**: Seamless real-time mute/unmute control; stops ongoing audio playback immediately on mute.

### F. Language Separation
- **English**: Fully live pipeline (`AudioRecord` -> `Whisper.cpp` -> `Echo UI` -> `Android TTS`).
- **Telugu (తెలుగు)**: Clearly maintained in demonstration mode with localized prompts and status banner indicating that full offline Telugu recognition is scheduled for a future release.

---

## 4. Build Environment & Requirements

- **JDK**: OpenJDK 17.0.20.1
- **Gradle**: 9.3.1 (via bundled wrapper)
- **Android Gradle Plugin**: 9.1.1
- **Android SDK Platform**: android-36 (compileSdk 36.1, targetSdk 36, minSdk 24)
- **Android SDK Build-Tools**: 36.0.0
- **Android NDK**: 26.3.11579264 (r26c)
- **CMake**: 3.22.1

---

## 5. Verification & Test Results

### Automated Unit & Integration Tests
Executed via `./gradlew.bat testDebugUnitTest`:

| Test Suite | Test Case | Status |
|---|---|---|
| `AudioRecorderTest` | `testPcm16ToNormalizedFloatConversion` | **PASSED** |
| `AudioRecorderTest` | `testInitialState` | **PASSED** |
| `AudioRecorderTest` | `testSafeCancelWhenIdle` | **PASSED** |
| `AudioRecorderTest` | `testStopWhenIdleReturnsEmptyArray` | **PASSED** |
| `AudioRecorderTest` | `testMultipleSequentialReleasesDoNotThrow` | **PASSED** |
| `VoiceDoctorPipelineTest` | `testLiveEnglishEchoPipeline` | **PASSED** |
| `VoiceDoctorPipelineTest` | `testCancellationDiscardsAudioAndReturnsToIdle` | **PASSED** |
| `VoiceDoctorPipelineTest` | `testEmptyOrTooShortAudioDoesNotFabricateTranscript` | **PASSED** |
| `VoiceDoctorPipelineTest` | `testTeluguDemoModeMaintainsSafeSeparation` | **PASSED** |
| `VoiceDoctorPipelineTest` | `testMuteToggleStopsSpeech` | **PASSED** |
| `ExampleRobolectricTest` | `read string from context` | **PASSED** |
| `ExampleRobolectricTest` | `test initial state and sound toggle` | **PASSED** |
| `ExampleRobolectricTest` | `test language toggle updates messages and state` | **PASSED** |
| `ExampleRobolectricTest` | `test recording flow state transition in telugu demo mode` | **PASSED** |
| `ExampleRobolectricTest` | `test clear and restore conversation` | **PASSED** |
| `ExampleUnitTest` | `addition_isCorrect` | **PASSED** |

**Total Tests**: 16 executed, 16 passed, 0 failed.

### Build Verification
- Task: `./gradlew.bat assembleDebug`
- Status: **BUILD SUCCESSFUL**
- Output APK: `app/build/outputs/apk/debug/app-debug.apk`
- APK Size: 59,882,245 bytes (~57.1 MB)
- Verified APK Contents:
  - `lib/arm64-v8a/libvoice_whisper.so` (Present)
  - `lib/x86_64/libvoice_whisper.so` (Present)
  - `assets/models/ggml-tiny-q5_1.bin` (Present, 32.15 MB)

---

## 6. Physical Device Testing Instructions

To test on physical hardware:

1. **Install APK**:
   ```bash
   adb install -r app/build/outputs/apk/debug/app-debug.apk
   ```
2. **Offline / Airplane Mode Test**:
   - Enable **Airplane Mode** on the device.
   - Disable Wi-Fi and Cellular Data.
   - Launch "Voice Doctor AI".
3. **Verify Audio Pipeline**:
   - Grant microphone permission when prompted.
   - Press and hold the microphone button at the bottom.
   - Speak: *"Hello doctor, how are you today?"*
   - Notice the waveform reacting to voice intensity.
   - Release the button.
   - Verify that Whisper transcribes the speech locally within 1-2 seconds.
   - Verify that the recognized text appears in the conversation bubbles.
   - Verify that Android TextToSpeech speaks the recognized text offline.
   - Verify the doctor illustration displays Listening, Processing, and Speaking animations matching playback.
4. **Verify Telugu Demo Separation**:
   - Tap the language toggle to switch to **తెలుగు**.
   - Notice the demo mode banner.
   - Hold to talk — notice the demo mode behavior is preserved without executing English ASR.

> [!NOTE]
> Physical device and hardware airplane-mode validation could not be executed directly in the host environment because no physical Android handset was attached via ADB. All unit, integration, native compilation, and packaging tests completed successfully.

---

## 7. Known Issues and Future Improvements

1. **Telugu Speech Recognition**: Phase 2 focuses exclusively on English offline ASR. Real multilingual Telugu ASR will be added in Phase 3.
2. **Medical Diagnosis LLM**: Phase 2 operates as a voice echo test pipeline. Medical diagnosis generation via on-device LLM will be introduced in subsequent phases.
3. **GPU / NPU Acceleration**: Current inference runs on the ARM CPU backend with NEON SIMD optimizations. Future optimizations can explore QNN or Vulkan delegates for lower latency.
