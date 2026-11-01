# Voice Doctor AI — Phase 2 Hardware Verification Checklist

**Status:** NOT STARTED — all hardware checks below must be executed on a physical Android device.  
**Repository:** https://github.com/shanmukhdatta/Voice_Doc_AI  
**Implementation branch:** `feature/offline-voice-baseline`  
**Scope:** English offline microphone → whisper.cpp Tiny Q5 → visible transcript → Android TextToSpeech speech echo. Telugu remains **DEMO ONLY**.  
**Related implementation report:** [PHASE2_IMPLEMENTATION.md](../PHASE2_IMPLEMENTATION.md)

> **Testing rule:** Never mark a hardware test as passed from a successful Gradle build, emulator test, Robolectric test, or AI-agent assertion. Record observations from the actual handset. Leave unchecked items unchecked; use **PASS / FAIL / BLOCKED / NOT RUN** in the log. Do not use real patient data, actual medical histories, or sensitive recordings in test evidence.

## 0. Test session record (fill before testing)

| Field | Record |
|---|---|
| Test date/time (IST) | TODO |
| Tester | TODO |
| Phone model | TODO |
| Android version / API level | TODO |
| Device CPU ABI (`adb shell getprop ro.product.cpu.abi`) | TODO |
| Git commit SHA / branch | TODO |
| APK version / build type | TODO |
| APK size | TODO |
| Whisper model | `ggml-tiny-q5_1.bin`, multilingual Tiny Q5_1 |
| Whisper model SHA-256 (if locally checked) | TODO |
| Installed Android TTS engine / voice | TODO |
| Offline English voice installed before airplane mode? | YES / NO |
| Airplane mode + Wi-Fi off verified? | YES / NO |
| Starting battery / available storage | TODO |
| Links to non-sensitive logs/screenshots | TODO |

**Expected implementation as of Phase 2:** NDK `26.3.11579264`, CMake `3.22.1`, Whisper.cpp `v1.7.4`, English real STT, Telugu demo UI. These are implementation claims from the project report; verify the installed APK and model.

## 1. Prepare a reproducible build and installation

- [ ] Check out the correct implementation branch (or the merged commit containing Phase 2) and record the commit hash.
- [ ] If using a fresh clone, initialize Git LFS and fetch the **real binary** model (not an LFS pointer); confirm a file around 31 MiB.
- [ ] Verify Android SDK, NDK, CMake and JDK prerequisites; build using the repository's Gradle wrapper.
- [ ] Run `gradlew.bat testDebugUnitTest assembleDebug` on Windows (or `./gradlew testDebugUnitTest assembleDebug` on Linux/macOS). Record actual test totals and exit status.
- [ ] Check that the APK includes `lib/arm64-v8a/libvoice_whisper.so` and `assets/models/ggml-tiny-q5_1.bin`.
- [ ] Enable developer options and USB debugging on the test phone; connect it via USB.
- [ ] Run `adb devices`, verify the phone appears as **device**, and record its ABI and OS version.
- [ ] Install or update the APK using `adb install -r app/build/outputs/apk/debug/app-debug.apk`. If signing differs from the installed app, document the conflict and preserve data before any uninstall.
- [ ] Launch the app; confirm the existing doctor, pill input, chat, language selector and mute control render correctly without crashes.
- [ ] Confirm the APK is running the expected Git commit and not an older installed build.

**Windows commands (from the repository root):**

```powershell
git status --short
git rev-parse HEAD
git lfs pull
.\gradlew.bat testDebugUnitTest assembleDebug
adb devices
adb shell getprop ro.product.cpu.abi
adb shell getprop ro.build.version.release
adb install -r app\build\outputs\apk\debug\app-debug.apk
```

If `adb` is not on PATH, use the Android SDK's `platform-tools\adb.exe`. Do not copy a private test recording into the public repository.

## 2. English microphone capture — real hardware

