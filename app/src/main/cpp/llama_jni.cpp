#include <android/log.h>
#include <jni.h>
#include <algorithm>
#include <iomanip>
#include <cmath>
#include <string>
#include <unistd.h>
#include "llama.h"
#include "common.h"

#define TAG "LlamaJni"
#define LOGi(...) __android_log_print(ANDROID_LOG_INFO, TAG, __VA_ARGS__)
#define LOGe(...) __android_log_print(ANDROID_LOG_ERROR, TAG, __VA_ARGS__)

static std::string cached_token_chars;

static bool is_valid_utf8(const char * string) {
    if (!string) {
        return true;
    }

    const auto * bytes = reinterpret_cast<const unsigned char *>(string);
    int num;

    while (*bytes != 0x00) {
        if ((*bytes & 0x80) == 0x00) {
            num = 1;
        } else if ((*bytes & 0xE0) == 0xC0) {
            num = 2;
        } else if ((*bytes & 0xF0) == 0xE0) {
            num = 3;
        } else if ((*bytes & 0xF8) == 0xF0) {
            num = 4;
        } else {
            return false;
        }

        bytes += 1;
        for (int i = 1; i < num; ++i) {
            if ((*bytes & 0xC0) != 0x80) {
                return false;
            }
            bytes += 1;
        }
    }

    return true;
}

static void log_callback(ggml_log_level level, const char * fmt, void * data) {
    if (level == GGML_LOG_LEVEL_ERROR)     __android_log_print(ANDROID_LOG_ERROR, TAG, fmt, data);
    else if (level == GGML_LOG_LEVEL_INFO) __android_log_print(ANDROID_LOG_INFO, TAG, fmt, data);
    else if (level == GGML_LOG_LEVEL_WARN) __android_log_print(ANDROID_LOG_WARN, TAG, fmt, data);
    else __android_log_print(ANDROID_LOG_DEFAULT, TAG, fmt, data);
}

