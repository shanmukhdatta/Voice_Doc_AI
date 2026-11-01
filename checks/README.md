# Voice Doctor AI — Hardware Testing Plans

All **physical-device verification** is deferred until a real Android handset is available. Code builds and automated tests are not substitutes for these checklists.

| Phase | Checklist | What to verify | Current status |
|---|---|---|---|
| **Phase 2** | [PHASE2_HARDWARE_CHECKS.md](./PHASE2_HARDWARE_CHECKS.md) | English microphone → Whisper.cpp Tiny Q5 → transcript → offline Android TextToSpeech; Telugu demo separation | **NOT TESTED** |
| **Phase 3** | [PHASE3_HARDWARE_CHECKS.md](./PHASE3_HARDWARE_CHECKS.md) | User-approved Qwen GGUF download; llama.cpp text generation; English Whisper → LLM → offline TTS; Room history; privacy/performance | **NOT TESTED** |

## Recommended verification order

1. Run **Phase 2** microphone, Whisper recognition and offline TTS checks first.
2. Run **Phase 3** model download and keyboard-to-LLM generation checks next.
3. Run **Phase 3** full offline voice conversation, saved history, lifecycle and privacy checks.
4. Record device details, observed outcomes, failures, fixes and retests **inside the relevant phase checklist**.
5. Do not mark a phase **GO** merely because the APK builds, automated tests pass, or an AI agent claims completion.

These files are testing plans only; adding them does not modify application functionality or mark any hardware checks complete.