Start with **English** selected. Grant microphone permission only when the app requests it.

- [ ] **MIC-01 Permission granted:** First microphone press requests permission, then recording can start once granted. No fake recording state if the permission was denied.
- [ ] **MIC-02 Hold gesture:** Press and hold for 3–5 seconds; recording indicator and listening state stay active throughout the hold.
- [ ] **MIC-03 Real waveform:** Waveform amplitude changes when speaking versus sitting silently (if connected to RMS amplitude).
- [ ] **MIC-04 Release:** Release finger; capture stops once and the app transitions into transcription. No premature submission caused by recomposition or recording-sheet changes.
- [ ] **MIC-05 Cancel:** Cancel a recording; nothing gets transcribed, appended or spoken. Repeat with the phone's Back button.
- [ ] **MIC-06 Short clip:** Release in under 0.3 seconds; app shows an appropriate short-audio message rather than hallucinating text.
- [ ] **MIC-07 Silence:** Hold 3–5 seconds in a quiet room without speech; no fabricated transcript or doctor response.
- [ ] **MIC-08 Max duration:** Hold until the configured 30-second limit; recording stops cleanly and remains usable for a new request.
- [ ] **MIC-09 Denied permission:** Revoke microphone permission in Android Settings; retry and confirm a user-friendly explanation without a crash.
- [ ] **MIC-10 Repeated use:** Perform 10 normal press–release cycles without a stuck microphone, duplicate messages, resource leak or freeze.
- [ ] **MIC-11 Interruptions:** Background the app / lock screen / switch away during capture; verify microphone is released and the app recovers.
- [ ] **MIC-12 Competing app:** Try recording while another app is using the mic; verify graceful failure/recovery if the OS does not grant access.

## 3. Whisper.cpp real transcription — English

Do not accept stubbed test strings as evidence: transcription must come from actual microphone samples and the bundled model. Select **English** throughout. Compare recognized text with the original sentence; record substantial errors without pasting sensitive speech into public logs.

| ID | Test utterance (neutral and safe) | Actual recognized text / error | PASS / FAIL / BLOCKED |
|---|---|---|---|
| STT-01 | Hello doctor, how are you today? | TODO | NOT RUN |
| STT-02 | Please repeat what I just said. | TODO | NOT RUN |
| STT-03 | I am testing the microphone on my phone. | TODO | NOT RUN |
| STT-04 | Today is Sunday and the weather is pleasant. | TODO | NOT RUN |
| STT-05 | The meeting begins at nine thirty. | TODO | NOT RUN |
| STT-06 | One, two, three, four, five. | TODO | NOT RUN |
| STT-07 | I would like a glass of water. | TODO | NOT RUN |
| STT-08 | Can you understand my English clearly? | TODO | NOT RUN |
| STT-09 | There are three books on the table. | TODO | NOT RUN |
| STT-10 | Thank you for helping me test this app. | TODO | NOT RUN |

- [ ] **STT-11 Model load:** Native library and model initialize on device; no JNI load/link error, invalid handle, or memory crash.
- [ ] **STT-12 No fabricated input:** Speech text changes appropriately for distinct utterances; it is not cycling through old hardcoded sample strings.
- [ ] **STT-13 Language:** English is explicitly selected for inference; no unexpected translation into another language.
- [ ] **STT-14 Different conditions:** Repeat at least three utterances at normal voice volume, quieter speech, and moderate background noise; record differences.
- [ ] **STT-15 Errors:** Capture and document meaningful error messages for missing model/native failure without false success claims.
- [ ] **STT-16 Timing:** Measure end-of-press to transcription display latency for at least 5 recordings. Do not assume the 1–2 second estimate from the implementation report.

**Quality notes:** Tiny Q5 is a baseline, not a guarantee of robust noisy speech or Telugu accuracy. Record troublesome words, clipped starts/ends, hallucinations and whether larger Whisper models may be needed later.

