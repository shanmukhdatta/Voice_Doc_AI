# `app/src/main/res/xml/` — Directory reference

Android backup rules and data-extraction policies; audit these before handling stored medical conversations.

## Direct contents

- [`backup_rules.xml`](backup_rules.xml) — Repository-tracked Android XML resource/configuration. Review its source before assuming runtime behavior.
- [`data_extraction_rules.xml`](data_extraction_rules.xml) — Repository-tracked Android XML resource/configuration. Review its source before assuming runtime behavior.

## Immediate subfolders

None.

## Interfaces and architecture

See the root README's system diagram for the complete offline, Kotlin/JNI, RAG and Qwen flow.

## Verification and status

This README is mirrored under docs/folder-reference because adding Markdown inside Android resources/assets can break or inflate the app.

**Documentation reflects committed sources, not a guarantee of correctness.** The Phase 4 prototype has open clinical grounding, conversation and device-validation tasks recorded in [the handoff report](../../../../../docs/PHASE4_HANDOFF_AND_RELEASE_GATES.md).
