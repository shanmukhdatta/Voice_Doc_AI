#include <jni.h>
#include <android/asset_manager.h>
#include <android/asset_manager_jni.h>
#include <android/log.h>
#include <stdlib.h>
#include <string.h>
#include <string>
#include <vector>
#include "whisper.h"
#include "ggml.h"

#define TAG "WhisperJNI"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO,  TAG, __VA_ARGS__)
#define LOGW(...) __android_log_print(ANDROID_LOG_WARN,  TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, TAG, __VA_ARGS__)

static size_t asset_read(void *ctx, void *output, size_t read_size) {
    return (size_t) AAsset_read((AAsset *) ctx, output, read_size);
}

static bool asset_is_eof(void *ctx) {
    return AAsset_getRemainingLength64((AAsset *) ctx) <= 0;
}

static void asset_close(void *ctx) {
    AAsset_close((AAsset *) ctx);
}

extern "C" {

JNIEXPORT jlong JNICALL
Java_com_example_voice_WhisperNative_init(
        JNIEnv *env,
        jobject /* thiz */,
        jstring model_path_str) {
    if (model_path_str == nullptr) {
        LOGE("Model path cannot be null.");
        return 0;
    }

    const char *model_path_chars = env->GetStringUTFChars(model_path_str, nullptr);
    LOGI("Initializing Whisper context from file: %s", model_path_chars);

    struct whisper_context_params cparams = whisper_context_default_params();
    struct whisper_context *context = whisper_init_from_file_with_params(model_path_chars, cparams);

    env->ReleaseStringUTFChars(model_path_str, model_path_chars);

    if (context == nullptr) {
        LOGE("Failed to create Whisper context from file.");
        return 0;
    }

    LOGI("Whisper context initialized successfully. Pointer: %p", context);
    return (jlong) context;
}

JNIEXPORT jlong JNICALL
Java_com_example_voice_WhisperNative_initFromAsset(
        JNIEnv *env,
        jobject /* thiz */,
        jobject asset_manager_obj,
        jstring asset_path_str) {
    if (asset_manager_obj == nullptr || asset_path_str == nullptr) {
        LOGE("AssetManager or asset path is null.");
        return 0;
    }

    AAssetManager *asset_manager = AAssetManager_fromJava(env, asset_manager_obj);
    if (asset_manager == nullptr) {
        LOGE("Failed to get native AAssetManager.");
        return 0;
    }

    const char *asset_path_chars = env->GetStringUTFChars(asset_path_str, nullptr);
    LOGI("Initializing Whisper context from asset: %s", asset_path_chars);

    AAsset *asset = AAssetManager_open(asset_manager, asset_path_chars, AASSET_MODE_STREAMING);
    if (asset == nullptr) {
        LOGE("Failed to open asset: %s", asset_path_chars);
        env->ReleaseStringUTFChars(asset_path_str, asset_path_chars);
        return 0;
    }

    struct whisper_model_loader loader = {
        /* .context = */ asset,
        /* .read    = */ &asset_read,
        /* .eof     = */ &asset_is_eof,
        /* .close   = */ &asset_close
    };

    struct whisper_context_params cparams = whisper_context_default_params();
    struct whisper_context *context = whisper_init_with_params(&loader, cparams);

    env->ReleaseStringUTFChars(asset_path_str, asset_path_chars);

    if (context == nullptr) {
        LOGE("Failed to create Whisper context from asset loader.");
        return 0;
    }

    LOGI("Whisper context initialized from asset successfully.");
    return (jlong) context;
}

JNIEXPORT jstring JNICALL
Java_com_example_voice_WhisperNative_transcribe(
        JNIEnv *env,
        jobject /* thiz */,
        jlong context_handle,
        jfloatArray samples_array,
        jstring language_str,
        jint num_threads) {
    if (context_handle == 0) {
        LOGE("Invalid Whisper context handle (0).");
        return env->NewStringUTF("");
    }

    if (samples_array == nullptr) {
        LOGW("Audio samples array is null.");
        return env->NewStringUTF("");
    }

    jsize sample_count = env->GetArrayLength(samples_array);
    if (sample_count == 0) {
        LOGW("Audio sample count is 0.");
        return env->NewStringUTF("");
    }

    struct whisper_context *context = (struct whisper_context *) context_handle;

    const char *lang_chars = "en";
    if (language_str != nullptr) {
        lang_chars = env->GetStringUTFChars(language_str, nullptr);
    }

    jfloat *audio_samples = env->GetFloatArrayElements(samples_array, nullptr);

    struct whisper_full_params params = whisper_full_default_params(WHISPER_SAMPLING_GREEDY);
    params.print_realtime   = false;
    params.print_progress   = false;
    params.print_timestamps = false;
    params.print_special    = false;
    params.translate        = false;
    params.language         = lang_chars;
    params.n_threads        = num_threads > 0 ? num_threads : 4;
    params.offset_ms        = 0;
    params.no_context       = true;
    params.single_segment   = false;

    whisper_reset_timings(context);

    LOGI("Starting whisper_full inference with %d samples on %d threads, lang='%s'",
         sample_count, params.n_threads, lang_chars);

    int result = whisper_full(context, params, audio_samples, sample_count);

    std::string recognized_text;
    if (result != 0) {
        LOGE("whisper_full failed with code: %d", result);
    } else {
        int n_segments = whisper_full_n_segments(context);
        for (int i = 0; i < n_segments; ++i) {
            const char *seg_text = whisper_full_get_segment_text(context, i);
            if (seg_text != nullptr) {
                recognized_text += seg_text;
            }
        }
        LOGI("Whisper inference complete. Segment count: %d, Result length: %zu",
             n_segments, recognized_text.length());
    }

    env->ReleaseFloatArrayElements(samples_array, audio_samples, JNI_ABORT);
    if (language_str != nullptr) {
        env->ReleaseStringUTFChars(language_str, lang_chars);
    }

    return env->NewStringUTF(recognized_text.c_str());
}

JNIEXPORT void JNICALL
Java_com_example_voice_WhisperNative_release(
        JNIEnv * /* env */,
        jobject /* thiz */,
        jlong context_handle) {
    if (context_handle != 0) {
        struct whisper_context *context = (struct whisper_context *) context_handle;
        LOGI("Releasing Whisper context: %p", context);
        whisper_free(context);
    }
}

JNIEXPORT jstring JNICALL
Java_com_example_voice_WhisperNative_getSystemInfo(
        JNIEnv *env,
        jobject /* thiz */) {
    const char *info = whisper_print_system_info();
    return env->NewStringUTF(info != nullptr ? info : "");
}

} // extern "C"

