# `tools/medical_pack/test_fixtures/` — Directory reference

Golden embedding/tokenizer fixtures for parity tests.

## Direct contents

- [`embedding_parity_fixtures.json`](embedding_parity_fixtures.json) — Repository-tracked structured metadata or evaluation data. Review its source before assuming runtime behavior.

## Immediate subfolders

None.

## Interfaces and architecture

Updated user facts + current utterance + evidence -> Qwen3 planner -> parsed ASK/ANSWER/KNOWLEDGE_GAP/POSSIBLE_URGENT -> validator -> UI. Raw token rendering and factual grounding still have open review issues.

## Verification and status

Python executes on the development machine; the Android app does not run a FastAPI server.

**Documentation reflects committed sources, not a guarantee of correctness.** The Phase 4 prototype has open clinical grounding, conversation and device-validation tasks recorded in [the handoff report](../../../docs/PHASE4_HANDOFF_AND_RELEASE_GATES.md).
