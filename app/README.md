# `app/` — Directory reference

Android application module, Gradle dependencies, code shrinker rules and app build configuration.

## Direct contents

- [`.gitignore`](.gitignore) — Repository-tracked file. Review its source before assuming runtime behavior.
- [`build.gradle.kts`](build.gradle.kts) — Android module build configuration, dependencies, native build and model packaging.
- [`proguard-rules.pro`](proguard-rules.pro) — Repository-tracked file. Review its source before assuming runtime behavior.

## Immediate subfolders

- [`src/`](./src/README.md) — Android source sets, separating runtime code, instrumentation tests and JVM unit tests.

## Interfaces and architecture

See the root README's system diagram for the complete offline, Kotlin/JNI, RAG and Qwen flow.

## Verification and status

Consult the main architecture README for complete end-to-end flow.

**Documentation reflects committed sources, not a guarantee of correctness.** The Phase 4 prototype has open clinical grounding, conversation and device-validation tasks recorded in [the handoff report](../docs/PHASE4_HANDOFF_AND_RELEASE_GATES.md).
