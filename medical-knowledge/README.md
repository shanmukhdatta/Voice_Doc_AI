# `medical-knowledge/` — Directory reference

Prepared MedlinePlus corpus, vectors, SQLite index, passage mapping, source manifests and retrieval benchmarks.

## Direct contents

- [`knowledge_pack_manifest.json`](knowledge_pack_manifest.json) — Medical pack version, hashes, source and embedding compatibility metadata.
- [`medical_embeddings.bin`](medical_embeddings.bin) — Flat 384-D float32 vector matrix aligned with passage IDs.
- [`medical_knowledge.db`](medical_knowledge.db) — Prebuilt read-only SQLite FTS4 medical passage store.
- [`parsed_topics.jsonl`](parsed_topics.jsonl) — Repository-tracked file. Review its source before assuming runtime behavior.
- [`passage_ids.json`](passage_ids.json) — Position-to-passage-ID map for the binary vector matrix.
- [`passages.jsonl`](passages.jsonl) — Repository-tracked file. Review its source before assuming runtime behavior.
- [`retrieval_benchmark_results.json`](retrieval_benchmark_results.json) — Saved evaluation: hybrid currently trails vector-only on Recall@3 and MRR.
- [`sources_manifest.json`](sources_manifest.json) — Repository-tracked structured metadata or evaluation data. Review its source before assuming runtime behavior.

## Immediate subfolders

- [`raw_source/`](./raw_source/README.md) — Original downloaded XML source snapshot for reproducible parsing and source audit.

## Interfaces and architecture

See the root README's system diagram for the complete offline, Kotlin/JNI, RAG and Qwen flow.

## Verification and status

Clinical-source provenance, retrieval relevance and source-license eligibility require independent checking.

**Documentation reflects committed sources, not a guarantee of correctness.** The Phase 4 prototype has open clinical grounding, conversation and device-validation tasks recorded in [the handoff report](../docs/PHASE4_HANDOFF_AND_RELEASE_GATES.md).
