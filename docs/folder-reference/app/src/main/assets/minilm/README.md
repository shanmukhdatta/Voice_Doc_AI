# `app/src/main/assets/minilm/` — Directory reference

Quantized MiniLM ONNX embedding model and the matching WordPiece tokenizer vocabulary.

## Direct contents

- [`model_quantized.onnx`](model_quantized.onnx) — INT8 ONNX sentence-embedding weights, not a conversational LLM.
- [`vocab.txt`](vocab.txt) — Model-specific WordPiece vocabulary.

## Immediate subfolders

None.

## Interfaces and architecture

See the root README's system diagram for the complete offline, Kotlin/JNI, RAG and Qwen flow.

## Verification and status

This README is mirrored under docs/folder-reference because adding Markdown inside Android resources/assets can break or inflate the app.

**Documentation reflects committed sources, not a guarantee of correctness.** The Phase 4 prototype has open clinical grounding, conversation and device-validation tasks recorded in [the handoff report](../../../../../docs/PHASE4_HANDOFF_AND_RELEASE_GATES.md).
