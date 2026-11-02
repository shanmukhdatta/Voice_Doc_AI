# `app/src/main/java/com/example/medical/` — Directory reference

Conversational RAG orchestration, existing Qwen planner, grounding validator and narrow safety interrupt.

## Direct contents

- [`ConversationalRagOrchestrator.kt`](ConversationalRagOrchestrator.kt) — Connects input, safety interrupt, state-first updates, retrieval, Qwen action generation and validation; an implementation prototype requiring integration review.
- [`MedicalGroundingValidator.kt`](MedicalGroundingValidator.kt) — Validates action shape and citation IDs; currently does NOT prove factual support and can attach a citation automatically.
- [`NarrowSafetyInterrupt.kt`](NarrowSafetyInterrupt.kt) — Conservative keyword-based interruption for potential urgent concerns; not clinically validated triage.
- [`QwenConversationPlanner.kt`](QwenConversationPlanner.kt) — Formats a bounded Qwen conversation prompt, generates and parses ASK/ANSWER/KNOWLEDGE_GAP/POSSIBLE_URGENT actions.

## Immediate subfolders

None.

## Interfaces and architecture

Updated user facts + current utterance + evidence -> Qwen3 planner -> parsed ASK/ANSWER/KNOWLEDGE_GAP/POSSIBLE_URGENT -> validator -> UI. Raw token rendering and factual grounding still have open review issues.

## Verification and status

These are first-party application source files; code changes must retain data flow and privacy boundaries.

**Documentation reflects committed sources, not a guarantee of correctness.** The Phase 4 prototype has open clinical grounding, conversation and device-validation tasks recorded in [the handoff report](../../../../../../../docs/PHASE4_HANDOFF_AND_RELEASE_GATES.md).
