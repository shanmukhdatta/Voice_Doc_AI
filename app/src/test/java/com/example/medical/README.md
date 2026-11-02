# `app/src/test/java/com/example/medical/` — Directory reference

Local JVM unit-test package.

## Direct contents

- [`MedicalGroundingValidatorTest.kt`](MedicalGroundingValidatorTest.kt) — Repository-tracked Kotlin source/test file. Review its source before assuming runtime behavior.
- [`NarrowSafetyInterruptTest.kt`](NarrowSafetyInterruptTest.kt) — Repository-tracked Kotlin source/test file. Review its source before assuming runtime behavior.

## Immediate subfolders

None.

## Interfaces and architecture

Updated user facts + current utterance + evidence -> Qwen3 planner -> parsed ASK/ANSWER/KNOWLEDGE_GAP/POSSIBLE_URGENT -> validator -> UI. Raw token rendering and factual grounding still have open review issues.

## Verification and status

These tests run on the host JVM; they are not evidence of handset or clinical validation.

**Documentation reflects committed sources, not a guarantee of correctness.** The Phase 4 prototype has open clinical grounding, conversation and device-validation tasks recorded in [the handoff report](../../../../../../../docs/PHASE4_HANDOFF_AND_RELEASE_GATES.md).
