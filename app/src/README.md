# `app/src/` — Directory reference

Android source sets, separating runtime code, instrumentation tests and JVM unit tests.

## Direct contents

No directly tracked files; contents live in subdirectories.

## Immediate subfolders

- [`androidTest/`](./androidTest/README.md) — On-device/instrumentation Android tests.
- [`main/`](./main/README.md) — Runtime Android manifest, Kotlin source, native C++ bridges and packaged app resources.
- [`test/`](./test/README.md) — JVM unit-test source root, including mocked Android interaction tests.

## Interfaces and architecture

See the root README's system diagram for the complete offline, Kotlin/JNI, RAG and Qwen flow.

## Verification and status

Consult the main architecture README for complete end-to-end flow.

**Documentation reflects committed sources, not a guarantee of correctness.** The Phase 4 prototype has open clinical grounding, conversation and device-validation tasks recorded in [the handoff report](../../docs/PHASE4_HANDOFF_AND_RELEASE_GATES.md).
