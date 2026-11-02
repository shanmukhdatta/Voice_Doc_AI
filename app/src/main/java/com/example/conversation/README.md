# `app/src/main/java/com/example/conversation/` — Directory reference

Structured multi-turn user facts, pending questions, contextual query construction and adaptive retrieval decisions.

## Direct contents

- [`AdaptiveRetrievalController.kt`](AdaptiveRetrievalController.kt) — Selects RETRIEVE, REUSE_CACHE, SKIP or DEGRADED; cache invalidation/reuse requires verification.
- [`ContextualQueryBuilder.kt`](ContextualQueryBuilder.kt) — Builds medical retrieval queries from current question and earlier reported context.
- [`SessionState.kt`](SessionState.kt) — Repository-tracked Kotlin source/test file. Review its source before assuming runtime behavior.
- [`SessionStateManager.kt`](SessionStateManager.kt) — Tracks user facts, updates and pending questions; fixed symptom-list parsing is an identified limitation.

## Immediate subfolders

None.

## Interfaces and architecture

User utterance -> pending-question resolution and user-reported facts -> contextual query -> adaptive retrieval decisions; conversation state is not trusted medical reference data.

## Verification and status

These are first-party application source files; code changes must retain data flow and privacy boundaries.

**Documentation reflects committed sources, not a guarantee of correctness.** The Phase 4 prototype has open clinical grounding, conversation and device-validation tasks recorded in [the handoff report](../../../../../../../docs/PHASE4_HANDOFF_AND_RELEASE_GATES.md).
