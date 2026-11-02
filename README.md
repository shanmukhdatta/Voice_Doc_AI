# Voice Doctor AI

> **An offline-first, voice-first, privacy-conscious Android health-information assistant with adaptive conversational retrieval-augmented generation (RAG).** A user can describe a concern in natural language; the assistant remembers relevant user-reported context, retrieves licensed medical reference passages locally, asks useful follow-up questions when needed, and responds with cautious, source-attributed educational information through chat and optional speech.

**Repository:** https://github.com/shanmukhdatta/Voice_Doc_AI  
**Architecture:** Offline Adaptive Conversational Medical RAG  
**Platform:** Native Android (Kotlin, Jetpack Compose, C++/JNI)  
**Real-language scope:** English; Telugu remains a clearly labeled demonstration in the present baseline.  
**Important status:** This README specifies the **target architecture**, not a claim that all Phase 4 components have been implemented, medically validated, or tested on a physical phone.

> [!IMPORTANT]
> Voice Doctor AI is a research and educational software project. It is **not a doctor, diagnostic device, certified triage system, prescription service, or replacement for a qualified healthcare professional**. Source retrieval, model output, and automated tests do not establish clinical safety. For potentially urgent symptoms, users should seek appropriate local emergency assistance rather than wait for chatbot questioning.

---

## Project website and folder documentation

**Static GitHub Pages showcase:** https://shanmukhdatta.github.io/Voice_Doc_AI/ (availability is subject to successful Pages activation and deployment).

