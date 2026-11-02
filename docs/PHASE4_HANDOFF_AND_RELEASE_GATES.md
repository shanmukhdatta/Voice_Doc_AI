# Phase 4 — Cross-Laptop Handoff and Release Gates

**Saved:** 2026-11-02. **Branch:** `feature/adaptive-conversational-rag-phase4`. **Implementation commit reviewed:** `205a45dd837fa93ca668ec13504e0ba2fd82b409`.

> **Status: SOURCE-CODE HANDOFF READY; NOT RELEASE-VALIDATED.** Antigravity's report states 50 unit tests passed and a ~132 MB debug APK built. GitHub confirms implementation code and assets were pushed. This review did not re-run Gradle tests, install a physical device, or validate clinical safety.

## 1. Files safely stored on GitHub

- Kotlin/Compose application, Whisper.cpp and llama.cpp integration, conversational state, hybrid search, safety/grounding classes, model and RAG assets.
- `app/src/main/assets/minilm/model_quantized.onnx` (23,455,019 bytes) and `vocab.txt`.
- `app/src/main/assets/medical_pack/medical_knowledge.db` (3,354,624 bytes), `medical_embeddings.bin` (1,560,588 bytes), passage IDs and manifest.
- Full source XML `medical-knowledge/raw_source/mplus_topics_2026-10-31.xml` and Python pack building/evaluation scripts in `tools/medical_pack/`.
- `PHASE4_ADAPTIVE_RAG_IMPLEMENTATION.md`, `docs/PHASE4_SIZE_AUDIT.md`, medical retrieval benchmark, question and adversarial test files, and unchecked Phase 4 phone checklist.
- **Whisper asset is stored as Git LFS pointer** (`app/src/main/assets/models/ggml-tiny-q5_1.bin`); use `git lfs pull` or verify the Gradle model-download step on the new machine.
- Qwen3-0.6B GGUF is separately downloaded by the app with user consent; it is not stored in this repository.

## 2. Verified code-review issues to fix before calling Phase 4 complete

### P0: Raw unvalidated model text leaks into UI

`ConversationalRagOrchestrator.kt` emits `OrchestratorUpdate.Token` during Qwen generation; `VoiceDoctorViewModel.kt` immediately displays the fragments in the doctor message. Buffer the raw model response, parse/validate a complete ASK/ANSWER/KNOWLEDGE_GAP/URGENT action, and only then show or speak the approved result. Add an integration test proving malformed or unsupported partial outputs never reach chat or TTS.

### P0: Fabricated citation attribution

`MedicalGroundingValidator.kt` automatically appends the first retrieved passage citation when Qwen supplied no valid citation. Citation-ID membership is not factual support. Remove automatic attribution; instead only cite passages when claim support is established, narrow unsupported text, or abstain with a knowledge limitation. Clinical correctness is still unproven.

### P1: User fact extraction still uses a hardcoded symptom list

`SessionStateManager.kt` contains `commonSymptoms` and substring rules for negation/subjects. This contradicts the goal of scalable real-language conversational fact capture. Test paraphrases, corrections, multiple symptoms, new speakers, follow-up answers, age-specific ambiguity, and negation. Keep only user-span-grounded facts and use validated interpretation when necessary.

### P1: Retrieval reuse/correction logic

`SessionStateManager.applyUtterance()` clears the pending question before `AdaptiveRetrievalController.decide()` runs; the `REUSE_CACHE` branch checks for pendingQuestion != null, so it may fail to reuse as intended. Cache/retrieve decisions must compare previous and updated facts and invalidate changed topic/subject/evidence versions. Add tests.

### P1: Hybrid retrieval currently underperforms vector-only on committed benchmark

`medical-knowledge/retrieval_benchmark_results.json`: vector Recall@3 = 0.7714, MRR = 0.668; hybrid Recall@3 = 0.7333, MRR = 0.5641 (125 queries). Examine FTS ordering, RRF fusion, labels and held-out evaluation. Similarity thresholds 0.42/0.55 do not indicate medical confidence. Keep a tested vector-only fallback when it performs better.

### P1: Asset integrity and medical-source rights

`MedicalPackManager` and `EmbeddingAssetManager` often accept existing files based on file presence/size, without checking SHA-256 against a validated manifest and compatibility per model/pack version. Improve validation and corruption tests. Audit medical-source copyright/eligibility per item, rather than assuming every linked resource is public domain. Independent clinical safety and accuracy assessments remain outstanding.

### P2: Size / real-device testing

The reported debug APK is ~132 MB because RAG assets are bundled, despite earlier on-demand asset goals. 24-test ONNX parity average cosine ~0.9623 needs broader model/Android verification. Real ARM64 voice -> medical retrieval -> Qwen -> validated speech/text, airplane mode, RAM, crash resilience, microphone cancellation and TTS tests are unchecked.

## 3. Python scripts and REST clarification

**There is no `face.py`, FastAPI, remote vector DB, or REST medical inference server required.** Python scripts under `tools/medical_pack/` are for off-device model export, XML acquisition and parsing, passage chunking, vector/SQLite index preparation, packaging, verification and retrieval evaluation. The generated assets are already on the feature branch, so no Python scripts need to run merely to clone and resume. Rebuild them when modifying or independently auditing the corpus or model.

## 4. Before closing the old laptop

Run these commands in the local repository and check whether any changes remain uncommitted:

```powershell
git branch --show-current
git status --short
git log -1 --oneline
git fetch origin
git status -sb
git lfs ls-files
```

All intended work must be committed and pushed. Do not force-push or merge to main just to close the session. This handoff was committed directly to GitHub after Antigravity's implementation, so the remote branch is one documentation commit ahead of Antigravity's original local checkout; pull fast-forward when clean.

## 5. Resume on a new Windows laptop

```powershell
git clone https://github.com/shanmukhdatta/Voice_Doc_AI.git
cd Voice_Doc_AI
git lfs install
git fetch origin
git switch --track origin/feature/adaptive-conversational-rag-phase4
git lfs pull
# Install Android Studio + required JDK/SDK/NDK and build:
.\gradlew.bat testDebugUnitTest
.\gradlew.bat assembleDebug
```

Review `README.md`, `PHASE4_ADAPTIVE_RAG_IMPLEMENTATION.md`, `docs/PHASE4_SIZE_AUDIT.md`, this handoff, and `checks/PHASE4_CONVERSATIONAL_RAG_HARDWARE_CHECKS.md`. Install Qwen on the test phone through the existing consent-based model downloader. A local debug APK is not automatically a GitHub Release artifact.

**GO:** end the current desktop session once local changes are pushed; GitHub has the code, scripts and RAG assets. **NO-GO:** declare the implementation medically reliable, fully validated or ready for release before the P0/P1 issues and real-device checks are addressed.