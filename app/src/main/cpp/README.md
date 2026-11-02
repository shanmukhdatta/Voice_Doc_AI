# `app/src/main/cpp/` — Directory reference

JNI glue and CMake configuration connecting Kotlin to llama.cpp and whisper.cpp.

## Direct contents

- [`CMakeLists.txt`](CMakeLists.txt) — Native C++ target configuration and integration of upstream model runtimes.
- [`llama_jni.cpp`](llama_jni.cpp) — C++ JNI bridge for llama.cpp inference and chunked prompt evaluation.
- [`whisper_jni.cpp`](whisper_jni.cpp) — C++ JNI bridge for Whisper speech recognition.

## Immediate subfolders

None.

## Interfaces and architecture

Android Kotlin -> JNI C++ -> bundled whisper.cpp or llama.cpp C/C++ engine -> Kotlin. Third-party vendor code remains separately licensed.

## Verification and status

Consult the main architecture README for complete end-to-end flow.

**Documentation reflects committed sources, not a guarantee of correctness.** The Phase 4 prototype has open clinical grounding, conversation and device-validation tasks recorded in [the handoff report](../../../../docs/PHASE4_HANDOFF_AND_RELEASE_GATES.md).
