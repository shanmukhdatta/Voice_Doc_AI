# `app/src/test/java/com/example/conversation/` — Directory reference

Local JVM unit-test package.

## Direct contents

- [`ContextualQueryBuilderTest.kt`](ContextualQueryBuilderTest.kt) — Repository-tracked Kotlin source/test file. Review its source before assuming runtime behavior.
- [`SessionStateManagerTest.kt`](SessionStateManagerTest.kt) — Repository-tracked Kotlin source/test file. Review its source before assuming runtime behavior.

## Immediate subfolders

None.

## Interfaces and architecture

User utterance -> pending-question resolution and user-reported facts -> contextual query -> adaptive retrieval decisions; conversation state is not trusted medical reference data.

## Verification and status

These tests run on the host JVM; they are not evidence of handset or clinical validation.

**Documentation reflects committed sources, not a guarantee of correctness.** The Phase 4 prototype has open clinical grounding, conversation and device-validation tasks recorded in [the handoff report](../../../../../../../docs/PHASE4_HANDOFF_AND_RELEASE_GATES.md).
