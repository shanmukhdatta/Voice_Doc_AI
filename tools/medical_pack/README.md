# `tools/medical_pack/` — Directory reference

Python scripts to download MedlinePlus XML, chunk and embed passages, build indexes, package and evaluate.

## Direct contents

- [`build_chunks.py`](build_chunks.py) — Creates compact section-aware passages from source topics.
- [`build_lexical_index.py`](build_lexical_index.py) — Builds medical SQLite metadata and FTS4 keyword index.
- [`build_vector_index.py`](build_vector_index.py) — Embeds passages and serializes VEC1 flat vectors with passage ID correspondence.
- [`download_source.py`](download_source.py) — Downloads official MedlinePlus XML snapshot for the offline corpus.
- [`evaluate_retrieval.py`](evaluate_retrieval.py) — Measures vector-only and hybrid rankings on evaluation queries.
- [`export_minilm_onnx.py`](export_minilm_onnx.py) — Exports a pinned MiniLM model to ONNX.
- [`generate_eval_conversations.py`](generate_eval_conversations.py) — Generates multi-turn conversation evaluation cases.
- [`generate_eval_queries.py`](generate_eval_queries.py) — Generates retrieval evaluation queries; examine whether labels are independent.
- [`package_pack.py`](package_pack.py) — Builds the versioned pack asset manifest.
- [`parse_medlineplus.py`](parse_medlineplus.py) — Parses eligible English MedlinePlus topics into structured rows.
- [`pyproject.toml`](pyproject.toml) — Repository-tracked file. Review its source before assuming runtime behavior.
- [`quantize_minilm.py`](quantize_minilm.py) — Performs INT8 quantization and prepares parity fixtures.
- [`requirements.lock`](requirements.lock) — Development Python dependencies; these do not ship in the Android APK.
- [`verify_pack.py`](verify_pack.py) — Verifies prepared pack artifacts and manifest.

## Immediate subfolders

- [`test_fixtures/`](./test_fixtures/README.md) — Golden embedding/tokenizer fixtures for parity tests.

## Interfaces and architecture

Updated user facts + current utterance + evidence -> Qwen3 planner -> parsed ASK/ANSWER/KNOWLEDGE_GAP/POSSIBLE_URGENT -> validator -> UI. Raw token rendering and factual grounding still have open review issues.

## Verification and status

Python executes on the development machine; the Android app does not run a FastAPI server.

**Documentation reflects committed sources, not a guarantee of correctness.** The Phase 4 prototype has open clinical grounding, conversation and device-validation tasks recorded in [the handoff report](../../docs/PHASE4_HANDOFF_AND_RELEASE_GATES.md).
