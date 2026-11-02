# `app/src/main/java/com/example/rag/` — Directory reference

Medical passage types, SQLite keyword search, exact vector search, RRF fusion and offline pack management.

## Direct contents

- [`EvidenceBundle.kt`](EvidenceBundle.kt) — Passage collection and evidence availability/status contract.
- [`EvidencePassage.kt`](EvidencePassage.kt) — Identifiable source passage data for prompts and citation UI.
- [`HybridRetriever.kt`](HybridRetriever.kt) — Runs MiniLM vector search, SQLite lexical retrieval, and RRF fusion; benchmark hybrid quality currently lags vector-only.
- [`KeywordRetriever.kt`](KeywordRetriever.kt) — Small adapter for SQLite full-text lexical candidates.
- [`MedicalKnowledgeStore.kt`](MedicalKnowledgeStore.kt) — Opens the read-only medical SQLite database and queries passage rows / FTS4 index.
- [`MedicalPackManager.kt`](MedicalPackManager.kt) — Unpacks medical DB and vector assets; manifest hash checks still need hardening.
- [`VectorRetriever.kt`](VectorRetriever.kt) — Loads 384-D float vectors from the compact VEC1 index and performs exact cosine/dot-product ranking.

## Immediate subfolders

None.

## Interfaces and architecture

Contextual query -> embedding + exact vector matching, plus SQLite FTS4 -> RRF -> passage IDs and evidence bundle -> Qwen planner.

## Verification and status

These are first-party application source files; code changes must retain data flow and privacy boundaries.

**Documentation reflects committed sources, not a guarantee of correctness.** The Phase 4 prototype has open clinical grounding, conversation and device-validation tasks recorded in [the handoff report](../../../../../../../docs/PHASE4_HANDOFF_AND_RELEASE_GATES.md).
