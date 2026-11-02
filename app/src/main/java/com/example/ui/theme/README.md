# `app/src/main/java/com/example/ui/theme/` — Directory reference

Material/Compose colors, typography and visual theme configuration.

## Direct contents

- [`Color.kt`](Color.kt) — Repository-tracked Kotlin source/test file. Review its source before assuming runtime behavior.
- [`Theme.kt`](Theme.kt) — Repository-tracked Kotlin source/test file. Review its source before assuming runtime behavior.
- [`Type.kt`](Type.kt) — Repository-tracked Kotlin source/test file. Review its source before assuming runtime behavior.

## Immediate subfolders

None.

## Interfaces and architecture

Jetpack Compose input/events -> VoiceDoctorViewModel -> local orchestrator, model and storage. Display only validated medical output (open P0 code-review item).

## Verification and status

These are first-party application source files; code changes must retain data flow and privacy boundaries.

**Documentation reflects committed sources, not a guarantee of correctness.** The Phase 4 prototype has open clinical grounding, conversation and device-validation tasks recorded in [the handoff report](../../../../../../../../docs/PHASE4_HANDOFF_AND_RELEASE_GATES.md).