extern "C" {

JNIEXPORT void JNICALL
Java_com_example_voice_llm_LlamaNative_logToAndroid(JNIEnv *, jobject) {
    llama_log_set(log_callback, nullptr);
}

JNIEXPORT jstring JNICALL
Java_com_example_voice_llm_LlamaNative_systemInfo(JNIEnv *env, jobject) {
    return env->NewStringUTF(llama_print_system_info());
}

JNIEXPORT void JNICALL
Java_com_example_voice_llm_LlamaNative_backendInit(JNIEnv *, jobject) {
    llama_backend_init();
}

JNIEXPORT void JNICALL
Java_com_example_voice_llm_LlamaNative_backendFree(JNIEnv *, jobject) {
    llama_backend_free();
}

JNIEXPORT jlong JNICALL
Java_com_example_voice_llm_LlamaNative_loadModel(JNIEnv *env, jobject, jstring jpath) {
    llama_model_params model_params = llama_model_default_params();
    const char *path = env->GetStringUTFChars(jpath, nullptr);
    if (!path) return 0;

    LOGi("Loading llama model from: %s", path);
    auto model = llama_model_load_from_file(path, model_params);
    env->ReleaseStringUTFChars(jpath, path);

    if (!model) {
        LOGe("llama_model_load_from_file failed");
        return 0;
    }
    return reinterpret_cast<jlong>(model);
}

JNIEXPORT void JNICALL
Java_com_example_voice_llm_LlamaNative_freeModel(JNIEnv *, jobject, jlong jmodel) {
    if (jmodel != 0) {
        llama_model_free(reinterpret_cast<llama_model *>(jmodel));
    }
}

JNIEXPORT jlong JNICALL
Java_com_example_voice_llm_LlamaNative_newContext(
        JNIEnv *,
        jobject,
        jlong jmodel,
        jint n_ctx,
        jint n_threads) {
    auto model = reinterpret_cast<llama_model *>(jmodel);
    if (!model) {
        LOGe("newContext: model is null");
        return 0;
    }

    int threads = n_threads;
    if (threads <= 0) {
        threads = std::max(1, std::min(4, (int) sysconf(_SC_NPROCESSORS_ONLN) - 1));
    }

    llama_context_params ctx_params = llama_context_default_params();
    ctx_params.n_ctx = (n_ctx > 0) ? n_ctx : 2048;
    ctx_params.n_threads = threads;
    ctx_params.n_threads_batch = threads;

    LOGi("Creating context: n_ctx=%d, threads=%d", ctx_params.n_ctx, threads);
    llama_context *context = llama_init_from_model(model, ctx_params);
    if (!context) {
        LOGe("llama_new_context_with_model returned null");
        return 0;
    }
    return reinterpret_cast<jlong>(context);
}

JNIEXPORT void JNICALL
Java_com_example_voice_llm_LlamaNative_freeContext(JNIEnv *, jobject, jlong jcontext) {
    if (jcontext != 0) {
        llama_free(reinterpret_cast<llama_context *>(jcontext));
    }
}

JNIEXPORT jlong JNICALL
Java_com_example_voice_llm_LlamaNative_newBatch(JNIEnv *, jobject, jint n_tokens, jint embd, jint n_seq_max) {
    auto *batch = new llama_batch(llama_batch_init(n_tokens, embd, n_seq_max));
    return reinterpret_cast<jlong>(batch);
}

JNIEXPORT void JNICALL
Java_com_example_voice_llm_LlamaNative_freeBatch(JNIEnv *, jobject, jlong jbatch) {
    if (jbatch != 0) {
        auto *batch = reinterpret_cast<llama_batch *>(jbatch);
        llama_batch_free(*batch);
        delete batch;
    }
}

JNIEXPORT jlong JNICALL
Java_com_example_voice_llm_LlamaNative_newSampler(JNIEnv *, jobject, jfloat temp, jfloat top_p) {
    auto sparams = llama_sampler_chain_default_params();
    sparams.no_perf = true;
    llama_sampler *smpl = llama_sampler_chain_init(sparams);
    if (temp <= 0.01f) {
        llama_sampler_chain_add(smpl, llama_sampler_init_greedy());
    } else {
        llama_sampler_chain_add(smpl, llama_sampler_init_top_k(40));
        llama_sampler_chain_add(smpl, llama_sampler_init_top_p(top_p, 1));
        llama_sampler_chain_add(smpl, llama_sampler_init_temp(temp));
        llama_sampler_chain_add(smpl, llama_sampler_init_dist(42));
    }
    return reinterpret_cast<jlong>(smpl);
}

JNIEXPORT void JNICALL
Java_com_example_voice_llm_LlamaNative_freeSampler(JNIEnv *, jobject, jlong jsampler) {
    if (jsampler != 0) {
        llama_sampler_free(reinterpret_cast<llama_sampler *>(jsampler));
    }
}

JNIEXPORT jint JNICALL
Java_com_example_voice_llm_LlamaNative_completionInit(
        JNIEnv *env,
        jobject,
        jlong context_pointer,
        jlong batch_pointer,
        jstring jtext,
        jboolean parse_special) {

    cached_token_chars.clear();

    const char *text = env->GetStringUTFChars(jtext, nullptr);
    if (!text) return -1;

    auto *context = reinterpret_cast<llama_context *>(context_pointer);
    auto *batch = reinterpret_cast<llama_batch *>(batch_pointer);

    const auto tokens_list = common_tokenize(context, text, true, parse_special == JNI_TRUE);
    env->ReleaseStringUTFChars(jtext, text);

    const size_t total_tokens = tokens_list.size();
    auto n_ctx = llama_n_ctx(context);
    // Reserve at least 64 tokens for generation
    if (total_tokens + 64 > static_cast<size_t>(n_ctx)) {
        LOGe("Prompt size (%zu) + reserve (64) exceeds context window (%d)", total_tokens, n_ctx);
        return -2;
    }

    const size_t batch_capacity = 512;

    for (size_t chunk_start = 0; chunk_start < total_tokens; chunk_start += batch_capacity) {
        const size_t chunk_end = std::min(chunk_start + batch_capacity, total_tokens);
        common_batch_clear(*batch);

        for (size_t i = chunk_start; i < chunk_end; ++i) {
            const bool is_last = (i == total_tokens - 1);
            common_batch_add(*batch, tokens_list[i], static_cast<llama_pos>(i), { 0 }, is_last);
        }

        if (llama_decode(context, *batch) != 0) {
            LOGe("llama_decode failed on prompt chunk at %zu", chunk_start);
            return -1;
        }
    }

    return static_cast<jint>(total_tokens);
}

JNIEXPORT jstring JNICALL
Java_com_example_voice_llm_LlamaNative_completionLoop(
        JNIEnv *env,
        jobject,
        jlong context_pointer,
        jlong batch_pointer,
        jlong sampler_pointer,
        jint n_cur,
        jint max_tokens) {

    auto *context = reinterpret_cast<llama_context *>(context_pointer);
    auto *batch   = reinterpret_cast<llama_batch *>(batch_pointer);
    auto *sampler = reinterpret_cast<llama_sampler *>(sampler_pointer);
    const auto *model = llama_get_model(context);
    const auto *vocab = llama_model_get_vocab(model);

    if (n_cur >= max_tokens) {
        return nullptr;
    }

    const llama_token new_token_id = llama_sampler_sample(sampler, context, -1);
    llama_sampler_accept(sampler, new_token_id);

    if (llama_vocab_is_eog(vocab, new_token_id)) {
        return nullptr;
    }

    auto new_token_chars = common_token_to_piece(context, new_token_id);
    cached_token_chars += new_token_chars;

    jstring result = nullptr;
    if (is_valid_utf8(cached_token_chars.c_str())) {
        result = env->NewStringUTF(cached_token_chars.c_str());
        cached_token_chars.clear();
    } else {
        result = env->NewStringUTF("");
    }

    common_batch_clear(*batch);
    common_batch_add(*batch, new_token_id, static_cast<llama_pos>(n_cur), { 0 }, true);

    if (llama_decode(context, *batch) != 0) {
        LOGe("llama_decode failed during token generation");
        return nullptr;
    }

    return result;
}

JNIEXPORT void JNICALL
Java_com_example_voice_llm_LlamaNative_kvCacheClear(JNIEnv *, jobject, jlong jcontext) {
    if (jcontext != 0) {
        llama_kv_self_clear(reinterpret_cast<llama_context *>(jcontext));
    }
}

} // extern "C"
