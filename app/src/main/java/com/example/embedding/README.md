# `app/src/main/java/com/example/embedding/` — Directory reference

Android ONNX Runtime MiniLM inference, WordPiece encoding, asset installation and vector interface.

## Direct contents

- [`EmbeddingAssetManager.kt`](EmbeddingAssetManager.kt) — Loads/extracts ONNX and tokenizer assets to private storage; verify all hashes.
- [`MiniLmOnnxEmbeddingEngine.kt`](MiniLmOnnxEmbeddingEngine.kt) — Executes quantized ONNX MiniLM with attention-masked mean pooling and L2 normalization.
- [`TextEmbeddingEngine.kt`](TextEmbeddingEngine.kt) — Embedding interface for 384-element normalized query vectors.
- [`WordPieceTokenizer.kt`](WordPieceTokenizer.kt) — Kotlin BERT-style WordPiece tokenization matching MiniLM vocabulary.

## Immediate subfolders

None.

## Interfaces and architecture

Text input -> WordPiece tensors -> MiniLM ONNX -> masked pooling + L2 normalization -> 384-D vector -> vector retriever.

## Verification and status

These are first-party application source files; code changes must retain data flow and privacy boundaries.

**Documentation reflects committed sources, not a guarantee of correctness.** The Phase 4 prototype has open clinical grounding, conversation and device-validation tasks recorded in [the handoff report](../../../../../../../docs/PHASE4_HANDOFF_AND_RELEASE_GATES.md).
