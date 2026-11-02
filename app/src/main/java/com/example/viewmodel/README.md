# `app/src/main/java/com/example/viewmodel/` — Directory reference

The lifecycle-aware bridge between UI, audio, RAG, Qwen, Room and TTS.

## Direct contents

- [`VoiceDoctorViewModel.kt`](VoiceDoctorViewModel.kt) — Owns event handling and chat UI state; calls the orchestrator and TTS. Existing raw token display before validation is a documented P0 issue.

## Immediate subfolders

None.

## Interfaces and architecture

Jetpack Compose input/events -> VoiceDoctorViewModel -> local orchestrator, model and storage. Display only validated medical output (open P0 code-review item).

## Verification and status

These are first-party application source files; code changes must retain data flow and privacy boundaries.

**Documentation reflects committed sources, not a guarantee of correctness.** The Phase 4 prototype has open clinical grounding, conversation and device-validation tasks recorded in [the handoff report](../../../../../../../docs/PHASE4_HANDOFF_AND_RELEASE_GATES.md).