- [Website source and deployment instructions](site/README.md)
- [Phase 4 first-party folder-by-folder documentation](https://github.com/shanmukhdatta/Voice_Doc_AI/blob/feature/adaptive-conversational-rag-phase4/docs/FOLDER_INDEX.md)
- [Current Phase 4 implementation branch](https://github.com/shanmukhdatta/Voice_Doc_AI/tree/feature/adaptive-conversational-rag-phase4)
- [Known validation gaps and cross-laptop handoff](https://github.com/shanmukhdatta/Voice_Doc_AI/blob/feature/adaptive-conversational-rag-phase4/docs/PHASE4_HANDOFF_AND_RELEASE_GATES.md)

> The public site is an informational portfolio demonstration, not a medical-chat web app. Source code and tests do not establish clinical safety.

---


## 00. Read this first: implementation truth and branch separation

This repository contains several generations of work. **Do not mistake the old Phase 4 prototype for the newly finalized Phase 4 architecture.**

| Track | Repository branch | What it represents | Status |
|---|---|---|---|
| Baseline | [main](https://github.com/shanmukhdatta/Voice_Doc_AI/tree/main) | Repository landing branch and documented project direction | Source code may lag feature branches |
| Speech baseline | [feature/offline-voice-baseline](https://github.com/shanmukhdatta/Voice_Doc_AI/tree/feature/offline-voice-baseline) | Android microphone, Whisper.cpp STT, offline TTS | Implemented; physical-device checklist pending |
| Local LLM | [feature/offline-llm-phase3](https://github.com/shanmukhdatta/Voice_Doc_AI/tree/feature/offline-llm-phase3) | Existing Qwen3-0.6B / llama.cpp inference, download manager, opt-in Room history | Implemented; physical-device checklist pending |
| **Superseded Phase 4** | [feature/medical-safety-phase4](https://github.com/shanmukhdatta/Voice_Doc_AI/tree/feature/medical-safety-phase4) | Rule-heavy symptom/request classifier and a 25-topic keyword pilot | **Historical experiment; not the target architecture** |
| **New Phase 4** | Planned: feature/adaptive-conversational-rag-phase4 | Full offline **adaptive conversational hybrid RAG** | **Design finalized; verify branch and commits before claiming implementation** |

The original product-vision repository [VoiceDoc](https://github.com/shanmukhdatta/VoiceDoc) is an additional architectural reference; **this repository is the implementation target**. Build reports on feature branches describe the state of those branches at the time they were written; they do not automatically describe main.

### Project principles

1. **One existing small LLM:** Qwen3-0.6B handles planning, question generation, and educational answer generation. Do not add another large conversational model.
2. **Meaning over keyword lists:** Hybrid retrieval uses a compact semantic embedder alongside lexical search. Avoid a large hardcoded symptom/request taxonomy.
3. **Conversation, not one-shot Q&A:** Missing user details may trigger relevant follow-ups, one at a time, with state updates before the next retrieval.
4. **Evidence, not model memory alone:** Health claims in Evidence Mode should rely on identifiable, applicable medical reference passages.
5. **Offline inference:** After approved model and knowledge-pack installation, speech, embeddings, retrieval, Qwen, and TTS run locally.
6. **Separate trust domains:** User statements, model interpretations, retrieved source text, and clinician-reviewed material are not interchangeable.
7. **Small mobile footprint:** Reuse the existing Whisper and Qwen engines; download extra embedding/index assets outside the APK where practical.
8. **Evidence before completion claims:** Report actually executed tests, asset sizes, source counts, and physical-device status. Never infer clinical reliability from a passing unit suite.

---

## 01. What problem are we solving?

Modern health information often depends on an Internet connection, a remote assistant, or a user knowing precise medical terms. A person may say "my head keeps pounding" instead of "headache," or answer a prior question with "upper right" instead of repeating an entire symptom description. Conventional keyword search and isolated LLM calls may lose this context. Sending sensitive conversations to hosted services can also create privacy and connectivity concerns.

Voice Doctor AI explores a **local, evidence-aware conversational approach**: a user speaks naturally; the system keeps only the relevant conversational facts, searches a medically sourced offline corpus, and asks for additional context only if it would materially improve an appropriately limited answer.

### 01.1 Design goals

- Accessible voice/text interaction on Android with an existing doctor-themed interface.
- Local English speech recognition and optional offline speech output.
- Meaning-based search over a curated, auditable medical corpus.
- Step-by-step follow-up questions generated contextually, not a universal hardcoded questionnaire.
- Accurate handling of short replies, corrections, negation, repeated questions, multiple people, and topic changes.
- Transparent limitations when relevant medical reference evidence cannot be found.
- Source attribution that can be checked against the exact passages supplied to the model.
- Measurable speed, storage, retrieval relevance, and privacy behavior on commodity phones.

### 01.2 Explicit non-goals

- Diagnosing disease, prescribing drugs/doses, or replacing clinician-led triage.
- A live medical record system, a hospital integration, or a telemedicine service.
- Cloud inference, remote chat APIs, or uploading speech transcripts during ordinary use.
- Claiming comprehensive medical coverage from a limited offline knowledge pack.
- Automatically treating generated text or user chat history as trusted medical knowledge.
- Training ResNet-50 for text retrieval: ResNet-50 is an image model, not this pipeline's text embedder.
- Restoring the retired Phase 4 broad keyword-based intent and symptom-classification architecture.

---

## 02. What is already implemented and what is newly designed?

| Capability | Phase 3 baseline | New Phase 4 architecture |
|---|---|---|
| Android Compose chat and doctor avatar | Implemented | Preserve and minimally extend |
| Push-to-talk + AudioRecord | Implemented | Preserve |
| Whisper.cpp Tiny Q5 speech-to-text | Implemented, English | Preserve |
| On-device Qwen3-0.6B GGUF with llama.cpp | Implemented | Reuse; repair prompt formatting and context handling |
| Android TTS | Implemented | Speak only approved questions/answers |
| Opt-in Room conversation history | Implemented | Extend with structured session state and privacy checks |
| MiniLM-L6-v2 INT8 ONNX embedder | Not implemented in Phase 3 | New |
| Verified medical passage corpus and indexes | Not implemented in Phase 3 | New |
| Keyword + semantic hybrid retrieval | Not implemented in Phase 3 | New |
| Adaptive RETRIEVE / REUSE / SKIP controller | Not implemented in Phase 3 | New |
| Dynamic follow-up / context-building loop | Not implemented in Phase 3 | New |
| Validated structured Qwen action protocol | Not implemented in Phase 3 | New |
| Per-passage evidence and applicability checks | Not implemented in Phase 3 | New |
| Evidence Mode toggle | Not implemented in Phase 3 | New |
| Full medical safety evaluation | Not established | Required; qualified review needed |
| Real Telugu ASR / RAG / TTS | Demo only | Outside revised Phase 4 |
| Fully animated 3D half-body doctor | Not established | Separate future interface milestone |

**Historical automated results:** The Phase 3 implementation report states 28/28 automated tests passed and a roughly 76.5 MB debug APK built. These are historical report results, **not tests re-run by this README**. Physical phone verification remains pending in the repository checklists.

---

## 03. Complete system in one sentence

**User voice/text -> Whisper/text input -> immediate conservative safety interrupt -> conversation-state update -> adaptive retrieval decision -> (MiniLM semantic search + keyword search -> rank fusion) -> evidence pack -> existing Qwen conversation planner -> ask one relevant question OR produce supported educational response OR explain missing knowledge OR issue cautious urgent-care guidance -> validated chat/TTS -> next user turn.**

---

## 04. End-to-end runtime architecture

~~~mermaid
flowchart TD
    USER["User speaks or types"] --> INPUT["AudioRecord + Whisper.cpp / Compose text input"]
    INPUT --> SAFE["Narrow independent safety interrupt (every turn)"]
    SAFE -->|"Ordinary or uncertain"| STATE["Conversation State Manager"]
    SAFE -->|"Possible immediate concern"| URGENT["Reviewed conservative urgent-care message"]
    STATE <--> MEM[("In-memory session / optional Room history")]
    STATE --> UPDATE["Apply new user-reported facts & corrections"]
    UPDATE --> ROUTER["Adaptive Retrieval Controller"]
    ROUTER --> DEC{"Choose evidence route"}
    DEC -->|"RETRIEVE"| QUERY["Context-aware query builder"]
    QUERY --> EMB["MiniLM-L6-v2 INT8 ONNX"]
    EMB --> VECTOR["Exact local vector similarity"]
    QUERY --> LEX["SQLite FTS / BM25 fallback"]
    VECTOR --> FUSE["Fusion + dedup + applicability"]
    LEX --> FUSE
    KB[("Versioned local medical knowledge pack")] --> VECTOR
    KB --> LEX
    FUSE --> EVIDENCE["Evidence bundle + source IDs + limitations"]
    DEC -->|"REUSE"| CACHE["Verified version-matched cached evidence"]
    CACHE --> EVIDENCE
    DEC -->|"SKIP (nonmedical)"| SIMPLE["Simple UI action / limited conversation"]
    EVIDENCE --> QWEN["Existing Qwen3-0.6B conversation planner"]
    STATE --> QWEN
    QWEN --> ACTION{"Validated next action"}
    ACTION -->|"ASK"| ASK["One generated follow-up question"]
    ASK --> QVAL["Question validator"]
    QVAL --> OUTPUT["Compose doctor chat + offline Android TTS"]
    OUTPUT -->|"Next user reply"| USER
    ACTION -->|"ANSWER"| ANSWER["Grounded educational answer + real citations"]
    ACTION -->|"KNOWLEDGE_GAP"| GAP["Explain absent/weak medical evidence"]
    ACTION -->|"POSSIBLE_URGENT"| CAUTION["Conservative urgent-care guidance"]
    ANSWER --> VALID["Evidence + safety + output validator"]
    GAP --> VALID
    CAUTION --> VALID
    SIMPLE --> VALID
    VALID --> OUTPUT
    URGENT --> OUTPUT
~~~

### 04.1 Exact ordering matters

A short follow-up such as **"upper right"** cannot be searched as an isolated new question. First associate it with the pending question (for example, the location of previously reported stomach discomfort), update user-reported state, and **then** construct the contextual retrieval query. If that association is ambiguous, ask for confirmation or use a bounded, validated Qwen interpretation step before changing facts.

### 04.2 Loop, not a fixed questionnaire

**ASK -> display/speak the question -> user replies -> resolve pending question -> update state -> decide whether retrieval must change -> Qwen decides next action.** The system can stop to give limited information, respect a refusal, or interrupt for a potentially urgent issue. A question cap prevents endless loops; reaching it is **not** evidence of medical completeness.

### 04.3 Two fundamentally different information gaps

- **Missing user context:** A worthwhile follow-up may clarify where/when/how a symptom was reported or which person is being discussed.
- **Missing medical reference evidence:** More user answers cannot manufacture trusted medical literature. Explain the offline knowledge limitation; do not make unsupported claims.

---

## 05. Offline knowledge-preparation architecture

The medical dataset is processed **off-device** on a development machine. The Android application does not scrape sites, train an embedding model, run Python, or construct the complete index during each conversation.

~~~mermaid
flowchart LR
    XML["Official eligible MedlinePlus health-topic XML"] --> RIGHTS["License and provenance audit"]
    RIGHTS --> PARSE["Parse, sanitize, normalize, deduplicate"]
    PARSE --> CHUNK["Section-aware medical passages"]
    CHUNK --> META["Stable passage IDs + source metadata"]
    META --> EMB["MiniLM document embeddings (Python)"]
    EMB --> IDX["Packed vectors + lexical search data"]
    IDX --> PACK["Versioned manifest + SHA-256 + compatibility"]
    PACK --> INSTALL["Consent-based install into Android private storage"]
    INSTALL --> APP["Offline retrieval at runtime"]
~~~

### 05.1 Source-of-truth policy

Initial content is based on **eligible English MedlinePlus health-topic summaries** from official downloads: https://medlineplus.gov/xml.html . See MedlinePlus's content reuse policy at https://medlineplus.gov/about/using/usingcontent/ . Public-domain health-topic summaries and copyrighted third-party linked content must be distinguished **per item**. Do not silently scrape and redistribute copyrighted illustrations, commercial encyclopedias, drug guides, or pages outside the permitted corpus.

### 05.2 Pilot versus planned full pack

- **Build smoke/evaluation pack:** approximately 100–250 diverse eligible English topics from actual official source files.
- **Production candidate:** expand to the eligible summaries as licensing, quality, phone storage, and retrieval evaluation permit.
- Record **actual** topic count, passage count, bytes, language coverage, exclusions, and source revision. These are measured outputs, not fixed promises.
- The old 25-topic manually prepared Phase 4 pack is a historical experiment, **not** the primary production corpus.

### 05.3 Ingestion outputs (planned)

- Clean, deduplicated source/passage dataset.
- Auditable source manifest and explicit eligibility decisions.
- Stable, deterministic passage IDs and content hashes.
- Reproducible Python embedding/index build.
- Binary normalized embedding matrix plus ID mapping.
- Keyword/FTS index or portable inverted-index fallback.
- Versioned package manifest with per-file SHA-256 hashes and model/tokenizer compatibility.
- Benchmark questions with labeled relevant passages, including negatives and out-of-scope queries.
- Data-quality report stating unreviewed content and coverage gaps.

### 05.4 Corpus governance

Provenance should include actual publisher, exact URL, available publication/revision date (never invented), language, original or transformed text status, applicable license, last-source-check date, and **clinical review status**. A valid checksum proves byte integrity only; it does not prove that a passage is medically accurate or safe for personal advice.

---

## 06. Final technology stack

| Layer | Implementation | Runtime location | Status / note |
|---|---|---|---|
| Android | Kotlin, Jetpack Compose, Material 3 | Device | Existing |
| State and async work | MVVM, ViewModel, Coroutines, StateFlow/Flow | Device | Existing; extend |
| Microphone | android.media.AudioRecord, 16 kHz mono PCM | Device | Existing |
| Speech recognition | whisper.cpp v1.7.4, Tiny Q5_1, C++/JNI | Device | Existing English |
| Text generation | Qwen3-0.6B, Q4_K_M GGUF | Device, downloaded | Existing |
| LLM runtime | llama.cpp b4999 (baseline), GGML, C++/JNI | Device | Existing; correct batching/formatting |
| Conversation planning | **Same** Qwen3 model, structured action protocol | Device | Planned |
| Text embeddings | sentence-transformers/all-MiniLM-L6-v2 | Device | Planned |
| Embedding format | ONNX, dynamic INT8, 384-dimensional output | Device | Planned; verify export |
| Mobile embedding runtime | ONNX Runtime Android (CPU first) | Device | Planned |
| MiniLM tokenizer | Checkpoint-compatible WordPiece + attention mask | Device | Planned |
| Document embeddings | Python/SentenceTransformers, identical pooling and normalization | Build machine | Planned |
| Vector search | Exact cosine/dot-product on normalized 384-D vectors | Device | Planned |
| Lexical retrieval | SQLite FTS where supported or compatible BM25/index fallback | Device | Planned; verify Android SQLite |
| Hybrid ranking | Reciprocal Rank Fusion (RRF) + evidence applicability | Device | Planned |
| Medical knowledge | Audited, reusable MedlinePlus English summaries | Build machine -> device | Planned |
| Conversation persistence | Room/SQLite **opt-in** | Device | Existing; harden |
| Speech output | Android TextToSpeech with offline voice preference | Device | Existing |
| Downloads | OkHttp over HTTPS, manifest/size/hash checks | Device, installation only | Existing for Qwen; extend assets |
| Build tooling | Gradle, Android SDK 36, NDK r26c, CMake | Development machine | Existing |
| Planned REST inference backend | **None** | N/A | Offline-first design choice |

### 06.1 Why MiniLM rather than ResNet-50 or a large multilingual embedder?

MiniLM-L6-v2 is a **text sentence-embedding model**, approximately 22.7 million parameters, producing **384-dimensional** vectors. ResNet-50 is principally an image classification backbone. The finalized Phase 4 **English-first** choice is MiniLM for its relatively small footprint. A multilingual E5 model was considered in an earlier draft but **is not the frozen Phase 4 choice**. Genuine Telugu retrieval requires separate model/data evaluation later.

**Model:** https://huggingface.co/sentence-transformers/all-MiniLM-L6-v2

### 06.2 Why no extra medical-intent model?

The retired Phase 4 approach relied on exact symptom and request-type keyword lists. The new design uses one Qwen model to plan follow-ups and generate educational text; retrieval is semantic + lexical. A **narrow independently evaluated safety interrupt** remains for potentially immediate concerns and is not a replacement for a giant hardcoded symptom taxonomy.

### 06.3 Why exact local vector search?

For an initially modest offline corpus, normalized vectors and cosine/dot-product ranking are simpler, smaller, and more auditable than installing FAISS/Chroma/Pinecone/pgvector. Approximate nearest neighbors should be considered only after real phone profiling shows a need.

---

## 07. Mobile app layers and boundaries

~~~text
app/
  Presentation
    MainActivity / Compose screen / doctor avatar / chat / input controls
  ViewModel
    VoiceDoctorViewModel / immutable UI state / lifecycle
  Application logic [new]
    ConversationalRagOrchestrator
    ConversationStateManager
    AdaptiveRetrievalController
    ContextualQueryBuilder
    ConversationPlanner
    QuestionValidator
    EvidenceValidator / OutputSafetyGate
  Domain interfaces [new]
    EmbeddingEngine / MedicalRetriever / KnowledgePackRepository
    SessionState / EvidenceBundle / PlannerDecision / ResponseResult
  Device adapters
    AudioRecorder / WhisperSpeechToTextEngine / TextToSpeechManager
    LlamaCppEngine / LlamaNative / ModelRepository
    MiniLmOnnxEmbeddingEngine [new]
    HybridMedicalRetriever [new]
  Storage
    In-memory session / optional Room histories / local verified knowledge assets
  Native
    libvoice_whisper.so + libvoice_llm.so / GGML
~~~

The names above specify **ownership and responsibilities**. New file paths are proposed contracts until the redesigned Phase 4 branch actually implements them. Do not imply that merely documenting a Kotlin type makes it present in the repository.

### 07.1 Responsibility of each layer

- **Compose:** render state, capture mic/text interactions and mode changes; no medical ranking or clinical decision logic.
- **ViewModel:** manage lifecycle and screen events, not construct unbounded prompts or load tensors on the main thread.
- **Orchestrator:** coordinate one user turn from input to validated output; terminate or cancel cleanly.
- **State manager:** track confirmed-to-be-user-reported facts, corrections, subject boundaries and pending questions.
- **Retrieval controller:** choose RETRIEVE/REUSE/SKIP/DEGRADED; never mistake retrieval latency for medical confidence.
- **Embedder:** map contextual query text to a reproducible normalized 384-D vector.
- **Retriever:** combine semantic and lexical rankings; return source passages and a calibrated evidence status.
- **Planner:** choose ASK/ANSWER/POSSIBLE_URGENT/KNOWLEDGE_GAP with **the same Qwen model**.
- **Validators:** constrain generated questions, citations, unsupported claims, unsafe model formatting and speech display.
- **Room:** opt-in human conversation records, not the trusted medical RAG corpus.
- **Native runtime:** safe, bounded and cancellable model inference.

---

## 08. Detailed component reference

### 08.1 Doctor interface and interaction states

Preserve the existing doctor-themed Android Compose screen, push-to-talk affordance, chat sheet, language selector, model status, and speaking/listening states. Do not redesign the entire UI as a prerequisite for RAG. The long-term half-body 3D doctor with minimal greeting, listening and mouth animations is a **separate visual enhancement**, not a Phase 4 completion gate.

### 08.2 Voice capture: AudioRecord

- Input: user holds microphone; microphone permission granted.
- Signal: 16,000 Hz, mono, PCM16 from AudioRecord; Phase 2 baseline caps a recording session at 30 seconds.
- Processing: convert to normalized floats for Whisper; expose amplitude only for UI feedback.
- Output: a bounded audio buffer; silence/very-short audio should not fabricate a medical question.
- Error states: permission denied, interrupted capture, empty buffer, device route change, cancellation.

### 08.3 Speech recognition: Whisper.cpp

- Input: normalized audio samples, language setting, native Whisper context.
- Runtime: whisper.cpp Tiny Q5_1 using a JNI bridge and dedicated coroutine dispatcher.
- Output: recognized **English transcript**; user may confirm/correct a consequentially ambiguous transcript.
- Important: transcription uncertainty is not medical certainty. Negation and numbers can be misrecognized.
- Preserve the existing model asset path and version unless separately verified.

### 08.4 Typed input

- Input: text from Compose, or approved quick prompt.
- Output: same normalized conversational turn contract as Whisper, with an origin flag TEXT/VOICE.
- Do not run typed text through speech recognition.
- Do not automatically treat preset demonstration content as real patient information.

### 08.5 Immediate safety interrupt

- Runs on **every** new turn before routine questioning and again before release of generated output.
- Uses conservative, narrow, independently reviewed handling for possible urgent concerns; bypasses long routine interviews when appropriate.
- Model predictions alone are not a certified urgency detector. The safety interrupt should not recreate the retired hardcoded symptom/request taxonomy.
- Locale-dependent emergency contacts must not be guessed. Ambiguous localization should result in general appropriate local emergency-service guidance.
- Test false negatives, false positives, negation, quoted/fictional examples, and uncertain ASR. Never claim perfect detection.

### 08.6 Conversation State Manager

- Holds session ID, subject/person ID, latest user turn, user-reported facts, original source spans, negation/polarity, corrections, pending question, questions already asked, and relevant prior turn IDs.
- Stores **user-reported** facts, not confirmed diagnoses.
- Corrects or supersedes facts when the user says "actually ...".
- Keeps reports from different people separated.
- Maintains in-memory state without requiring persistent chat-history consent.

### 08.7 Pending-question resolver

If the model asked "Where is the discomfort?" and the user replies "Upper right," the reply is a potential answer to that pending question. Associate it with the active reported symptom before retrieval. If the association is ambiguous, ask for clarification. The model may propose a context patch, but **only apply a user fact supported by an actual user utterance span**.

### 08.8 Adaptive Retrieval Controller

Three normal decisions:

- **RETRIEVE:** first substantive medical query; new topic; corrected location/duration; meaningful new details; changed subject; stale pack/cache.
- **REUSE:** same facts and question, valid session-scoped evidence, same model/index/pack revision.
- **SKIP:** acknowledgements, mute, stop, repeat, or other interactions that do **not** introduce new medical facts.

DEGRADED mode permits verified lexical-only retrieval if embedding inference fails. SKIP must not become permission to generate new unsupported health claims.

### 08.9 Contextual Query Builder

- Input: original current question, relevant earlier user statements, resolved pending-question answers and subject identifier.
- Output: a concise, bounded retrieval query with no invented facts.
- Preserve meaningful negation, duration, location and the fact that the speaker may be describing another person.
- Example: "Upper right" in context becomes "user-reported stomach discomfort, location upper abdomen/right side" only if those associations were actually stated or safely clarified.
- Retain original wording alongside reformulated text to audit mistakes.

### 08.10 WordPiece Tokenizer

- Load the exact MiniLM checkpoint vocabulary and normalization settings.
- Add [CLS], [SEP], [PAD], attention masks and optional token-type IDs as the exported model requires.
- Bound sequences to the model's supported limit (reference model card: up to 256 wordpieces); warn about meaningful truncation.
- Match Android tokens to Python reference fixtures, including punctuation, medical terms, abbreviations and long questions.

### 08.11 MiniLM ONNX Embedding Engine

- Input: contextual English text and tokenized tensors.
- Model: INT8 ONNX export from pinned all-MiniLM-L6-v2 weights.
- Postprocess: **attention-mask-aware mean pooling**, then L2 normalization. Do not substitute a raw [CLS] vector.
- Output: finite, normalized **FloatArray(384)**.
- Android inference: ONNX Runtime CPU first, off the UI thread, with resource closure and cancellation-aware lifecycle.
- No E5-style "query:" or "passage:" prefixes: those belong to a different model family.

### 08.12 Vector search

- Passage embeddings are precomputed off-device using matching model weights and normalization.
- At runtime, calculate cosine similarity, equivalently dot product when both vectors are normalized.
- Validate vector dimension, file hash, model revision, NaN/infinity protection, passage-ID alignment and index version.
- Return top-N candidates and scores as **retrieval evidence**, not medical confidence.

### 08.13 Keyword search

- Search medical titles, headings, synonyms and passage text through a lightweight local lexical index.
- Prefer SQLite full-text search with supported ranking semantics; measure actual API 24+ availability rather than assuming FTS5 is universally built into Android.
- Provide a portable BM25/inverted-index fallback if required.
- Keyword matching complements, rather than replaces, semantic retrieval.

### 08.14 Rank fusion and deduplication

- Merge semantic and lexical ranked result lists using Reciprocal Rank Fusion, initial experimental constant k = 60.
- Deduplicate overlapping passages and preserve exact original passage IDs.
- Apply language/source eligibility, applicability constraints and relevance calibration.
- Do not interpret Rank 1 as "medically most correct."

### 08.15 Evidence status and bundle

A typed retrieval outcome should distinguish:
- RELEVANT
- WEAK_OR_CONFLICTING
- NONE
- INDEX_UNAVAILABLE

It should return the selected source passage IDs, titles, URLs, excerpts, rank/scores, pack revision, review status, and limitations. **Relevant** means matching the question; it does not mean clinically verified or sufficient for diagnosis.

### 08.16 Evidence caching

A cache key includes session, subject, current user-fact fingerprint, question/topic fingerprint, embedding-model revision and medical-pack version. Repeated questions can reuse valid evidence. Corrections, new topics/subjects, pack updates, mode switches and session deletion invalidate it.

### 08.17 The Qwen conversation planner

- Model: existing Qwen3-0.6B GGUF; no second model.
- Inputs: latest user turn, structured user-reported state, concise recent dialogue, evidence bundle/status, safety constraints, previous asked questions and response budget.
- Outputs: structured action ASK / ANSWER / POSSIBLE_URGENT / KNOWLEDGE_GAP.
- Qwen generates a **candidate** action, not an unquestionable medical conclusion.
- Parse robustly; if invalid, attempt at most one bounded repair/retry or a conservative fallback.

### 08.18 Dynamic Question Generator

- Given incomplete but potentially answerable user context, produce **one** relevant unanswered question.
- Examples of possible question categories: location, duration, progression, severity, or another symptom if important to the question.
- These are possible categories, **not a fixed ordering or mandatory checklist**.
- Never infer that a person's age, temperature, or diagnosis is known when it was not provided.
- Honor "I don't know," "skip," "answer with what you have," and interruptions.

### 08.19 Question Validator

Before a generated question is displayed or spoken:
- Check it is one clear question, related to the current subject and actual information gap.
- Reject repeated, leading, inappropriate, diagnostic, medication-prescriptive, or unnecessarily intrusive prompts.
- Verify it does not overwrite user-reported facts with model guesses.
- Respect the current conversation's question budget and potential urgent signals.
- Only mark a question as pending **after** the approved question has been presented.

### 08.20 Evidence-Grounded Answer Generator

The same Qwen model receives a bounded prompt containing the original query, context, relevant passages, and citation IDs. Generate concise, readable educational information without declaring diagnoses or specific medications. If the retrieved passages are not adequate, produce a limitation rather than supplementing them with unverified model memory.

### 08.21 Evidence and Output Validator

- Buffer generated medical text until validation completes; do not speak unchecked token fragments.
- Reject hallucinated source IDs, inapplicable citations, missing required support and unsafe model formatting.
- Check that cited passages could actually support the associated factual claims; mere citation syntax is not fact verification.
- If support cannot reasonably be established, narrow the answer or use a transparent uncertainty fallback.
- Do not silently assign a citation to an uncited answer.
- Independent clinical review is still required for trustworthy medical-answer quality.

### 08.22 Conservative knowledge-gap response

If medical reference evidence is unavailable or insufficient, **do not send the user through endless questions**. Ask only if a missing detail would change the retrieval result; otherwise explicitly say the offline knowledge base cannot support a confident answer and refer to appropriate trusted resources or professional care.

### 08.23 Possible urgent concern

Urgent concern may appear on the first question **or in any later follow-up**. Stop routine question generation where appropriate. Offer cautious, reviewed urgent-care language. Do not claim that the small Qwen model provides certified emergency detection or diagnosis.

### 08.24 Offline TextToSpeech

Speak only the approved final question or response. Prefer an installed offline English voice, preserve sound toggle and speaking animations, stop gracefully on cancellation or mute. Absence of an offline voice must not prevent a textual answer.

### 08.25 Room conversation history

Persist chats only after explicit opt-in; distinguish persistent conversation records from in-memory active state. Preserve history across multiple turns without destructive parent-row replacement. The user must be able to delete chats. Sensitive content must be excluded from unsupported backup paths and must never be copied into the RAG source database.

### 08.26 Model/pack installation manager

Display explicit asset name, expected download size, progress, storage availability, hashes, error and cancellation states. Download to temporary files, verify exact version/size/checksum and atomic-install compatible packs. Reject corrupt, partial, mismatched or unlicensed assets. Runtime queries do not call the network.

### 08.27 Diagnostics and evaluation

Track per-stage latency, embedding size, index size, retrieval result IDs, safe fallback counts and memory use. Do **not** log raw personal medical text in analytics. Debug traces should be opt-in, local and removable.

---

## 09. Interfaces: local method calls, JNI and REST/HTTP

A frequent source of confusion is assuming every component communicates through a REST API. **It does not. This is a native offline Android app.** Most data travels through Kotlin interfaces, coroutines, Room and JNI—not HTTP calls.

### 09.1 Protocol ownership

| Boundary | Transport / API style | Actual or planned |
|---|---|---|
| Compose -> ViewModel | Kotlin callbacks, StateFlow | Existing |
| ViewModel -> orchestrator | Kotlin suspend function | Planned |
| Orchestrator -> state manager | Kotlin objects / in-memory state | Planned |
| Query builder -> embedder | Kotlin interface + ONNX Runtime Java bindings | Planned |
| Retriever -> corpus and index | App-private file I/O + SQLite | Planned |
| App -> Whisper | Kotlin -> JNI -> C++ function calls | Existing |
| App -> Qwen | Kotlin -> JNI -> llama.cpp C++ calls | Existing |
| App -> TTS | Android platform TextToSpeech API | Existing |
| App -> Room | DAO functions / SQLite | Existing |
| App -> model hosting | HTTPS GET using OkHttp for **user-approved asset downloads** | Existing for Qwen; planned for additional RAG assets |
| App -> remote medical answer server | **None required or planned for Phase 4** | Offline architecture |
| App -> remote chat REST API | **None** | Privacy and offline constraint |

### 09.2 Existing HTTP download behavior (not a chat API)

The existing Qwen manager performs a user-consented **HTTPS GET** of the GGUF file; it streams the bytes to a temporary local file, validates SHA-256 and size, and installs into app-private storage. There is no production app-hosted server endpoint behind this operation.

For future embedding/knowledge assets, use equivalent pinned, checksum-verified download manifests. Any example GET URL is an **artifact source**, not a clinical inference service.

### 09.3 Planned local orchestration contract (illustrative, not an existing endpoint)

~~~kotlin
interface ConversationalRagEngine {
    suspend fun handleTurn(
        sessionId: String,
        subjectId: String,
        input: UserTurn,
        mode: EvidenceMode
    ): AssistantTurnResult
}
~~~

Inputs include user message, source type (VOICE/TEXT), user-approved transcript and bounded session identity. Outputs distinguish approved follow-up, grounded answer, knowledge gap, cautious urgent-care guidance or unavailable component.

### 09.4 Planned embedding contract

~~~kotlin
interface EmbeddingEngine {
    suspend fun embedEnglish(query: String): FloatArray // 384 normalized values
}
~~~

Reject empty, non-finite, incompatible or failed outputs. Check tokenizer parity before calling this ready.

### 09.5 Planned retrieval contract

~~~kotlin
interface MedicalRetriever {
    suspend fun retrieve(
        query: String,
        subjectId: String,
        topK: Int = 3
    ): EvidenceBundle
}
~~~

The result includes exact passage citations and a calibrated support status. A search result must not overwrite patient context.

### 09.6 Planned retrieval-policy contract

~~~kotlin
enum class RetrievalDecision {
    RETRIEVE, REUSE, SKIP, DEGRADED
}
~~~

Decisions use active topic, new/corrected user facts, evidence-cache compatibility, pack version and evidence status.

### 09.7 Planned planner output (illustrative JSON protocol, not REST)

~~~json
{
  "action": "ASK",
  "question": "How long has this been happening?",
  "answer": "",
  "source_ids": [],
  "needed_information": "duration",
  "user_fact_updates": [],
  "uncertainty": ""
}
~~~

Other allowed actions: ANSWER, POSSIBLE_URGENT, KNOWLEDGE_GAP. A JSON parser and schema validator must reject unsupported action names, extra assertions, fake citations or malformed payloads. This JSON is **a local LLM output contract**, not an HTTP response.

### 09.8 JNI flow: Whisper

~~~text
AudioRecord PCM -> FloatArray
   -> WhisperSpeechToTextEngine
   -> WhisperNative JNI
   -> native Whisper context / libvoice_whisper.so
   -> decoded English text
   -> ViewModel / orchestrator
~~~

### 09.9 JNI flow: Qwen

~~~text
Conversation context + evidence + system instructions
   -> ONE chat-template formatter
   -> LlamaCppEngine
   -> LlamaNative JNI / libvoice_llm.so
   -> llama.cpp tokenizer, bounded native batches, sampling
   -> generated local candidate
   -> structured parser and output validators
   -> approved UI/TTS
~~~

**Non-negotiable:** Only one component formats the Qwen chat template. The retired Phase 4 introduced a risk of double formatting; test the **actual final string at the native boundary**. Handle Qwen3 non-thinking-mode semantics correctly; stripping thought tags after generation is not enough. Never exceed the allocated native prompt batch or model context budget.

### 09.10 Why not FastAPI/Retrofit/cloud RAG?

A remote REST backend would contradict the principal offline-inference and private-chat objective. Retrofit and other dependencies may appear in the inherited Gradle catalog but their presence **does not mean the current medical conversation calls an API**. We may evaluate a separately opt-in remote synchronization service in a future project phase, but it is not part of Phase 4 and must not be presented as implemented.

---

## 10. Precise model input/output sequence

### 10.1 Whisper input/output

**Input:** up to the bounded microphone clip, 16 kHz mono PCM converted to floats.  
**Output:** an English transcript string, or explicit error/empty state.

### 10.2 Conversation manager input/output

**Input:** transcript or typed message, active session and previous pending question.  
**Output:** updated **user-reported** fact state, correction markers, relevant recent turns and flags requiring re-retrieval.

### 10.3 MiniLM input/output

**Input:** normalized, contextual English retrieval query -> checkpoint-compatible WordPiece token IDs, attention mask (and any required tensor input).  
**Output:** pooled, L2-normalized vector of shape **[384]**. The same MiniLM model is used off-device to embed document passages.

### 10.4 Vector/keyword search input/output

**Input:** 384-D query vector and textual query.  
**Output:** ranked candidate passage IDs and scores from each engine; fuse, deduplicate and filter.

### 10.5 Qwen planner input/output

**Input:** recent user conversation, user-reported facts, already asked question IDs, latest query, evidence snippets with source IDs, retrieval status, response-policy instructions and token budget.  
**Output:** one structured candidate action, optional single question, optional educational answer, citation IDs and uncertainty.

### 10.6 Validated output

**Input:** candidate action/text, user facts, retrieved passages and safety context.  
**Output:** approved user-visible text, optional citation chips, approved question to speak, or conservative fallback. Never expose raw unvalidated model tokens to TTS.

---

## 11. Conversation state schema

The schema below describes **planned in-memory application state**, not an existing database migration.

~~~text
SessionState
  session_id
  active_subject_id                  # self / explicitly mentioned other person
  latest_user_turn_id
  current_topic
  phase                              # COLLECTING / ANSWERING / UNCERTAIN / POSSIBLE_URGENT
  user_reported_facts[]
    - attribute                      # location / duration / severity / etc.
    - value
    - polarity                       # reported / explicitly negated
    - source_turn_id
    - source_span                    # exact words supporting the fact
    - subject_id
    - correction_of                  # optional previous fact ID
  previous_assistant_questions[]
  pending_question_id
  question_budget_for_topic
  relevant_turn_ids[]
  evidence_cache
    - query, passage_ids[], pack_version, model_revision, subject_id
  last_approved_answer
~~~

### 11.1 Example short-answer resolution

~~~text
Turn 1 USER: "My stomach feels uncomfortable."
Turn 2 ASSISTANT (ASK): "Where do you feel the discomfort?"
Turn 3 USER: "Upper right."

Before retrieval:
  pending question = location of reported stomach discomfort
  new user-reported fact = location described as "upper right"
  previous symptom report remains linked
  contextual query = original stomach concern + reported upper/right location
After retrieval:
  model may ask another useful question or provide cautious information
~~~

### 11.2 Example correction

~~~text
USER: "I've had a cough for two days."
LATER USER: "Actually, two weeks."

Replace/supersede duration from two days -> two weeks;
invalidate the previous evidence cache;
reformulate and retrieve again if medically relevant;
do not retain both as simultaneously current reports.
~~~

### 11.3 Separating different people

A question about the user's father must not inherit the user's own symptoms. State includes subject IDs and explicit relationship/subject changes. If a pronoun cannot be resolved reliably, ask which person is being discussed.

### 11.4 Unknowns remain unknown

Do not fill age, pregnancy status, severity, temperature, onset or medication status unless the user stated it. Treat Qwen suggestions as hypotheses awaiting validated user grounding.

### 11.5 Persistent memory versus session memory

In-memory context enables a multi-turn conversation **even when persistent history is OFF**. Room persistence is optional and controlled by user consent; clearing a conversation must erase associated session-scoped cache/state.

---

## 12. Adaptive retrieval specification

### 12.1 First medical question

Default to RETRIEVE when relevant trusted knowledge is installed. Build the contextual query and search both retrieval branches.

### 12.2 Repeated question

Reuse cached evidence only if source pack, user facts, topic and subject remain applicable. Do **not** bypass retrieval merely because the wording is similar.

### 12.3 New relevant fact

A new location, duration, severity, negation or other materially relevant detail may require a new query and retrieval.

### 12.4 New topic or person

Reset/invalidate topic-specific evidence and pending questions as appropriate. Never merge one person's context into another's.

### 12.5 Simple conversational message

"Thanks," "stop speaking," or "repeat that" may be handled directly or by minimal Qwen response without generating new medical claims.

### 12.6 Absent embedder or corrupted index

Try verified lexical-only retrieval if available, label the degraded evidence state, and refrain from unsupported health claims. Handle unavailable models with clear installation instructions.

### 12.7 Evidence Mode setting

- **ON (default):** adaptive RAG, evidence-aware Qwen and cited educational answers.
- **OFF (experimental):** Phase 3-style direct Qwen for demonstrations/comparisons. Clearly label **"LLM only — not source-grounded."**
- Both modes retain independently reviewed conservative safety handling.
- A mode switch invalidates incompatible cached evidence; it must not silently persist source claims from a different mode.

### 12.8 Cache compatibility

Include session ID, active subject, fact fingerprint, topic/query fingerprint, knowledge-pack version, embedding model revision and index schema version. Never share patient-specific cache across sessions.

---

## 13. Semantic retrieval mathematics

### 13.1 Embedding

For WordPiece tokens with attention mask m_i and token embeddings h_i, apply masked mean pooling:

~~~text
pooled = sum_i(m_i * h_i) / max(1, sum_i(m_i))
normalized = pooled / max(epsilon, L2norm(pooled))
~~~

Reject zero, NaN, infinity, incompatible or incorrectly dimensioned results.

### 13.2 Cosine search

If both query q and passage p are L2-normalized, similarity is their dot product:

~~~text
sim(q,p) = sum_j(q_j * p_j), j = 1..384
~~~

This is **semantic similarity**, not a calibrated medical confidence score.

### 13.3 Reciprocal Rank Fusion

Combine separately ranked vector and lexical lists:

~~~text
RRF(d) = sum over retrievers r of 1 / (k + rank_r(d))
~~~

Start with experimental k = 60, deduplicate stable IDs, evaluate against relevant and non-relevant test questions. No single ranking formula proves that a passage is medically appropriate.

### 13.4 Why not simply use Rank 1?

The highest-ranked chunk can be incomplete or inapplicable. Choose a small **evidence pack**, initially up to three short passages within the actual Qwen context budget. Check conflicting/weak evidence and abstain from unsupported claims.

---

## 14. Medical knowledge data contract

### 14.1 Passage metadata (illustrative JSON)

~~~json
{
  "passage_id": "medlineplus_example_topic_section_001",
  "topic_id": "example_topic",
  "section": "overview",
  "source_title": "Example Health Topic",
  "source_url": "https://medlineplus.gov/",
  "publisher": "U.S. National Library of Medicine",
  "language": "en",
  "license_status": "verify_per_item",
  "review_status": "clinical_review_pending",
  "pack_version": "development-example",
  "text_sha256": "COMPUTED_NOT_GUESSED",
  "text": "Illustrative placeholder only; not medical advice."
}
~~~

The object above illustrates field names. **It is not a real indexed medical passage and must not be copied as a fact.**

### 14.2 Knowledge manifest

Include pack ID, semantic version, source revision/snapshot, content-type/license eligibility rules, passage count, vector dimension, embedding model checkpoint+revision, tokenizer hash, pooling and normalization, index format version, per-file hashes, source review status and install-time compatibility checks.

### 14.3 Chunking

Split by actual medical topic/section boundaries. Target coherent passages around 100–200 words **only where the model's 256-wordpiece limit permits**. Keep safety-relevant context attached. No silent truncation of warnings.

### 14.4 Source and knowledge trust boundaries

- Official source material: provenance-checked.
- Curated/rewritten derivative: transformation logged and review-pending until qualified review.
- User statement: user-reported, not reference material.
- Retrieved passage: available evidence, not automatically a medical recommendation.
- Qwen answer: untrusted candidate until validation.
- Medical review: separate human qualification process.

### 14.5 Offline update plan

Model and knowledge packs are separately downloadable, versioned and integrity-checked. A source update invalidates relevant cache and incompatible indexes. Do not call the source website during a normal offline conversation.

---

## 15. Qwen3 planner and response contract

### 15.1 One model, multiple responsibilities

The existing Qwen3-0.6B model should serve lightweight context interpretation, conversational follow-up planning and evidence-aware answer generation. No dedicated medical intent BERT, reranker LLM or second generative model is added by default.

### 15.2 One chat template owner

Choose **one** owner for the model's chat template: either an engine method accepts structured messages and formats exactly once, or it accepts a clearly tagged preformatted prompt without wrapping it again. Regression-test the exact native input. The previous Phase 4 experiment risked double-formatting.

### 15.3 Thinking and token budget

Use the correct **Qwen3** non-thinking configuration; do not accidentally document Qwen2.5. Reserve response tokens within the measured context length. Trim history/evidence by priority, not merely rough character count. Native C++ prompt evaluation must chunk safely within allocated batch capacity.

### 15.4 Planner decision schema

~~~json
{
  "action": "ASK",
  "question": "Which location are you referring to?",
  "answer": "",
  "source_ids": [],
  "needed_information": "location",
  "user_fact_updates": [],
  "uncertainty": "Current location reference is ambiguous"
}
~~~

Valid action names are ASK, ANSWER, POSSIBLE_URGENT, KNOWLEDGE_GAP. The output schema is a **candidate** to parse/validate; it is not a clinical verdict.

### 15.5 ASK decision

Ask one meaningful, non-repeated question when the missing user detail would materially improve the educational explanation or retrieval. Validate the question before presenting it. Default to at most three targeted questions per topic **as an implementation safeguard**, configurable with tests and user-controlled skipping.

### 15.6 ANSWER decision

Use relevant passage text and exact source IDs. Make appropriately limited educational statements; distinguish general information from individualized assessment. Avoid definitive diagnosis and medication prescriptions.

### 15.7 KNOWLEDGE_GAP decision

If evidence is missing, weak, contradictory or unavailable, explain that limitation. Ask for additional user context **only if** it could materially change what can be retrieved. Do not invent facts or cite an irrelevant result to fill the response.

### 15.8 POSSIBLE_URGENT decision

May prompt reviewed conservative guidance and stop routine questioning. It is not proof of an emergency nor proof of medical triage accuracy.

### 15.9 Malformed output

A small LLM may output invalid JSON, unsupported action names, reasoning tags or truncated text. Implement schema checks, bounded repair if justified, and deterministic conservative fallbacks. Do not expose failed parsing as trustworthy guidance.

---

## 16. Dynamic conversation example

~~~text
User (voice): "My stomach hurts."
Whisper output: "My stomach hurts."
Session: new self-reported concern; location/duration not supplied.
Retrieval: relevant generic medical passages; evidence may not resolve user context.
Qwen proposed action: ASK.
Question validator: one relevant question, not previously asked.
Doctor: "Where do you feel the discomfort?"

User: "Upper right."
Session: attach answer to the prior location question; retain original statement.
Adaptive retrieval: RETRIEVE if the location changes applicable reference evidence.
Qwen proposed action: ASK or limited ANSWER depending on evidence/uncertainty.
Doctor: "How long has it been happening?" (if useful and safe)

User: "About two hours."
Session: duration updated, same subject; no invented clinical diagnosis.
Retriever: REUSE or RETRIEVE based on materiality of new fact.
Qwen: bounded, evidence-aware educational response or one more relevant question.
Validator: verify claim support, citation IDs and no unsafe claims.
UI/TTS: present approved result; no raw Qwen tokens spoken.
~~~

This is an **illustrative interaction**, not a prescribed clinical questioning workflow.

---

## 17. Failure handling and edge-case matrix

| Case | Required system behavior |
|---|---|
| First vague symptom description | Ask one useful question if needed; do not invent missing details |
| Short answer to last question | Resolve to pending question before retrieval |
| User correction ("two weeks, not two days") | Supersede fact, invalidate relevant cache, search again as needed |
| Explicit negation | Preserve negated information; avoid silently reversing it |
| Whisper misheard a number/negation | Seek confirmation when consequential |
| Multiple complaints in one message | Maintain separate reported observations; avoid assuming a single cause |
| Another person's symptoms | Separate subjects and evidence caches |
| Hypothetical/general question | Do not turn into a personal medical history |
| Unspecified age or applicability | Preserve unknown and avoid inapplicable assertions |
| Repeated unchanged question | Reuse still-valid evidence; avoid redundant full retrieval |
| Same topic but changed facts | Re-retrieve if applicability changes |
| User says "I don't know" | Respect it, do not repeat the same question |
| User says "just answer" | Give appropriately limited supported information |
| Asked-question limit reached | Stop interrogating; use limited answer or knowledge-gap fallback |
| Missing medical source | Explain missing knowledge; do not hallucinate |
| Conflicting passages | Surface uncertainty or abstain; avoid choosing a false certainty |
| False high cosine score | Calibrate against negatives; relevance ≠ accuracy |
| Corrupted embedding file | Do not run invalid vectors; use verified degradation |
| Embedding model absent | Offer installation or verified lexical fallback |
| Qwen model absent | Explain installation; do not present generated answer |
| Wrong embedder/index revision | Refuse mismatched pack and request compatible install |
| Model returns malformed JSON | Schema failure -> conservative fallback |
| Model includes invented citation | Reject the citation and unsupported answer |
| User switches topic/person mid-question | Cancel/invalidate old pending question and cache |
| User clears conversation | Cancel active inference and discard session-specific memory |
| User interrupts speech | Stop TTS without corrupting state |
| User disables Evidence Mode | Label LLM-only mode; retain independent safety handling |
| Potential urgent concern at any turn | Interrupt routine follow-up and use cautious urgent-care response |
| Source passage contains adversarial instruction | Treat retrieved text as **data**, never system instructions |
| User attempts prompt override | Do not disclose/change internal policies or remove safeguards |
| Memory exhausted / device hot | Cancel or degrade gracefully; no freeze or undefined JNI behavior |
| No offline TTS voice installed | Continue with text; do not silently use network TTS |
| Device offline from installation onward | Show clearly which assets are required and not installed |
| Privacy/backup path exists | Audit backup rules, opt-in, deletion and actual network behavior |

### 17.1 Independent safety evaluation

A narrow safety interrupt exists to limit severe failures and works **even when RAG/LLM is unavailable**. It is not the old broad keyword classifier and cannot guarantee detection of all urgent medical situations. Evaluate missed concerns, unnecessary warnings, confusing phrasing, negation, transcription uncertainty, and follow-up transitions. Human clinical evaluation is required for safety claims.

### 17.2 Evidence-injection defense

Medical passage text can include markup and could contain malicious instructions in untrusted fields. The prompt builder must separate instructions from retrieved evidence, quote/escape source text appropriately, and never let a passage overwrite system policy.

### 17.3 Error transparency

Distinguish at least: transcription unavailable, model missing, embedding unavailable, no relevant source, conflicting sources, index mismatch, unsupported claim, bad citation, malformed planner output, cancellation and no offline TTS.

---

## 18. Safety and privacy architecture

### 18.1 Private on-device inference

Normal conversations must not upload transcripts, health facts, embeddings or model prompts to a remote server. Do not add hidden analytics or remote LLM fallback. Assets may be downloaded after the user opts in.

### 18.2 Storage policy

In-memory session state by default. Room persistence opt-in. Keep user history separate from trusted reference corpus. Delete session cache on clear, account/device handoff and subject reset as needed. Review Android cloud backup and device-transfer exclusions for all sensitive app files.

### 18.3 Consent and controls

Request microphone permission through Android runtime permissions. Make large asset downloads explicit. Provide model deletion, history control, Evidence Mode transparency, TTS toggle and user-controlled conversation clearing.

### 18.4 Medical limitations

The app should not claim to be a certified triage tool or give definitive diagnoses. All source-grounding and output filters must be evaluated. The presence of medically reputable content does not validate a model's application of it to a particular person.

### 18.5 Sensitive logging

Do not log raw health conversation text, audio, embeddings or full prompts by default. Prefer local, aggregate counters and non-identifying performance instrumentation.

---

## 19. Knowledge coverage and model limitations

1. **MiniLM is English-first** in this phase, not a validated Telugu semantic model.
2. **Qwen3-0.6B is small**, so structured JSON adherence, nuanced follow-ups and factual reliability require real evaluation and conservative fallbacks.
3. **MedlinePlus summaries are not comprehensive specialist guidelines** and may omit relevant context.
4. **Nearest-neighbor retrieval does not establish applicability** to a specific user's symptoms.
5. **No universal emergency detector** can be guaranteed by these components.
6. **The 25-topic historic pilot and 100-case old Phase 4 rule tests** are not evidence of the new RAG engine's quality.
7. **A download checksum** is not a clinical review.
8. **Android SQLite FTS availability and ONNX Runtime footprint** must be measured on supported devices.
9. **A beautiful avatar** does not mean the underlying response is medically correct.
10. **Offline use** depends on all required assets and an installed offline TTS voice.

---

## 20. Performance and APK-size strategy

### 20.1 Known Phase 3 baseline

- Historical Phase 3 report: ~76.5 MB debug APK, Whisper Tiny Q5 bundled.
- Existing Qwen GGUF: **484,220,000 bytes**, downloaded separately after consent.
- Baseline minSdk 24, targetSdk 36, NDK 26.3.11579264; ABIs include arm64-v8a and x86_64 in development.

These are existing implementation/report values, **not final Phase 4 measurements**.

### 20.2 Planned additional RAG assets

- MiniLM-L6-v2 quantized ONNX, **expected planning range ~23–35 MB**, not a guaranteed final export size.
- WordPiece vocab, passage text, source metadata, vector matrix and lexical index: measured per actual corpus.
- ONNX Runtime Android dependency: measure its native binary overhead; consider supported packaging/runtime reductions.
- Prefer separately installed RAG assets rather than bundling them into the initial APK.
- Do not promise an exact final application size until the assembled release candidate is measured.

### 20.3 On-device budgets

Record peak RAM, model-load latency, query embedding latency, hybrid retrieval latency, Qwen first/total token latency, conversation turn latency, cold-start time, battery/thermal behavior and APK/installed asset bytes. Start CPU-first. Only optimize kernels/ANN indexes after measurement.

### 20.4 Functional size tradeoff

Avoid an extra planner model, large cross-encoder reranker, vector server, Python runtime on phone, duplicate ONNX runtimes, unnecessary model assets or broad framework imports. Keep the native Phase 3 engine intact wherever possible.

---

## 21. Evaluation and acceptance gates

### 21.1 Retrieval evaluation

Build a **held-out labeled set** with direct questions, paraphrases, multi-turn queries, corrected facts, contradictions, hard negatives and deliberately unanswerable questions. Compare lexical-only, semantic-only, and hybrid retrieval using Recall@1/3/5, MRR, nDCG, empty-result calibration and per-query latency. Report actual corpus size and relevance thresholds.

### 21.2 Conversation evaluation

Test that:
- A short follow-up resolves to the right pending question.
- The model asks at most one relevant question per ASK action.
- It does not repeat answered questions.
- Corrected facts supersede obsolete ones.
- Refusal/skip ends unnecessary interrogation.
- A topic/person switch invalidates stale evidence.
- An ordinary answer may be given without forcing the maximum question count.
- A question budget prevents endless loops.
- Unknown data remains unknown.
- Answers remain appropriately limited if knowledge is absent.

### 21.3 Grounding evaluation

Measure unsupported factual claims, fabricated citations, wrong source IDs, irrelevant/inapplicable sources, missing source attribution and appropriate abstention. Do not count citation-format validation alone as factual support.

### 21.4 Safety evaluation

Include potentially urgent concerns arising at different turns, uncertain ASR, negation and follow-up traps. Report both missed and excessive warnings. **Automated testing is not clinical review.**

### 21.5 Android end-to-end evaluation

Test with actual native Whisper and Qwen where device/emulator resources permit; separate mocked tests from real-engine tests. Physical-phone checks remain documented and unchecked until performed. Do not claim real airplane-mode performance without testing it.

### 21.6 Release gates (go/no-go)

- Reproducible legal-medical corpus generation and valid source manifest.
- MiniLM export and tokenizer/embedding parity established within measured tolerance.
- Functional on-device vector + lexical search with evidence IDs.
- Structured conversation loop end-to-end, with ASK/ANSWER/KNOWLEDGE_GAP.
- Independent safety interrupt and question/output validation connected.
- No raw unchecked medical text spoken.
- Model/JNI batching and chat formatting tests pass.
- No user-data exfiltration in normal offline operation.
- APK builds; actual sizes recorded; unresolved tests explicitly listed.
- No clinical quality claims until qualified review and independent evaluation.

---

## 22. Test and build commands

Run in the repository root using the Gradle wrapper. The commands below describe the **existing build system** and are not proof of a new Phase 4 build.

~~~bash
# Windows
gradlew.bat testDebugUnitTest
gradlew.bat assembleDebug

# Linux / macOS
./gradlew testDebugUnitTest
./gradlew assembleDebug
~~~

The historical Phase 2/3 checklists are maintained under the [checks directory](checks/). Hardware verification is intentionally deferred until a physical phone is available.

### 22.1 Developer environment

- Android Studio with appropriate Android SDK and NDK components.
- JDK compatible with the checked-in Gradle/AGP versions; Phase 2 report used JDK 17.
- Android SDK platform 36 and NDK **26.3.11579264**.
- CMake as required by existing native build configuration.
- Enough disk space for Qwen, Whisper, embedded-model assets and native builds.
- Network may be needed at **build/install time** for dependencies or model assets; runtime inference is designed to work offline.

### 22.2 One-time model setup

The app's existing model manager offers a Qwen GGUF download after user consent. The new Phase 4 should add explicit installation and verification for embedding/index/medical assets. Do not silently ship a nonworking button or claim all models are embedded in the APK.

---

## 23. Proposed Phase 4 code ownership

Paths below show where **new** code should be located, adapted to the actual project. They are **not a claim these paths already exist**.

~~~text
app/src/main/java/com/example/
  viewmodel/
    VoiceDoctorViewModel.kt                # existing; integrate orchestrator
  voice/
    AudioRecorder.kt                       # existing
    WhisperSpeechToTextEngine.kt           # existing
    TextToSpeechManager.kt                 # existing
    llm/LlamaCppEngine.kt                  # existing; fix prompt/JNI boundaries
  modelmanager/
    ModelRepository.kt                     # existing; add verified asset management
  data/
    VoiceDoctorDatabase.kt                 # existing Room; keep conversations private
  rag/                                      # NEW proposed package
    ConversationalRagOrchestrator.kt
    ConversationStateManager.kt
    PendingQuestionResolver.kt
    AdaptiveRetrievalController.kt
    ContextualQueryBuilder.kt
    ConversationPlanner.kt
    PlannerDecisionParser.kt
    QuestionValidator.kt
    EvidenceValidator.kt
    ConservativeSafetyInterrupt.kt
    KnowledgePackRepository.kt
    MiniLmOnnxEmbeddingEngine.kt
    WordPieceTokenizer.kt
    HybridMedicalRetriever.kt
    VectorIndex.kt
    LexicalIndex.kt
    EvidenceCache.kt
    model/SessionState.kt
    model/EvidenceBundle.kt

tools/medical_pack/                         # NEW planned build-time Python scripts
  build_pack.py
  verify_pack.py
  export_minilm.py
  evaluate_retrieval.py
  tests/

medical-knowledge/                          # NEW planned source manifests and audit
  README.md
  sources_manifest.json

checks/                                     # Existing folder; add new Phase 4 checklist
  PHASE2_HARDWARE_CHECKS.md
  PHASE3_HARDWARE_CHECKS.md
  PHASE4_HARDWARE_CHECKS.md
~~~

### 23.1 Avoid unnecessary module rewrites

Prefer adding clear Kotlin packages and interfaces to the existing :app module first. Consider extracting Gradle modules when boundaries stabilize and real tests justify it. Keep existing working Phase 3 files rather than rewriting the entire app to match a diagram.

---

## 24. Ordered engineering implementation plan

| Step | Work | Verifiable output |
|---|---|---|
| **4.0** | Inspect current repo, preserve Phase 3 and create fresh branch | Baseline SHA, clean branch, dependency inventory |
| **4.1** | Audit/download eligible source XML, create corpus | Reproducible subset, manifests, legal-content report |
| **4.2** | Export & INT8-quantize MiniLM, tokenizer parity | ONNX model, reference fixtures, size/quality measurements |
| **4.3** | Evaluate desktop vector/keyword/hybrid retrieval | Held-out retrieval metrics, calibrated relevance thresholds |
| **4.4** | Implement Android MiniLM inference, packed index, BM25 fallback | Kotlin integration tests and measured device/runtime sizes |
| **4.5** | Implement state-first conversation and adaptive cache | Unit tests for corrections, subjects, short replies and reuse |
| **4.6** | Integrate one Qwen planner with correct native template | End-to-end structured ASK/ANSWER loop, fallbacks |
| **4.7** | Integrate safe UI/TTS and Evidence Mode | Existing doctor UI preserved, approved text only |
| **4.8** | Reliability, privacy, adversarial and performance evaluation | Reproducible results with limitations |
| **4.9** | Build APK, document actual status, push feature work | Commit SHAs, artifact sizes, pending hardware checklist |

The implementation specification previously agreed for the redesigned Phase 4 is the document **VOICE_DOCTOR_PHASE4_ANTIGRAVITY_EXECUTE_FINAL.md**. Treat it as an engineering instruction document where available; this README is the public architecture and status reference.

---

## 25. Phase 3 bugs to preserve fixes for (do not carry mistakes forward)

Before combining evidence and a longer conversation context, review:

1. **JNI prompt-batch capacity:** The original Phase 3 native batch could be overfilled by long prompts. Use safe chunking and context boundaries.
2. **Room parent-row replacement:** Replace-on-conflict of a conversation parent may cascade-delete child messages. Preserve history without destructive replacement.
3. **Chat template double formatting:** The superseded Phase 4 built a full chat template and passed it to an engine that also wrapped prompts. Exactly one layer must format chat messages.
4. **Qwen3 non-thinking output:** Configure model-appropriate behavior and test; stripping thought tags is not enough.
5. **Model integrity:** Validate actual downloaded bytes, not file length alone, and pair receipts with verified data.
6. **Android backup rules:** Exclude health history and other sensitive files appropriately.
7. **Unchecked generation:** Never stream unchecked medical-answer text directly to TTS/UI in the new evidence-aware mode.
8. **Long conversations:** Bound state, evidence, and model tokens without dropping crucial context or overflowing JNI memory.

These are **required engineering review items**, not a claim they have all been fixed on main.

---

## 26. Security and adversarial boundaries

### 26.1 Prompt injection from user text

A user can write "ignore previous instructions" in the conversation. User messages are untrusted content; they cannot change safety, privacy, or source-attribution policies.

### 26.2 Prompt injection from medical documents

Retrieved passage text is also **data**, never system policy. Escape/separate source text from instructions and keep source URLs/IDs controlled by the trusted index metadata.

### 26.3 Cross-session leakage

Never copy user-specific cache, conversation facts or retrieved personalized context into a different session or subject.

### 26.4 Artifact supply-chain integrity

Pin actual model revisions and binary hashes; verify downloads before activation. A newly downloaded knowledge pack must be compatible with the exact embedding model/tokenizer/index schema.

### 26.5 Test data privacy

Use synthetic or consented evaluation conversations. Avoid collecting real private health information in public issues, logs, datasets or telemetry.

---

## 27. User experience and visual design principles

### 27.1 Doctor presence

Retain the current doctor-themed visual identity and compact conversational screen. The longer-term aesthetic direction is a centered half-body 3D doctor with restrained idle, greeting, listening and speaking motions, but that is **not** necessary to complete Phase 4 functional RAG.

### 27.2 Listening

Press-and-hold microphone; animate input intensity and show recognized transcript when ready. Allow correction on potentially consequential transcription uncertainty.

### 27.3 Thinking and searching

Display a simple progress state while retrieving evidence or waiting for local Qwen. Do not display private chain-of-thought or unvalidated partial health claims.

### 27.4 Asking

Show one short follow-up question in the doctor chat and speak it only after validation; clearly allow the user to decline or answer by text.

### 27.5 Answering

Show the approved educational response and concise reference links/source chips, without implying the source endorses the assistant's personal assessment.

### 27.6 Evidence Mode

Default ON for health information. An OFF toggle is acceptable for controlled comparisons, clearly labeled LLM-only and without bypassing essential safety restrictions.

### 27.7 Accessibility and language

Keep readable text, touch targets, screen-reader semantics and mute control. Telugu remains visibly **demo-only** until actual Telugu ASR, retrieval, model quality and offline speech have been evaluated.

---

## 28. Research and engineering evaluation opportunities

- Semantic-only versus lexical-only versus hybrid retrieval effectiveness.
- Corrected conversational queries versus naïve latest-turn-only queries.
- Adaptive retrieval/cache reuse versus always retrieving.
- Quantized MiniLM parity and latency/quality tradeoff.
- Qwen3-0.6B dynamic question selection and failure analysis.
- Evidence-supported claims versus unsupported direct LLM answers.
- Question budget versus user experience and task completion.
- Full on-device RAM/latency/energy/size tradeoffs.
- Error propagation from Whisper STT to retrieval and response quality.
- Evaluated English-only coverage and future multilingual adaptation.

**Research standard:** publish reproducible datasets, baselines, ablations, failure cases, source-license audit and (where feasible) qualified safety assessment. Do not publish exaggerated accuracy or diagnostic claims.

---

## 29. Future milestones (not part of revised Phase 4)

- Real Telugu speech recognition, semantic retrieval and natural-language responses after appropriate multilingual evaluation.
- Broader, professionally reviewed medical coverage and robust source update policies.
- Further context/latency optimizations based on device measurements.
- On-device acceleration if proven beneficial to battery and stability.
- Full half-body 3D doctor, restrained mouth/listen/greeting animations.
- Accessibility and localization improvements.
- A separately scoped Phase 5 **after** working Phase 4 RAG; do not repeat the same retrieval architecture in Phase 5.
- Optional future remote asset update/catalog services **only with explicit consent**; no medical chat backend is required.

---

## 30. Source documents and useful references

### Internal implementation documents

- [Phase 2 implementation report](https://github.com/shanmukhdatta/Voice_Doc_AI/blob/feature/offline-voice-baseline/PHASE2_IMPLEMENTATION.md) — on its feature branch.
- [Phase 3 implementation report](https://github.com/shanmukhdatta/Voice_Doc_AI/blob/feature/offline-llm-phase3/PHASE3_IMPLEMENTATION.md) — on its feature branch.
- [Hardware checklists](checks/) — Phase 2 and Phase 3 on main; later checklists may live on feature branches.
- [Superseded Phase 4 implementation](https://github.com/shanmukhdatta/Voice_Doc_AI/blob/feature/medical-safety-phase4/PHASE4_IMPLEMENTATION.md) — **historical experiment; not target Phase 4**.
- [Original product architecture](https://github.com/shanmukhdatta/VoiceDoc/blob/main/README.md) — broader product vision.

### Technology and source links

- [whisper.cpp](https://github.com/ggml-org/whisper.cpp)
- [llama.cpp](https://github.com/ggml-org/llama.cpp)
- [Qwen3-0.6B](https://huggingface.co/Qwen/Qwen3-0.6B)
- [all-MiniLM-L6-v2](https://huggingface.co/sentence-transformers/all-MiniLM-L6-v2)
- [ONNX Runtime Mobile](https://onnxruntime.ai/docs/tutorials/mobile/)
- [Sentence Transformers pooling](https://www.sbert.net/docs/sentence_transformer/usage/custom_models.html)
- [Android Jetpack Compose](https://developer.android.com/develop/ui/compose)
- [Android Room](https://developer.android.com/training/data-storage/room)
- [MedlinePlus XML](https://medlineplus.gov/xml.html)
- [MedlinePlus content-use policy](https://medlineplus.gov/about/using/usingcontent/)
- [SQLite FTS5](https://www.sqlite.org/fts5.html)

---

## 31. Current status, contribution discipline, and licensing

**The design is approved; implementation completeness must be established from code, assets and tests on the actual new Phase 4 branch.** Do not use a README architecture diagram as proof that a runtime module, clinical test, or mobile feature already exists.

Every implementation PR/commit should state:

1. Which section of this README was implemented.
2. Changed files and the exact commit SHA.
3. Which real tests and device checks ran, with outcomes.
4. Which assets/models were actually obtained and their hashes/licenses.
5. Actual APK, on-device model/index size, RAM and latency when measured.
6. Missing functionality, open bugs and unverified assumptions.
7. Whether protected personal data, medical licensing or safety review is implicated.

**Licensing:** Repository-code licensing must be decided separately from model and content licenses. Keep every model license, MedlinePlus content eligibility decision and third-party library notice in its appropriate artifact manifest before redistribution.

---

**Voice Doctor AI: preserve the local voice interface, add reproducible medical evidence retrieval, and build a genuine multi-turn conversation without pretending that an offline small language model is a physician.**
