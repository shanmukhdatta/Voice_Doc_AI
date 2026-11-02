# `app/src/test/java/com/example/voice/` — Directory reference

Local JVM unit-test package.

## Direct contents

- [`AudioRecorderTest.kt`](AudioRecorderTest.kt) — Repository-tracked Kotlin source/test file. Review its source before assuming runtime behavior.
- [`ThinkingTagFilterTest.kt`](ThinkingTagFilterTest.kt) — Repository-tracked Kotlin source/test file. Review its source before assuming runtime behavior.
- [`VoiceDoctorPipelineTest.kt`](VoiceDoctorPipelineTest.kt) — Repository-tracked Kotlin source/test file. Review its source before assuming runtime behavior.

## Immediate subfolders

None.

## Interfaces and architecture

AudioRecord 16 kHz PCM -> Whisper.cpp via JNI -> text; accepted final response -> Android TextToSpeech. No remote inference API.

## Verification and status

These tests run on the host JVM; they are not evidence of handset or clinical validation.

**Documentation reflects committed sources, not a guarantee of correctness.** The Phase 4 prototype has open clinical grounding, conversation and device-validation tasks recorded in [the handoff report](../../../../../../../docs/PHASE4_HANDOFF_AND_RELEASE_GATES.md).
