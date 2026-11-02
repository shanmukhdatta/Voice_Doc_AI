# `app/src/main/java/com/example/` — Directory reference

Android entry Activity and all first-party Kotlin packages.

## Direct contents

- [`MainActivity.kt`](MainActivity.kt) — Android Activity entry and Compose content host.

## Immediate subfolders

- [`components/`](./components/README.md) — Reusable doctor-themed Jetpack Compose UI widgets, chat sheets, voice input and model download screens.
- [`conversation/`](./conversation/README.md) — Structured multi-turn user facts, pending questions, contextual query construction and adaptive retrieval decisions.
- [`data/`](./data/README.md) — Opt-in Room/SQLite conversation entities, DAO, database singleton and repository; separate from the medical corpus.
- [`embedding/`](./embedding/README.md) — Android ONNX Runtime MiniLM inference, WordPiece encoding, asset installation and vector interface.
- [`medical/`](./medical/README.md) — Conversational RAG orchestration, existing Qwen planner, grounding validator and narrow safety interrupt.
- [`model/`](./model/README.md) — UI-level immutable state and chat/domain models.
- [`modelmanager/`](./modelmanager/README.md) — HTTPS Qwen GGUF download manager with progress and integrity checks.
- [`rag/`](./rag/README.md) — Medical passage types, SQLite keyword search, exact vector search, RRF fusion and offline pack management.
- [`ui/`](./ui/README.md) — Main Compose screen that renders app state and passes events to the ViewModel.
- [`viewmodel/`](./viewmodel/README.md) — The lifecycle-aware bridge between UI, audio, RAG, Qwen, Room and TTS.
- [`voice/`](./voice/README.md) — AudioRecord capture, Whisper speech-to-text, Android TTS and native Whisper bridge.

## Interfaces and architecture

See the root README's system diagram for the complete offline, Kotlin/JNI, RAG and Qwen flow.

## Verification and status

These are first-party application source files; code changes must retain data flow and privacy boundaries.

**Documentation reflects committed sources, not a guarantee of correctness.** The Phase 4 prototype has open clinical grounding, conversation and device-validation tasks recorded in [the handoff report](../../../../../../docs/PHASE4_HANDOFF_AND_RELEASE_GATES.md).
