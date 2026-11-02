# `app/src/main/java/com/example/data/` — Directory reference

Opt-in Room/SQLite conversation entities, DAO, database singleton and repository; separate from the medical corpus.

## Direct contents

- [`ChatMessageEntity.kt`](ChatMessageEntity.kt) — Child message row with role, content, source and timestamp.
- [`ConversationDao.kt`](ConversationDao.kt) — Room insert, update, query and delete contracts for conversations and messages.
- [`ConversationEntity.kt`](ConversationEntity.kt) — Database parent row for one chat conversation.
- [`ConversationRepository.kt`](ConversationRepository.kt) — Opt-in chat storage abstraction using Room and SharedPreferences.
- [`VoiceDoctorDatabase.kt`](VoiceDoctorDatabase.kt) — Room database singleton for personal history; not the trusted medical FTS database.

## Immediate subfolders

None.

## Interfaces and architecture

User opt-in -> ConversationRepository -> Room entities/DAO -> app-private SQLite; medical corpus is separate.

## Verification and status

These are first-party application source files; code changes must retain data flow and privacy boundaries.

**Documentation reflects committed sources, not a guarantee of correctness.** The Phase 4 prototype has open clinical grounding, conversation and device-validation tasks recorded in [the handoff report](../../../../../../../docs/PHASE4_HANDOFF_AND_RELEASE_GATES.md).
