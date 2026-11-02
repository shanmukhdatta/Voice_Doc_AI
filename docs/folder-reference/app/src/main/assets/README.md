# `app/src/main/assets/` — Directory reference

Packaged offline models and medical retrieval data; copied or opened by app-private asset managers.

## Direct contents

No directly tracked files; contents live in subdirectories.

## Immediate subfolders

- [`medical_pack/`](./medical_pack/README.md) — Packaged read-only MedlinePlus SQLite FTS4 database, float32 vector matrix, passage IDs and integrity manifest.
- [`minilm/`](./minilm/README.md) — Quantized MiniLM ONNX embedding model and the matching WordPiece tokenizer vocabulary.
- [`models/`](./models/README.md) — Whisper Tiny Q5_1 GGML model, tracked as a Git LFS object.

## Interfaces and architecture

See the root README's system diagram for the complete offline, Kotlin/JNI, RAG and Qwen flow.

## Verification and status

This README is mirrored under docs/folder-reference because adding Markdown inside Android resources/assets can break or inflate the app.

**Documentation reflects committed sources, not a guarantee of correctness.** The Phase 4 prototype has open clinical grounding, conversation and device-validation tasks recorded in [the handoff report](../../../../docs/PHASE4_HANDOFF_AND_RELEASE_GATES.md).
