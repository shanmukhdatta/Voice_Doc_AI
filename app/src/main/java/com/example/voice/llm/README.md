# `app/src/main/java/com/example/voice/llm/` — Directory reference

Qwen3 inference via llama.cpp/JNI and streaming-filter utilities.

## Direct contents

- [`LlamaCppEngine.kt`](LlamaCppEngine.kt) — Native Qwen model loading, chat prompt formatting, generation stream and lifecycle.
- [`LlamaNative.kt`](LlamaNative.kt) — JNI interface to llama.cpp.
- [`LocalLlmEngine.kt`](LocalLlmEngine.kt) — Abstract local text generation interface.
- [`ThinkingTagFilter.kt`](ThinkingTagFilter.kt) — Filtering model-internal tags from streamed Qwen output.

## Immediate subfolders

None.

## Interfaces and architecture

AudioRecord 16 kHz PCM -> Whisper.cpp via JNI -> text; accepted final response -> Android TextToSpeech. No remote inference API.

## Verification and status

These are first-party application source files; code changes must retain data flow and privacy boundaries.

**Documentation reflects committed sources, not a guarantee of correctness.** The Phase 4 prototype has open clinical grounding, conversation and device-validation tasks recorded in [the handoff report](../../../../../../../../docs/PHASE4_HANDOFF_AND_RELEASE_GATES.md).
