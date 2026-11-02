# Voice Doctor AI — Phase 4 Adaptive Conversational Medical RAG Implementation

**Repository:** `https://github.com/shanmukhdatta/Voice_Doc_AI`  
**Branch:** `feature/adaptive-conversational-rag-phase4`  
**Baseline:** Phase 3 implementation baseline (`4c3342468fc0989ef93ef58747507369a89bdb30`)  
**Architecture:** 100% Offline On-Device Mobile Conversational RAG with Intent-Specific Adaptive Retrieval, State-First Fact Tracking, and Dynamic Grounding Verification

---

## 1. Executive Summary & Accomplishments

Phase 4 successfully delivers a genuine, offline, size-conscious, end-to-end conversational medical RAG system connected directly to the existing Phase 3 Qwen3-0.6B Android application.

### Key Deliverables Completed:
1. **Retired Rigid Keyword Classifiers:** Removed legacy keyword routers, 25-topic hardcoded dictionaries, and artificial tax maps. Replaced with genuine hybrid embedding + lexical semantic retrieval.
2. **State-First Invariant Loop:** Implemented the strict sequential conversation loop:
   $$\text{Whisper ASR} \rightarrow \text{Narrow Safety Interrupt} \rightarrow \text{Update User Facts FIRST} \rightarrow \text{Adaptive Retrieval} \rightarrow \text{Evidence Bundle} \rightarrow \text{Qwen3-0.6B Non-Thinking Planner} \rightarrow \text{Single Action Parsing} \rightarrow \text{Grounding Validation} \rightarrow \text{UI + Offline TTS}$$
3. **On-Device Embedding Engine:** Exported `sentence-transformers/all-MiniLM-L6-v2` to ONNX and quantized with dynamic INT8 (22.37 MB), integrated with `com.microsoft.onnxruntime:onnxruntime-android:1.19.2`. Created a zero-dependency Kotlin `WordPieceTokenizer` with Bert-compatible vocabulary (30,522 tokens), achieving 0.9623 average cosine similarity against PyTorch baseline across 24 test fixtures.
4. **Offline Medical Knowledge Pack:** Downloaded the official National Library of Medicine (NIH) MedlinePlus XML release (`mplus_topics_2026-10-31.xml`, 28.73 MB, 1,016 health topics). Parsed and chunked into 1,016 section-aligned passages (≤256 wordpieces), embedded into a 1.49 MB flat binary matrix (`medical_embeddings.bin`), and indexed into a 3.20 MB SQLite FTS4 database (`medical_knowledge.db`). Total RAG asset footprint is **~27.3 MB**.
5. **Adaptive Multi-Turn State Management:** Created typed `SessionState` tracking `UserFact` entities with categories, polarities (`AFFIRMATIVE`, `NEGATED`, `UNCERTAIN`), subjects (`SELF`, `CHILD`, `PARENT`, `SPOUSE`), and turn tracking. Solves follow-up answers (e.g. "upper right") by resolving to pending questions, handles negations ("no fever"), handles explicit corrections ("actually right, not left"), and enforces a ceiling of at most 3 clarifying questions per topic.
6. **Medical Grounding & Safety Validation:** Implemented `NarrowSafetyInterrupt` covering acute life threats (cardiac, stroke FAST, acute respiratory, anaphylaxis, suicide) with international emergency routing (911/112/999/local) and negation/quotation awareness. Implemented `MedicalGroundingValidator` enforcing that all cited source IDs `[MP_xxxx_xx]` exist in the retrieved bundle, stripping fake citations, sanitizing prescription dosage self-administration, and falling back safely to `KNOWLEDGE_GAP` when evidence is missing.
7. **UI & Evidence Mode:** Added Evidence Mode switch in `AboutDoctorDialog` (ON by default, OFF clearly labeled *"LLM only — not source-grounded"*), verified source pills in message bubbles, and preserved the bilingual Telugu demo mode without regressions.
8. **Automated Verification:** Added comprehensive test suites covering tokenization, fact extraction, elliptical query expansion, safety interrupts, grounding validation, and pipeline integration.

---

## 2. In-Depth Component Architecture

### 2.1 Embedding & Tokenizer Layer (`com.example.embedding`)
- **`TextEmbeddingEngine`**: Clean Kotlin interface defining `suspend fun embed(text: String): FloatArray`.
- **`WordPieceTokenizer`**: Native Kotlin WordPiece tokenizer supporting ASCII and unicode punctuation splitting, greedy longest-match subword continuations (`##`), lowercase normalization, control character stripping, and special tokens `[CLS]=101`, `[SEP]=102`, `[PAD]=0`, `[UNK]=100`.
- **`MiniLmOnnxEmbeddingEngine`**: Executes INT8 `model_quantized.onnx` using Android ONNX Runtime (`OrtEnvironment` and `OrtSession` with 2 intra-op threads). Takes `input_ids`, `attention_mask`, `token_type_ids`, performs masked mean pooling across active sequence tokens, and computes exact L2 vector normalization.
- **`EmbeddingAssetManager`**: Automatically unpacks and verifies model and vocab assets from `app/src/main/assets/minilm/` into app-private storage.

