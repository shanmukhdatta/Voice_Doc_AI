# `app/src/test/java/com/example/embedding/` — Directory reference

Local JVM unit-test package.

## Direct contents

- [`WordPieceTokenizerTest.kt`](WordPieceTokenizerTest.kt) — Repository-tracked Kotlin source/test file. Review its source before assuming runtime behavior.

## Immediate subfolders

None.

## Interfaces and architecture

Text input -> WordPiece tensors -> MiniLM ONNX -> masked pooling + L2 normalization -> 384-D vector -> vector retriever.

## Verification and status

These tests run on the host JVM; they are not evidence of handset or clinical validation.

**Documentation reflects committed sources, not a guarantee of correctness.** The Phase 4 prototype has open clinical grounding, conversation and device-validation tasks recorded in [the handoff report](../../../../../../../docs/PHASE4_HANDOFF_AND_RELEASE_GATES.md).
