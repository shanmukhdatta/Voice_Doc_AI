cmake_minimum_required(VERSION 3.10)
project(voice_whisper)

set(CMAKE_CXX_STANDARD 17)
set(CMAKE_C_STANDARD 11)

set(WHISPER_DIR ${CMAKE_CURRENT_SOURCE_DIR}/whisper.cpp)

set(SOURCE_FILES
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
    list(APPEND SOURCE_FILES
        ${WHISPER_DIR}/ggml/src/ggml-cpu/ggml-cpu-aarch64.cpp
    )
endif()

if (${ANDROID_ABI} STREQUAL "x86_64" OR ${ANDROID_ABI} STREQUAL "x86")
    list(APPEND SOURCE_FILES
        ${WHISPER_DIR}/ggml/src/ggml-cpu/cpu-feats-x86.cpp
    )
endif()

add_library(voice_whisper SHARED ${SOURCE_FILES})

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

if (NOT ${CMAKE_BUILD_TYPE} STREQUAL "Debug")
    target_compile_options(voice_whisper PRIVATE -O3 -fvisibility=hidden -ffunction-sections -fdata-sections)
    target_link_options(voice_whisper PRIVATE -Wl,--gc-sections)
endif()

