# Voice Doctor AI — Full repository folder index

This index was derived from the **actual tracked files and folders** of the Phase 4 feature branch. It includes each project-owned directory, including nested Kotlin packages, tests, data pipelines, assets and Android resources. Every folder has either a local README or a mirrored documentation README.

> **Why mirrored directories?** `app/src/main/res` is interpreted by Android's resource compiler, so adding Markdown there can break builds; extra files inside `assets` enlarge the APK. Those folders are therefore documented under `docs/folder-reference/` without changing app asset or resource packaging.

> **Third-party source policy:** The large vendored `app/src/main/cpp/llama.cpp` and `app/src/main/cpp/whisper.cpp` trees retain upstream README/license files. We do not add hundreds of modified READMEs to their upstream internals. Their integration is documented in `app/src/main/cpp/README.md`.

> **Generated Antigravity workspace:** `.aistudio/artifacts` and its temporary media files are not application modules. We do not alter generated workspace data.

## Repository's documented directories

- [`app/`](../app/README.md) — Android application module, Gradle dependencies, code shrinker rules and app build configuration.
- [`app/src/`](../app/src/README.md) — Android source sets, separating runtime code, instrumentation tests and JVM unit tests.
- [`app/src/androidTest/`](../app/src/androidTest/README.md) — On-device/instrumentation Android tests.
- [`app/src/androidTest/java/`](../app/src/androidTest/java/README.md) — Tracked Android source, test or resource directory.
- [`app/src/androidTest/java/com/`](../app/src/androidTest/java/com/README.md) — Tracked Android source, test or resource directory.
- [`app/src/androidTest/java/com/example/`](../app/src/androidTest/java/com/example/README.md) — Tracked Android source, test or resource directory.
- [`app/src/main/`](../app/src/main/README.md) — Runtime Android manifest, Kotlin source, native C++ bridges and packaged app resources.
- [`app/src/main/assets/`](folder-reference/app/src/main/assets/README.md) — Packaged offline models and medical retrieval data; copied or opened by app-private asset managers.
- [`app/src/main/assets/medical_pack/`](folder-reference/app/src/main/assets/medical_pack/README.md) — Packaged read-only MedlinePlus SQLite FTS4 database, float32 vector matrix, passage IDs and integrity manifest.
- [`app/src/main/assets/minilm/`](folder-reference/app/src/main/assets/minilm/README.md) — Quantized MiniLM ONNX embedding model and the matching WordPiece tokenizer vocabulary.
- [`app/src/main/assets/models/`](folder-reference/app/src/main/assets/models/README.md) — Whisper Tiny Q5_1 GGML model, tracked as a Git LFS object.
- [`app/src/main/cpp/`](../app/src/main/cpp/README.md) — JNI glue and CMake configuration connecting Kotlin to llama.cpp and whisper.cpp.
- [`app/src/main/java/`](../app/src/main/java/README.md) — Tracked Android source, test or resource directory.
- [`app/src/main/java/com/`](../app/src/main/java/com/README.md) — Tracked Android source, test or resource directory.
- [`app/src/main/java/com/example/`](../app/src/main/java/com/example/README.md) — Android entry Activity and all first-party Kotlin packages.
- [`app/src/main/java/com/example/components/`](../app/src/main/java/com/example/components/README.md) — Reusable doctor-themed Jetpack Compose UI widgets, chat sheets, voice input and model download screens.
- [`app/src/main/java/com/example/conversation/`](../app/src/main/java/com/example/conversation/README.md) — Structured multi-turn user facts, pending questions, contextual query construction and adaptive retrieval decisions.
- [`app/src/main/java/com/example/data/`](../app/src/main/java/com/example/data/README.md) — Opt-in Room/SQLite conversation entities, DAO, database singleton and repository; separate from the medical corpus.
- [`app/src/main/java/com/example/embedding/`](../app/src/main/java/com/example/embedding/README.md) — Android ONNX Runtime MiniLM inference, WordPiece encoding, asset installation and vector interface.
- [`app/src/main/java/com/example/medical/`](../app/src/main/java/com/example/medical/README.md) — Conversational RAG orchestration, existing Qwen planner, grounding validator and narrow safety interrupt.
- [`app/src/main/java/com/example/model/`](../app/src/main/java/com/example/model/README.md) — UI-level immutable state and chat/domain models.
- [`app/src/main/java/com/example/modelmanager/`](../app/src/main/java/com/example/modelmanager/README.md) — HTTPS Qwen GGUF download manager with progress and integrity checks.
- [`app/src/main/java/com/example/rag/`](../app/src/main/java/com/example/rag/README.md) — Medical passage types, SQLite keyword search, exact vector search, RRF fusion and offline pack management.
- [`app/src/main/java/com/example/ui/`](../app/src/main/java/com/example/ui/README.md) — Main Compose screen that renders app state and passes events to the ViewModel.
- [`app/src/main/java/com/example/ui/theme/`](../app/src/main/java/com/example/ui/theme/README.md) — Material/Compose colors, typography and visual theme configuration.
- [`app/src/main/java/com/example/viewmodel/`](../app/src/main/java/com/example/viewmodel/README.md) — The lifecycle-aware bridge between UI, audio, RAG, Qwen, Room and TTS.
- [`app/src/main/java/com/example/voice/`](../app/src/main/java/com/example/voice/README.md) — AudioRecord capture, Whisper speech-to-text, Android TTS and native Whisper bridge.
- [`app/src/main/java/com/example/voice/llm/`](../app/src/main/java/com/example/voice/llm/README.md) — Qwen3 inference via llama.cpp/JNI and streaming-filter utilities.
- [`app/src/main/res/`](folder-reference/app/src/main/res/README.md) — Android compiled resources; not a place for Markdown README files because AAPT expects resource file types.
- [`app/src/main/res/drawable/`](folder-reference/app/src/main/res/drawable/README.md) — App doctor-avatar imagery and XML vector drawable components.
- [`app/src/main/res/mipmap-anydpi-v26/`](folder-reference/app/src/main/res/mipmap-anydpi-v26/README.md) — Tracked Android source, test or resource directory.
- [`app/src/main/res/mipmap-hdpi/`](folder-reference/app/src/main/res/mipmap-hdpi/README.md) — Tracked Android source, test or resource directory.
- [`app/src/main/res/mipmap-mdpi/`](folder-reference/app/src/main/res/mipmap-mdpi/README.md) — Tracked Android source, test or resource directory.
- [`app/src/main/res/mipmap-xhdpi/`](folder-reference/app/src/main/res/mipmap-xhdpi/README.md) — Tracked Android source, test or resource directory.
- [`app/src/main/res/mipmap-xxhdpi/`](folder-reference/app/src/main/res/mipmap-xxhdpi/README.md) — Tracked Android source, test or resource directory.
- [`app/src/main/res/mipmap-xxxhdpi/`](folder-reference/app/src/main/res/mipmap-xxxhdpi/README.md) — Tracked Android source, test or resource directory.
- [`app/src/main/res/values-te/`](folder-reference/app/src/main/res/values-te/README.md) — Telugu UI demonstration strings; live Telugu ASR/RAG remains future work.
- [`app/src/main/res/values/`](folder-reference/app/src/main/res/values/README.md) — Base strings, colors and themes.
- [`app/src/main/res/xml/`](folder-reference/app/src/main/res/xml/README.md) — Android backup rules and data-extraction policies; audit these before handling stored medical conversations.
- [`app/src/test/`](../app/src/test/README.md) — JVM unit-test source root, including mocked Android interaction tests.
- [`app/src/test/java/`](../app/src/test/java/README.md) — Tracked Android source, test or resource directory.
- [`app/src/test/java/com/`](../app/src/test/java/com/README.md) — Tracked Android source, test or resource directory.
- [`app/src/test/java/com/example/`](../app/src/test/java/com/example/README.md) — Tracked Android source, test or resource directory.
- [`app/src/test/java/com/example/conversation/`](../app/src/test/java/com/example/conversation/README.md) — Tracked Android source, test or resource directory.
- [`app/src/test/java/com/example/data/`](../app/src/test/java/com/example/data/README.md) — Tracked Android source, test or resource directory.
- [`app/src/test/java/com/example/embedding/`](../app/src/test/java/com/example/embedding/README.md) — Tracked Android source, test or resource directory.
- [`app/src/test/java/com/example/medical/`](../app/src/test/java/com/example/medical/README.md) — Tracked Android source, test or resource directory.
- [`app/src/test/java/com/example/modelmanager/`](../app/src/test/java/com/example/modelmanager/README.md) — Tracked Android source, test or resource directory.
- [`app/src/test/java/com/example/voice/`](../app/src/test/java/com/example/voice/README.md) — Tracked Android source, test or resource directory.
- [`checks/`](../checks/README.md) — Unexecuted physical-device test protocols; passing unit tests do not satisfy these checkboxes.
- [`docs/`](../docs/README.md) — Engineering handoff, known gaps, size audit, and navigable architectural guides.
- [`gradle/`](../gradle/README.md) — Central Gradle dependency version catalog and wrapper tools.
- [`gradle/wrapper/`](../gradle/wrapper/README.md) — Gradle wrapper properties and distribution bootstrap jar.
- [`medical-knowledge/`](../medical-knowledge/README.md) — Prepared MedlinePlus corpus, vectors, SQLite index, passage mapping, source manifests and retrieval benchmarks.
- [`medical-knowledge/raw_source/`](../medical-knowledge/raw_source/README.md) — Original downloaded XML source snapshot for reproducible parsing and source audit.
- [`model_assets/`](../model_assets/README.md) — Development-machine model export and quantization outputs; distinct from packaged Android assets.
- [`model_assets/minilm/`](../model_assets/minilm/README.md) — Reproducible generated INT8 MiniLM ONNX model, tokenizer vocabulary and export metadata.
- [`public/`](../public/README.md) — Historical design assets from the UI prototype, not the deployed static site.
- [`public/assets/`](../public/assets/README.md) — Static design/reference assets.
- [`public/assets/aistudio/`](../public/assets/aistudio/README.md) — Image assets from the Android UI prototype; their provenance and redistribution rights should be checked.
- [`safety/`](../safety/README.md) — Research-only evaluation scenarios and audit inputs; not a substitute for qualified clinical review.
- [`safety/evaluation/`](../safety/evaluation/README.md) — Retrieval tests, multi-turn example dialogues and adversarial cases.
- [`tools/`](../tools/README.md) — Development-machine preprocessing and evaluation tools, not part of the Android runtime.
- [`tools/medical_pack/`](../tools/medical_pack/README.md) — Python scripts to download MedlinePlus XML, chunk and embed passages, build indexes, package and evaluate.
- [`tools/medical_pack/test_fixtures/`](../tools/medical_pack/test_fixtures/README.md) — Golden embedding/tokenizer fixtures for parity tests.

## Essential architectural references

- [Project architecture](../README.md)
- [Phase 4 implementation report](../PHASE4_ADAPTIVE_RAG_IMPLEMENTATION.md)
- [Phase 4 handoff and release blockers](PHASE4_HANDOFF_AND_RELEASE_GATES.md)
- [Phase 4 size audit](PHASE4_SIZE_AUDIT.md)

Do not treat source documentation as production or clinical verification.