## 4. Text-to-speech — real offline speech echo

Before airplane mode, check Android Settings → Text-to-speech output for installed offline English voice data. Then **disable Wi-Fi and mobile data** and enable airplane mode.

- [ ] **TTS-01 Offline voice confirmed:** App selects an installed English voice whose `isNetworkConnectionRequired` is false. Record selected engine and voice.
- [ ] **TTS-02 No network fallback:** If only network-required English voices exist, app shows an explicit unsupported/unavailable state and **does not invoke online TTS**.
- [ ] **TTS-03 Echo:** Speak an English phrase, release, and confirm the doctor reads the **same text Whisper recognized** rather than a medical advice template.
- [ ] **TTS-04 Display:** User transcript and visibly labeled echo appear in the conversation; no misleading clinical diagnosis is generated.
- [ ] **TTS-05 Speech lifecycle:** Speaking animation/status begins when TTS actually starts and returns to idle on its completion/error callback.
- [ ] **TTS-06 Mute:** Toggle mute before a response; text still appears but no audio plays.
- [ ] **TTS-07 Mute during playback:** Toggle mute mid-utterance; playback stops promptly and UI is not stuck in Speaking.
- [ ] **TTS-08 Interrupt speech:** Press-and-hold to start a new recording while previous speech is playing; previous output stops and new capture begins.
- [ ] **TTS-09 No offline voice:** Simulate/verify missing offline voice handling; text works and a clear setup/error message appears without a cloud fallback.
- [ ] **TTS-10 Repeat:** Speak 5 consecutive different transcripts; confirm output does not skip, duplicate, or mix utterances.

**Required code-review warning:** The currently reported `TextToSpeechManager.kt` included fallback selection of a possibly network-required `en-IN` voice, and a locale-based availability flag that did not prove offline availability. Fix this behavior before marking **TTS-01/TTS-02** PASS.

## 5. Full airplane-mode integration

- [ ] **OFF-01 Cold launch:** Force-stop the app, enable airplane mode, explicitly verify Wi-Fi/mobile data are off, then cold-launch without network.
- [ ] **OFF-02 Entire round trip:** Microphone → PCM → JNI Whisper Tiny Q5 → transcript shown → offline TTS audible → Idle, with no internet.
- [ ] **OFF-03 Five distinct rounds:** Run five different short English utterances in a row; all complete or show honest errors; no permanent stuck state.
- [ ] **OFF-04 Restore app:** Exit and relaunch the app offline; model still loads and voice pipeline still works.
- [ ] **OFF-05 Permission recovery:** Revoke and re-grant microphone permission; app recovers properly.
- [ ] **OFF-06 No cloud dependence:** Confirm no network API, Firebase inference or browser-based speech service is required by the voice round trip.
- [ ] **OFF-07 No private logs:** Confirm actual transcripts/recordings are not written to verbose production logs and recordings are not unexpectedly retained.
- [ ] **OFF-08 Telugu demo separation:** Switch to తెలుగు; a visible demo indication appears, and the app does **not** imply real Telugu Whisper recognition or offline Telugu TTS has been verified.

**Important:** Unit tests using fake STT/TTS services only validate application wiring. They do not satisfy OFF-02.

## 6. Reliability and performance measurements

Use at least 5 English utterances of varying lengths. Note actual device conditions.

| Trial | Audio length (s) | STT time from release (s) | TTS start delay (s) | Result / notes |
|---|---:|---:|---:|---|
| 1 | TODO | TODO | TODO | NOT RUN |
| 2 | TODO | TODO | TODO | NOT RUN |
| 3 | TODO | TODO | TODO | NOT RUN |
| 4 | TODO | TODO | TODO | NOT RUN |
| 5 | TODO | TODO | TODO | NOT RUN |