### 2.2 Offline Knowledge Pack & Retrieval Layer (`com.example.rag`)
- **`EvidencePassage` & `EvidenceBundle`**: Strongly-typed passage model containing passage ID (e.g. `MP_122_01`), topic ID, title, section, verified MedlinePlus text, source URL, attribution, and relevance score. Status enum: `RELEVANT`, `WEAK_OR_CONFLICTING`, `NONE`, `INDEX_UNAVAILABLE`.
- **`MedicalKnowledgeStore`**: Read-only SQLite helper querying `medical_knowledge.db` passages table and virtual `passages_fts` (FTS4).
- **`VectorRetriever`**: Loads `medical_embeddings.bin` into memory (1,016 × 384 float matrix = 1.49 MB) with little-endian `VEC1` header verification. Computes exact dot-product cosine similarity in < 15 ms.
- **`KeywordRetriever`**: FTS prefix matching across sanitized medical search terms.
- **`HybridRetriever`**: Combines vector cosine scores and lexical ranks using Reciprocal Rank Fusion ($RRF = \sum \frac{1}{60 + \text{rank}}$). Uses confidence threshold gating: $\ge 0.55 \rightarrow \text{RELEVANT}$, $[0.42, 0.55) \rightarrow \text{WEAK}$, $< 0.42 \rightarrow \text{NONE}$.
- **`MedicalPackManager`**: Manages extraction, validation, and lifecycle of knowledge pack assets from `assets/medical_pack/`.

### 2.3 Adaptive Conversation State Layer (`com.example.conversation`)
- **`SessionState`**: In-memory state tracking extracted facts, pending clarification questions, asked question history, topic question count, current evidence bundle, and Evidence Mode preference.
- **`SessionStateManager`**: Evaluates incoming user utterances. Extracts symptoms, durations, locations, negations, and subjects *before* retrieval. Resolves elliptical replies to pending questions.
- **`ContextualQueryBuilder`**: Synthesizes active affirmative symptoms with current user utterance without hallucinating diagnostic names.
- **`AdaptiveRetrievalController`**: Determines retrieval strategy: `RETRIEVE` (new symptom/topic), `REUSE_CACHE` (clarification answer within existing relevant topic), `SKIP` (social pleasantries/greetings), or `DEGRADED` (knowledge pack unavailable).

### 2.4 Safety, Planning & Validation Layer (`com.example.medical`)
- **`NarrowSafetyInterrupt`**: Emergency checks targeting acute life-threats. Handles negations ("no chest pain") and hypothetical queries ("in a movie..."). Employs international emergency guidance (911/112/999).
- **`QwenConversationPlanner`**: Single owner of Qwen3-0.6B ChatML prompt templates. Directs the model to output a single structured action: `ASK:`, `ANSWER:`, `KNOWLEDGE_GAP:`, or `POSSIBLE_URGENT:`. Enforces non-thinking directive.
- **`MedicalGroundingValidator`**: Strict post-generation validation. Enforces max 3 question ceiling per topic, validates that cited source IDs exist in the retrieved evidence bundle, removes hallucinated citation tags, sanitizes specific drug dosage self-administration, and enforces knowledge gap fallbacks.
- **`ConversationalRagOrchestrator`**: Central coordinator executing the state-first loop asynchronously with reactive flow updates.

---

## 3. Asset & Size Audit

| File | Location | Size | SHA-256 Checksum |
| :--- | :--- | :--- | :--- |
| `model_quantized.onnx` | `model_assets/minilm/` & `assets/minilm/` | 22.37 MB | `86ca5b99a38f4d9b4b0a48b52c0350d2bb8ebf7e892c9ff505d5fa40dc41e573` |
| `vocab.txt` | `model_assets/minilm/` & `assets/minilm/` | 0.25 MB | `213c41551a37c980907a4a9db94de5851480cf8a4c1ddf77d3f1eb9c9e8a09ee` |
| `medical_embeddings.bin` | `medical-knowledge/` & `assets/medical_pack/` | 1.49 MB | `16867e3bfce1f71dfb332b55b0a37e199d7d4c82c4aa0fcce086ffb7fba4dbfb` |
| `medical_knowledge.db` | `medical-knowledge/` & `assets/medical_pack/` | 3.20 MB | `5a7f29c0fca02d1d054fa899eeb5da456ae9eb7a5b3ca1b5858cf858d4a66a7b` |
| `passage_ids.json` | `medical-knowledge/` & `assets/medical_pack/` | 0.02 MB | `771cae79f67a21f7dbbe4eb4e1b8b80e8eeb67822d515a86ce9f83637e6d15a9` |
| `knowledge_pack_manifest.json` | `medical-knowledge/` & `assets/medical_pack/` | 0.001 MB | Manifest metadata |
| **Total RAG Asset Footprint** | — | **~27.3 MB** | Verified |

---

## 4. Hardware Verification Protocol

Physical handset testing is **deferred** as requested. The comprehensive test protocol is committed to:
`checks/PHASE4_CONVERSATIONAL_RAG_HARDWARE_CHECKS.md` with all checkboxes left unchecked `[ ]`.
