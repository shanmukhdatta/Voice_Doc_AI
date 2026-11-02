# `app/src/main/java/com/example/components/` — Directory reference

Reusable doctor-themed Jetpack Compose UI widgets, chat sheets, voice input and model download screens.

## Direct contents

- [`AboutDoctorDialog.kt`](AboutDoctorDialog.kt) — Project information and Evidence Mode controls.
- [`ConversationHistorySheet.kt`](ConversationHistorySheet.kt) — Repository-tracked Kotlin source/test file. Review its source before assuming runtime behavior.
- [`ConversationPreview.kt`](ConversationPreview.kt) — Repository-tracked Kotlin source/test file. Review its source before assuming runtime behavior.
- [`ConversationSheet.kt`](ConversationSheet.kt) — Repository-tracked Kotlin source/test file. Review its source before assuming runtime behavior.
- [`DoctorAvatar.kt`](DoctorAvatar.kt) — Doctor visual/animation presentation; a later 3D half-body animation is not implemented.
- [`HeaderBar.kt`](HeaderBar.kt) — Repository-tracked Kotlin source/test file. Review its source before assuming runtime behavior.
- [`MessageBubble.kt`](MessageBubble.kt) — Renders user/doctor messages and citation chips.
- [`ModelDownloadDialog.kt`](ModelDownloadDialog.kt) — Repository-tracked Kotlin source/test file. Review its source before assuming runtime behavior.
- [`QuickPromptsSheet.kt`](QuickPromptsSheet.kt) — Repository-tracked Kotlin source/test file. Review its source before assuming runtime behavior.
- [`RecordingSheet.kt`](RecordingSheet.kt) — Repository-tracked Kotlin source/test file. Review its source before assuming runtime behavior.
- [`VoiceInputBar.kt`](VoiceInputBar.kt) — Repository-tracked Kotlin source/test file. Review its source before assuming runtime behavior.

## Immediate subfolders

None.

## Interfaces and architecture

Jetpack Compose input/events -> VoiceDoctorViewModel -> local orchestrator, model and storage. Display only validated medical output (open P0 code-review item).

## Verification and status

These are first-party application source files; code changes must retain data flow and privacy boundaries.

**Documentation reflects committed sources, not a guarantee of correctness.** The Phase 4 prototype has open clinical grounding, conversation and device-validation tasks recorded in [the handoff report](../../../../../../../docs/PHASE4_HANDOFF_AND_RELEASE_GATES.md).
