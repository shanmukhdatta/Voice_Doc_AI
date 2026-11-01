cmake_minimum_required(VERSION 3.22.1)
project(voice_app)

set(CMAKE_CXX_STANDARD 17)
set(CMAKE_C_STANDARD 11)

# Options for llama.cpp
set(LLAMA_BUILD_TESTS OFF CACHE BOOL "llama: build tests" FORCE)
set(LLAMA_BUILD_EXAMPLES OFF CACHE BOOL "llama: build examples" FORCE)
set(LLAMA_BUILD_SERVER OFF CACHE BOOL "llama: build server" FORCE)
set(GGML_LLAMAFILE OFF CACHE BOOL "ggml: build llamafile" FORCE)
set(LLAMA_BUILD_COMMON ON CACHE BOOL "llama: build common" FORCE)

add_subdirectory(llama.cpp build-llama EXCLUDE_FROM_ALL)

# --- Library 1: voice_whisper ---
set(WHISPER_DIR ${CMAKE_CURRENT_SOURCE_DIR}/whisper.cpp)

set(WHISPER_SOURCE_FILES
    ${WHISPER_DIR}/src/whisper.cpp
    ${WHISPER_DIR}/ggml/src/ggml.c
    ${WHISPER_DIR}/ggml/src/ggml-alloc.c
    ${WHISPER_DIR}/ggml/src/ggml-backend.cpp
    ${WHISPER_DIR}/ggml/src/ggml-backend-reg.cpp
    ${WHISPER_DIR}/ggml/src/ggml-quants.c
    ${WHISPER_DIR}/ggml/src/ggml-threading.cpp
    ${WHISPER_DIR}/ggml/src/ggml-cpu/ggml-cpu.c
    ${WHISPER_DIR}/ggml/src/ggml-cpu/ggml-cpu.cpp
    ${WHISPER_DIR}/ggml/src/ggml-cpu/ggml-cpu-quants.c
    ${WHISPER_DIR}/ggml/src/ggml-cpu/ggml-cpu-traits.cpp
    ${CMAKE_CURRENT_SOURCE_DIR}/whisper_jni.cpp
)

if (${ANDROID_ABI} STREQUAL "arm64-v8a")
    list(APPEND WHISPER_SOURCE_FILES
        ${WHISPER_DIR}/ggml/src/ggml-cpu/ggml-cpu-aarch64.cpp
    )
endif()

if (${ANDROID_ABI} STREQUAL "x86_64" OR ${ANDROID_ABI} STREQUAL "x86")
    list(APPEND WHISPER_SOURCE_FILES
        ${WHISPER_DIR}/ggml/src/ggml-cpu/cpu-feats-x86.cpp
    )
endif()

add_library(voice_whisper SHARED ${WHISPER_SOURCE_FILES})

find_library(LOG_LIB log)
target_link_libraries(voice_whisper ${LOG_LIB} android)

target_include_directories(voice_whisper PRIVATE
    ${WHISPER_DIR}
    ${WHISPER_DIR}/src
    ${WHISPER_DIR}/include
    ${WHISPER_DIR}/ggml/include
    ${WHISPER_DIR}/ggml/src
    ${WHISPER_DIR}/ggml/src/ggml-cpu
)

if (${ANDROID_ABI} STREQUAL "arm64-v8a")
    target_compile_definitions(voice_whisper PRIVATE __ARM_NEON)
endif()

# Hidden symbol visibility for whisper to prevent collision with llama ggml
target_compile_options(voice_whisper PRIVATE -fvisibility=hidden -fvisibility-inlines-hidden)

# --- Library 2: voice_llm ---
add_library(voice_llm SHARED llama_jni.cpp)

target_link_libraries(voice_llm
    llama
    common
    ${LOG_LIB}
    android
)

target_include_directories(voice_llm PRIVATE
    llama.cpp/include
    llama.cpp/common
    llama.cpp/ggml/include
)

target_compile_options(voice_llm PRIVATE -fvisibility=hidden -fvisibility-inlines-hidden)

if (NOT ${CMAKE_BUILD_TYPE} STREQUAL "Debug")
    target_compile_options(voice_whisper PRIVATE -O3 -ffunction-sections -fdata-sections)
    target_link_options(voice_whisper PRIVATE -Wl,--gc-sections)
    target_compile_options(voice_llm PRIVATE -O3 -ffunction-sections -fdata-sections)
    target_link_options(voice_llm PRIVATE -Wl,--gc-sections)
endif()
