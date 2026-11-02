# `tools/` — Directory reference

Development-machine preprocessing and evaluation tools, not part of the Android runtime.

## Direct contents

No directly tracked files; contents live in subdirectories.

## Immediate subfolders

- [`medical_pack/`](./medical_pack/README.md) — Python scripts to download MedlinePlus XML, chunk and embed passages, build indexes, package and evaluate.

## Interfaces and architecture

See the root README's system diagram for the complete offline, Kotlin/JNI, RAG and Qwen flow.

## Verification and status

Python executes on the development machine; the Android app does not run a FastAPI server.

**Documentation reflects committed sources, not a guarantee of correctness.** The Phase 4 prototype has open clinical grounding, conversation and device-validation tasks recorded in [the handoff report](../docs/PHASE4_HANDOFF_AND_RELEASE_GATES.md).
