# `app/src/main/assets/medical_pack/` — Directory reference

Packaged read-only MedlinePlus SQLite FTS4 database, float32 vector matrix, passage IDs and integrity manifest.

## Direct contents

- [`knowledge_pack_manifest.json`](knowledge_pack_manifest.json) — Medical pack version, hashes, source and embedding compatibility metadata.
- [`medical_embeddings.bin`](medical_embeddings.bin) — Flat 384-D float32 vector matrix aligned with passage IDs.
- [`medical_knowledge.db`](medical_knowledge.db) — Prebuilt read-only SQLite FTS4 medical passage store.
- [`passage_ids.json`](passage_ids.json) — Position-to-passage-ID map for the binary vector matrix.

## Immediate subfolders

None.

## Interfaces and architecture

Updated user facts + current utterance + evidence -> Qwen3 planner -> parsed ASK/ANSWER/KNOWLEDGE_GAP/POSSIBLE_URGENT -> validator -> UI. Raw token rendering and factual grounding still have open review issues.

## Verification and status

This README is mirrored under docs/folder-reference because adding Markdown inside Android resources/assets can break or inflate the app.

**Documentation reflects committed sources, not a guarantee of correctness.** The Phase 4 prototype has open clinical grounding, conversation and device-validation tasks recorded in [the handoff report](../../../../../docs/PHASE4_HANDOFF_AND_RELEASE_GATES.md).
