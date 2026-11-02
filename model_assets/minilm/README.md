# `model_assets/minilm/` — Directory reference

Reproducible generated INT8 MiniLM ONNX model, tokenizer vocabulary and export metadata.

## Direct contents

- [`export_metadata.json`](export_metadata.json) — Repository-tracked structured metadata or evaluation data. Review its source before assuming runtime behavior.
- [`model_quantized.onnx`](model_quantized.onnx) — INT8 ONNX sentence-embedding weights, not a conversational LLM.
- [`quantization_metadata.json`](quantization_metadata.json) — Repository-tracked structured metadata or evaluation data. Review its source before assuming runtime behavior.
- [`vocab.txt`](vocab.txt) — Model-specific WordPiece vocabulary.

## Immediate subfolders

None.

## Interfaces and architecture

See the root README's system diagram for the complete offline, Kotlin/JNI, RAG and Qwen flow.

## Verification and status

Consult the main architecture README for complete end-to-end flow.

**Documentation reflects committed sources, not a guarantee of correctness.** The Phase 4 prototype has open clinical grounding, conversation and device-validation tasks recorded in [the handoff report](../../docs/PHASE4_HANDOFF_AND_RELEASE_GATES.md).
