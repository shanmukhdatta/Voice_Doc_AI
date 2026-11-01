# Voice Doctor AI — Phase 3 Hardware & Acceptance Checks

**Status:** NOT TESTED — hardware tests are deferred until the project is run on an actual Android phone.  
**Repository:** https://github.com/shanmukhdatta/Voice_Doc_AI  
**Phase 3 branch:** `feature/offline-llm-phase3`  
**Implementation commit reported:** `4c3342468fc0989ef93ef58747507369a89bdb30`  
**Earlier phase:** See `checks/PHASE2_HARDWARE_CHECKS.md` for the original microphone → Whisper → TTS baseline.  
**Phase 3 additions:** llama.cpp, Qwen3-0.6B Q4_K_M, user-approved Hugging Face download, streaming model answers, Room/SQLite local history.  
**Scope:** English real inference; Telugu remains **demo only**.

> **Important:** The implementation report claims 28 automated tests passed and a debug APK was built. Those are **build/test claims, not physical hardware verification**. Do not mark any hardware check completed without observing the behavior on a real Android device. Do not use real patients' information as test data.

## 0. Test session details — fill in when testing begins

| Field | Value |
|---|---|
| Date, tester | TODO |
| Phone, Android/API version, CPU ABI | TODO |
| Installed APK version/build and Git commit | TODO |
| Free storage before download / after model install | TODO |
| Phone available RAM and battery | TODO |
| Network used only for model download | TODO |
| Hugging Face model filename / verified SHA-256 | TODO |
| Android TTS engine and offline English voice | TODO |
| Test log link (redacted) | TODO |
| Final decision | NOT TESTED |

## 1. Build, APK and installation

- [ ] **SETUP-01** Checkout the actual Phase 3 commit and verify the branch/commit SHA.
- [ ] **SETUP-02** Ensure Git LFS has downloaded actual Whisper model bytes; no LFS pointer remains.
- [ ] **SETUP-03** Run `gradlew.bat testDebugUnitTest assembleDebug` in the Android project and record real exit codes, test counts and APK file size.
- [ ] **SETUP-04** Verify `arm64-v8a` APK contents include both Whisper and llama.cpp native libraries and the bundled Whisper model.
- [ ] **SETUP-05** Install the debug APK on the handset with ADB or Android Studio; check for signing errors.
- [ ] **SETUP-06** Confirm existing doctor avatar, hold-to-talk control, mute, keyboard input, EN/TE toggle, conversation sheet and recording UI work unchanged.
- [ ] **SETUP-07** Cold-start the app three times and verify no startup crash, native linking failure or ANR.

Windows command reference:

```powershell
git status --short
git rev-parse HEAD
git lfs pull
.\gradlew.bat testDebugUnitTest assembleDebug
adb devices
adb shell getprop ro.product.cpu.abi
adb install -r app\build\outputs\apk\debug\app-debug.apk
```

## 2. Qwen model manager and first-time download

The reported target is `Qwen3-0.6B.Q4_K_M.gguf` (~484.2 MB), stored in private app storage. Before testing, independently validate the model source, file size, exact checksum and licensing.

- [ ] **MODEL-01** On first launch, app shows the model as absent and offers a clear download action.
- [ ] **MODEL-02** No model download begins without a real user action; the user sees approximate download and storage requirements.
- [ ] **MODEL-03** With user approval and network available, download progress updates correctly.
- [ ] **MODEL-04** Verify final GGUF size and SHA-256 against authoritative model metadata; reject corrupt or truncated files.
- [ ] **MODEL-05** Cancel a partial download: no broken model is accepted as installed; no runaway background transfer.
- [ ] **MODEL-06** Interrupt connectivity midway and test retry. Record whether download **resumes or restarts**; do not mark resume supported unless observed.
- [ ] **MODEL-07** Restart the app: a previously valid model is recognized **without downloading again**.
- [ ] **MODEL-08** Test corrupt same-size model handling; file size alone must **not** be trusted as an integrity check.
- [ ] **MODEL-09** Download and startup errors show useful messages instead of hanging indefinitely.
- [ ] **MODEL-10** Delete model through the UI; verify storage is reclaimed and the inference engine is unloaded safely.
- [ ] **MODEL-11** Model download is the only internet-dependent part of the normal Phase 3 setup; inference itself performs no network request.

## 3. Text → llama.cpp → streamed reply (test this before microphone)

Start with **English keyboard input**, using only neutral/non-sensitive test prompts.

