# `app/src/main/res/` — Directory reference

Android compiled resources; not a place for Markdown README files because AAPT expects resource file types.

## Direct contents

No directly tracked files; contents live in subdirectories.

## Immediate subfolders

- [`drawable/`](./drawable/README.md) — App doctor-avatar imagery and XML vector drawable components.
- [`mipmap-anydpi-v26/`](./mipmap-anydpi-v26/README.md) — Nested launcher resource directory.
- [`mipmap-hdpi/`](./mipmap-hdpi/README.md) — Nested launcher resource directory.
- [`mipmap-mdpi/`](./mipmap-mdpi/README.md) — Nested launcher resource directory.
- [`mipmap-xhdpi/`](./mipmap-xhdpi/README.md) — Nested launcher resource directory.
- [`mipmap-xxhdpi/`](./mipmap-xxhdpi/README.md) — Nested launcher resource directory.
- [`mipmap-xxxhdpi/`](./mipmap-xxxhdpi/README.md) — Nested launcher resource directory.
- [`values-te/`](./values-te/README.md) — Telugu UI demonstration strings; live Telugu ASR/RAG remains future work.
- [`values/`](./values/README.md) — Base strings, colors and themes.
- [`xml/`](./xml/README.md) — Android backup rules and data-extraction policies; audit these before handling stored medical conversations.

## Interfaces and architecture

See the root README's system diagram for the complete offline, Kotlin/JNI, RAG and Qwen flow.

## Verification and status

This README is mirrored under docs/folder-reference because adding Markdown inside Android resources/assets can break or inflate the app.

**Documentation reflects committed sources, not a guarantee of correctness.** The Phase 4 prototype has open clinical grounding, conversation and device-validation tasks recorded in [the handoff report](../../../../docs/PHASE4_HANDOFF_AND_RELEASE_GATES.md).
