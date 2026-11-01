# VOICE DOCTOR AI — PHASE 3 HARDWARE & ACCEPTANCE VERIFICATION

This document outlines the validation status for **Phase 3 (On-Device Whisper + llama.cpp + Offline TTS + Room History)**, separating what was verified automatically in code/unit tests from tests that strictly require a physical Android handset.

---

## 1. Automated & Workstation Check Status

| Check Item | Environment | Result | Description |
| :--- | :--- | :---: | :--- |
| **NDK C++ Compilation** | Windows / NDK r26c (26.3.11579264) | **PASS** | `libvoice_whisper.so` and `libvoice_llm.so` compile and link cleanly for both `arm64-v8a` and `x86_64` |
| **Symbol Collision Isolation** | Ninja / LLVM ld.lld | **PASS** | Hidden symbol visibility (`-fvisibility=hidden -fvisibility-inlines-hidden`) prevents GGML conflicts between Whisper and Llama |
| **Thinking Tag Filter** | JVM Unit Tests | **PASS** | Strips `<think>...</think>` tags across multi-token chunk boundaries and suppresses trailing newlines (5/5 tests pass) |
| **Model Repository Integrity** | Robolectric / JVM | **PASS** | Validates exact byte count (484,220,000 B) and SHA-256 (`7af3fdf8...`), handles cancel & atomic install (3/3 tests pass) |
| **Opt-in Room Database** | Robolectric SQLite | **PASS** | History is OFF by default; when enabled, writes and loads conversations with foreign keys and cascade delete (3/3 tests pass) |
| **Full Pipeline Integration** | Robolectric 4.16 (SDK 34) | **PASS** | Voice input -> STT -> LLM prompt -> Streaming tokens -> TTS speak -> Room history (6/6 tests pass) |
| **Total Test Suite** | `./gradlew.bat testDebugUnitTest` | **PASS** | **28 of 28 tests pass** |
| **Debug APK Assembly** | `./gradlew.bat assembleDebug` | **PASS** | `app-debug.apk` (76.5 MB) generated successfully |

---

## 2. Physical Handset Verification Checklist (Requires Handset)

These checks cannot be validated in emulation and require an actual Android device (recommended: arm64-v8a with >= 4 GB RAM, Android 10+).

### Step 1: Install APK on Handset
```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
adb shell pm grant com.aistudio.voicedoctor.wkqmtz android.permission.RECORD_AUDIO
```

### Step 2: One-Time Model Download (Over Wi-Fi)
- [ ] **First Launch:** Ensure device is connected to Wi-Fi.
- [ ] **Model Prompt:** Observe download banner: *"Download AI Model (~484 MB) for offline conversation"*.
- [ ] **User Approval:** Tap the banner or open Menu -> AI Model.
- [ ] **Download Progress:** Tap "Download (~484 MB)". Observe progress bar showing byte count and percentage.
- [ ] **Integrity Check:** Observe brief "Verifying SHA-256..." transition.
- [ ] **Success Card:** Observe green badge: *"Installed & Ready Offline (484.2 MB in app private storage)"*.

### Step 3: Airplane Mode Conversation Test (100% Offline)
- [ ] **Enable Airplane Mode:** Swipe down Android quick settings and enable **Airplane Mode**. Turn OFF Wi-Fi and Cellular Data.
- [ ] **Ensure Offline TTS Voice:** In Android Settings -> Accessibility / System -> Text-to-Speech output, verify an English offline speech engine (e.g. Google TTS offline English voice) is installed.
- [ ] **Hold to Talk:** Press and hold microphone button on the main screen.
- [ ] **Speak:** Say clearly: *"I have had a mild headache and fever since yesterday."*
- [ ] **Release Button:**
  - Observe waveform sheet dismiss.
  - Observe "Processing..." badge as Whisper.cpp transcribes audio locally.
  - User message bubble appears with the recognized text.
  - Doctor avatar enters thinking/speaking animation.
  - Doctor reply streams into the doctor chat bubble with clinical advice (2-4 sentences, advising hydration and emergency care for severe symptoms).
  - Internal reasoning (`<think>` tags) does **NOT** appear in the bubble.
  - Offline Android TTS speaks the completed doctor advice aloud.
- [ ] **Latency Measurement:** Note real-device time to first token (target: < 2.5s on modern mid-range devices).

### Step 4: English Typed Text Test (Offline)
- [ ] Tap the keyboard icon to switch to text mode.
- [ ] Type: *"What is the best way to soothe a sore throat?"* and tap Send.
- [ ] Verify doctor reply streams locally without internet connection.
- [ ] If sound is unmuted, verify offline TTS speaks the answer.

### Step 5: Opt-In Conversation History Verification
- [ ] Open Menu -> History (or tap About Doctor -> History).
- [ ] Verify toggle *"Save conversations on this device"* is OFF by default.
- [ ] Turn the switch ON.
- [ ] Perform a voice or text exchange.
- [ ] Open History: verify the consultation appears with user query title and timestamp.
- [ ] Tap the conversation to reload it into the active conversation sheet.
- [ ] Tap the delete trash icon to verify single conversation removal.
- [ ] Tap "Clear All History" to verify complete database wipe.

### Step 6: Telugu Demo Isolation Check
- [ ] Switch language toggle to **తెలుగు**.
- [ ] Verify status message: *"భాష తెలుగులోకి మార్చబడింది (డెమో మోడ్)"*.
- [ ] Press and hold mic: observe Telugu simulated demonstration with Telugu prompt.
- [ ] Verify Telugu mode **never** calls English Whisper or LLM silently.

---

## 3. Real-Device Logcat Diagnostics

To capture on-device runtime metrics and NDK logs during hardware testing:

```bash
# Monitor llama.cpp and whisper logs
adb logcat -s "LlamaJni" "LlamaCppEngine" "WhisperSpeechToTextEngine" "AudioRecorder" "TextToSpeechManager"
```
