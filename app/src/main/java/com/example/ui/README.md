# `app/src/main/java/com/example/ui/` — Directory reference

Main Compose screen that renders app state and passes events to the ViewModel.

## Direct contents

- [`VoiceDoctorScreen.kt`](VoiceDoctorScreen.kt) — Compose interface, doctor portrait, input controls, conversation surfaces and Evidence Mode.

## Immediate subfolders

- [`theme/`](./theme/README.md) — Material/Compose colors, typography and visual theme configuration.

## Interfaces and architecture

Jetpack Compose input/events -> VoiceDoctorViewModel -> local orchestrator, model and storage. Display only validated medical output (open P0 code-review item).

## Verification and status

These are first-party application source files; code changes must retain data flow and privacy boundaries.

**Documentation reflects committed sources, not a guarantee of correctness.** The Phase 4 prototype has open clinical grounding, conversation and device-validation tasks recorded in [the handoff report](../../../../../../../docs/PHASE4_HANDOFF_AND_RELEASE_GATES.md).