- [ ] **PERF-01** Record model initialization duration on a cold launch.
- [ ] **PERF-02** Record latency for 3-second and 10-second utterances; calculate median time.
- [ ] **PERF-03** Record approximate peak RAM, CPU load and battery/temperature symptoms after 10 consecutive interactions.
- [ ] **PERF-04** Verify actual APK size, model size and app installed storage (note model extracted to app-private storage may add another copy).
- [ ] **PERF-05** Verify acceptable UI responsiveness during Whisper native inference.
- [ ] **PERF-06** Re-test after an app restart and after a longer session; no progressive slowdown, native crash or leaking microphone.
- [ ] **PERF-07** Verify clean behavior when the system is low on memory or terminates the app (as reasonably testable).

### Troubleshooting and safe evidence capture

Collect Android logs only if needed. Example on Windows:

```powershell
adb logcat -d -s WhisperJNI WhisperSTT AudioRecorder TextToSpeechManager AndroidRuntime
```

Before sharing logs, **redact all speech text, personal details, identifiers and tokens**. The current STT implementation was observed logging `Result: '$cleanText'`; remove/redact this transcript logging for a privacy-safe medical app.

If microphone capture stops inconsistently, review `AudioRecorder.stopRecording()` and cancellation ordering: the recorder job may still be exiting when the audio resource/buffer is accessed. Investigate with repeat tests and fix race conditions rather than hiding them behind retries.

If no offline TTS voice exists, record **BLOCKED**, install offline voice data while online, return to airplane mode, and repeat the test. Never mark an online-only speech test as an offline pass.

## 7. Failure log / defects (fill as encountered)

| ID | Date | Test ID | Expected | Observed | Severity | Fix commit / retest | Status |
|---|---|---|---|---|---|---|---|
| BUG-001 | TODO | TODO | TODO | TODO | TODO | TODO | OPEN |

Severity: **BLOCKER** (crash, cloud dependency, no voice round-trip, false transcription); **MAJOR** (unreliable controls, repeated failure); **MINOR** (cosmetic); **INFO** (measurement/limitation). Do not commit identifiable patient information as evidence.

## 8. Phase 2 sign-off (do not pre-check)

- [ ] Build and automated tests have been re-run from the recorded Git commit.
- [ ] Physical microphone capture, cancel, permission and repeat tests pass.
- [ ] Real English Whisper Tiny Q5 inference succeeds on a physical device.
- [ ] The recognized text is displayed correctly and read aloud.
- [ ] TTS uses a confirmed **offline** voice, with no network fallback.
- [ ] Five consecutive end-to-end airplane-mode voice round trips succeed.
- [ ] No native crash, stuck recording, serious audio race or unintended data retention.
- [ ] Telugu is explicitly demo-only.
- [ ] High-priority issues are fixed, re-tested and documented.
- [ ] The test log identifies handset, APK, commit SHA, environment, findings and remaining limitations.

**Decision:** `NOT TESTED / NO-GO / CONDITIONAL GO / GO`  
**Tester/date:** TODO  
**Passing tests:** TODO  
**Failed or blocked tests:** TODO  
**Highest-priority outstanding issues:** TODO  
**Evidence / fix commit(s):** TODO

**GO to Phase 3** only after the real English offline echo pipeline works repeatedly on the test phone and no blocker remains. A **conditional go** may permit independent LLM prototyping on a separate branch, but does **not** imply that Phase 2 has passed hardware acceptance.

## 9. Future work — explicitly not part of this hardware gate

- Real Telugu Whisper recognition and corresponding offline Telugu TTS.
- STT model comparisons: Tiny Q5 vs Base Q5/Base/Small, with real English/Telugu and noisy-speech benchmarks.
- Local llama.cpp model inference, then safety/evidence-grounded medical RAG.
- More natural neural speech and the half-body animated 3D doctor avatar.
- Source-verified medical safety, uncertainty handling, citations and clinical evaluation.

Leave these future items out of Phase 2 sign-off.
