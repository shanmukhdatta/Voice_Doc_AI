# `app/src/main/` — Directory reference

Runtime Android manifest, Kotlin source, native C++ bridges and packaged app resources.

## Direct contents

- [`AndroidManifest.xml`](AndroidManifest.xml) — Repository-tracked Android XML resource/configuration. Review its source before assuming runtime behavior.

## Immediate subfolders

- [`assets/`](./assets/README.md) — Packaged offline models and medical retrieval data; copied or opened by app-private asset managers.
- [`cpp/`](./cpp/README.md) — JNI glue and CMake configuration connecting Kotlin to llama.cpp and whisper.cpp.
- [`java/`](./java/README.md) — Nested project directory.
- [`res/`](./res/README.md) — Android compiled resources; not a place for Markdown README files because AAPT expects resource file types.

## Interfaces and architecture

See the root README's system diagram for the complete offline, Kotlin/JNI, RAG and Qwen flow.

## Verification and status

Consult the main architecture README for complete end-to-end flow.

**Documentation reflects committed sources, not a guarantee of correctness.** The Phase 4 prototype has open clinical grounding, conversation and device-validation tasks recorded in [the handoff report](../../../docs/PHASE4_HANDOFF_AND_RELEASE_GATES.md).
