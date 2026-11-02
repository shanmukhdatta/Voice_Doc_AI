# `model_assets/` — Directory reference

Development-machine model export and quantization outputs; distinct from packaged Android assets.

## Direct contents

No directly tracked files; contents live in subdirectories.

## Immediate subfolders

- [`minilm/`](./minilm/README.md) — Reproducible generated INT8 MiniLM ONNX model, tokenizer vocabulary and export metadata.

## Interfaces and architecture

See the root README's system diagram for the complete offline, Kotlin/JNI, RAG and Qwen flow.

## Verification and status

Consult the main architecture README for complete end-to-end flow.

**Documentation reflects committed sources, not a guarantee of correctness.** The Phase 4 prototype has open clinical grounding, conversation and device-validation tasks recorded in [the handoff report](../docs/PHASE4_HANDOFF_AND_RELEASE_GATES.md).