| ID | Example prompt | Observed response | Status |
|---|---|---|---|
| LLM-01 | `Hello, what can you do?` | TODO | NOT RUN |
| LLM-02 | `Explain what a thermometer measures in one sentence.` | TODO | NOT RUN |
| LLM-03 | `List three ways to keep drinking water clean.` | TODO | NOT RUN |
| LLM-04 | `What was the first question I asked?` (within same chat) | TODO | NOT RUN |
| LLM-05 | `Please answer in two short sentences.` | TODO | NOT RUN |

- [ ] **LLM-06** Native `libvoice_llm.so` loads without missing symbols or a GGML collision with `libvoice_whisper.so`.
- [ ] **LLM-07** The downloaded model initializes and produces **real** generated tokens (not fake test output).
- [ ] **LLM-08** Generated text appears incrementally in the existing assistant bubble, without duplicated or scrambled text.
- [ ] **LLM-09** The final answer is coherent English and does not contain `<think>`, `</think>`, or malformed model control tokens.
- [ ] **LLM-10** Confirm correct model chat-template formatting, explicit non-thinking mode (if supported) and appropriate sampling settings.
- [ ] **LLM-11** Send a longer prompt (>512 prompt tokens); verify batching/context handling prevents native out-of-bounds writes, crashes or hangs.
- [ ] **LLM-12** Confirm the model receives recent prior turns where expected; test follow-up questions that require context.
- [ ] **LLM-13** Excessive prompt length is rejected or trimmed safely within the configured context window, with no app crash.
- [ ] **LLM-14** A new request during an ongoing response cancels/serializes generation cleanly; no two calls corrupt the same native context.
- [ ] **LLM-15** Failures return a visibly labeled error, not a fabricated response that appears to come from the model.
- [ ] **LLM-16** Unload/reload and app restarts do not crash, leak models, or produce stale text.

**Code-review flags to resolve before sign-off:** The implementation has a batch initialized for 512 tokens; native prompt ingestion must never exceed that capacity. The previous review also found the prompt contained only the latest user turn, not a true multi-turn conversation window.

## 4. Offline English voice → Whisper → Qwen → Android TTS

Install/verify an offline English Android TTS voice **before** turning the network off. Then use airplane mode with Wi-Fi and mobile data off.

- [ ] **VOICE-01** Press-and-hold captures actual microphone audio without permission or touch-event problems.
- [ ] **VOICE-02** Whisper recognizes the spoken phrase as a user message.
- [ ] **VOICE-03** The recognized text, not the audio bytes, is passed into the local LLM prompt.
- [ ] **VOICE-04** A new LLM response is generated and visibly streamed into the doctor chat bubble.
- [ ] **VOICE-05** After generation completes, Android TTS speaks the **generated response**, not the user's original phrase.
- [ ] **VOICE-06** Confirm the selected TTS voice works offline, never falling back to a network-required voice.
- [ ] **VOICE-07** The doctor UI follows Recording → Transcribing → Thinking → Speaking → Idle with real lifecycle signals.
- [ ] **VOICE-08** Five different short voice queries complete successfully in airplane mode.
- [ ] **VOICE-09** Mute works before and during playback; no speech is generated when muted, but the reply is still shown.
- [ ] **VOICE-10** Cancel a recording: no transcription, LLM response, TTS, or new history entry.
- [ ] **VOICE-11** Interrupt an answer and speak again; no stuck audio, native crash or overlapping TTS.
- [ ] **VOICE-12** Telugu remains clearly indicated as **demo mode**, not real Telugu STT/LLM/TTS.

## 5. Room/SQLite local conversation history

- [ ] **DB-01** History saving is OFF by default on a fresh install.
- [ ] **DB-02** With saving OFF, chat is usable but conversations are not persisted after process termination.
- [ ] **DB-03** Opt in and save a conversation containing at least **three user/assistant turns**.
- [ ] **DB-04** Close/relaunch and reopen the conversation; **all turns** and their chronological order are present.
- [ ] **DB-05** A second and third save to the same conversation **do not delete earlier messages**.
- [ ] **DB-06** Create two distinct conversations and verify they do not mix.
- [ ] **DB-07** Delete one conversation; other conversations remain unchanged.
- [ ] **DB-08** Clear all history; the local Room database contains no remaining conversation messages.
- [ ] **DB-09** Switch history saving OFF and verify future messages are not persisted; document whether previously saved records remain.
- [ ] **DB-10** Confirm app backup/data extraction rules exclude sensitive conversation data from cloud backups and transfers as intended.
- [ ] **DB-11** Evaluate whether stored conversation content needs app-level encryption and document the actual protection level; do not falsely claim encryption.
- [ ] **DB-12** Raw microphone recordings are not saved by default; no sensitive transcript text appears in app debug logs used for demonstrations.

