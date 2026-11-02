# `app/src/main/java/com/example/voice/` — Directory reference

AudioRecord capture, Whisper speech-to-text, Android TTS and native Whisper bridge.

## Direct contents

- [`AudioRecorder.kt`](AudioRecorder.kt) — Records microphone PCM with cancellation safety.
- [`SpeechToTextEngine.kt`](SpeechToTextEngine.kt) — Whisper speech-to-text wrapper interface and engine.
- [`TextToSpeechManager.kt`](TextToSpeechManager.kt) — Android offline speech output and playback state.
- [`WhisperNative.kt`](WhisperNative.kt) — Kotlin JNI declarations for Whisper.cpp.

## Immediate subfolders

- [`llm/`](./llm/README.md) — Qwen3 inference via llama.cpp/JNI and streaming-filter utilities.

## Interfaces and architecture

AudioRecord 16 kHz PCM -> Whisper.cpp via JNI -> text; accepted final response -> Android TextToSpeech. No remote inference API.

## Verification and status

These are first-party application source files; code changes must retain data flow and privacy boundaries.

**Documentation reflects committed sources, not a guarantee of correctness.** The Phase 4 prototype has open clinical grounding, conversation and device-validation tasks recorded in [the handoff report](../../../../../../../docs/PHASE4_HANDOFF_AND_RELEASE_GATES.md).