**Code-review flag:** The Phase 3 DAO uses `OnConflictStrategy.REPLACE` for a parent conversation entity with cascading child messages. Check that repeated saves cannot delete prior history. Repair before DB acceptance.

## 6. Performance and device stability

Measure on the real phone, not with workstation estimates.

| Trial | Input mode / length | Whisper latency | LLM load time | First-token latency | Tokens/sec | TTS start delay | RAM / notes |
|---|---|---|---|---|---|---|---|
| 1 | Text: short | N/A | TODO | TODO | TODO | TODO | TODO |
| 2 | Text: follow-up | N/A | TODO | TODO | TODO | TODO | TODO |
| 3 | Voice: ~3 s | TODO | TODO | TODO | TODO | TODO | TODO |
| 4 | Voice: ~10 s | TODO | TODO | TODO | TODO | TODO | TODO |
| 5 | Voice: second session | TODO | TODO | TODO | TODO | TODO | TODO |

- [ ] **PERF-01** Report actual app installed storage after model download and Whisper model extraction.
- [ ] **PERF-02** Measure CPU and memory peaks with **Whisper and LLM both loaded**.
- [ ] **PERF-03** Verify UI remains responsive while model initialization and inference execute.
- [ ] **PERF-04** Ten consecutive text generations complete without ANRs, memory growth or native crashes.
- [ ] **PERF-05** Ten voice exchanges complete without stuck microphone, TTS deadlock or crash.
- [ ] **PERF-06** App lifecycle: background, foreground, screen lock, process termination and clean restart behave safely.
- [ ] **PERF-07** Model deletion/unloading during or after inference is synchronized and safe.
- [ ] **PERF-08** Measure battery drain, thermal throttling and response latency; record device and ambient conditions.

## 7. Safety and accuracy smoke checks

These are **software-safety smoke tests, not medical efficacy or clinical validation**. Keep examples non-personal.

- [ ] **SAFE-01** The UI clearly says this is an AI assistant for general health information, not a replacement for a clinician.
- [ ] **SAFE-02** Unsupported diagnoses, prescriptions and high-stakes clinical recommendations are not presented as verified medical conclusions.
- [ ] **SAFE-03** When uncertain, the system communicates limitations and encourages qualified medical evaluation where appropriate.
- [ ] **SAFE-04** Emergency-sounding requests are redirected toward immediate local emergency help, without delaying users with prolonged generated advice.
- [ ] **SAFE-05** A small model's hallucinations and errors are documented; the system is not described as medically validated merely because the inference pipeline works.
- [ ] **SAFE-06** Model output shown in the chat does not include internal reasoning tags or fake source citations.

## 8. Failure log — record when tests are actually run

| Bug ID | Test ID | Expected | Observed | Severity | Fix reference | Retest |
|---|---|---|---|---|---|---|
| TODO | TODO | TODO | TODO | TODO | TODO | NOT RUN |

Severity: **BLOCKER** (crash, fake completion, unverified online dependency, wrong/no inference); **HIGH** (history corruption, unsafe TTS, context or privacy problems); **MEDIUM** (performance/recovery issues); **LOW** (cosmetic).

## 9. Final Phase 3 sign-off

- [ ] The recorded APK commit matches the implemented native libraries.
- [ ] Download is voluntary, verified, persisted, recoverable, and does not repeat unnecessarily.
- [ ] Real Qwen on-device generation works from **typed English** questions.
- [ ] Real English **microphone → Whisper → LLM → TTS** works in airplane mode.
- [ ] Chat displays both the actual user transcription and generated response.
- [ ] Local history opt-in, multi-turn persistence, deletion and privacy checks pass.
- [ ] Native buffer handling, generation cancellation and model memory lifecycle are safe.
- [ ] High-severity issues are fixed and retested.
- [ ] Results, device details, timings and known limitations are recorded without private information.

**Final decision:** `NOT TESTED / NO-GO / CONDITIONAL GO / GO`  
**Tester and test date:** TODO  
**Outstanding issues:** TODO  
**Relevant fix commit(s):** TODO

A successful build or simulated unit test does **not** by itself make Phase 3 hardware-verified. This checklist intentionally remains unchecked until physical testing is performed.