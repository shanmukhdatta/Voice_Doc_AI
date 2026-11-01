package com.example.voice.llm

import android.util.Log
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import java.io.File
import java.util.concurrent.Executors
import kotlin.concurrent.thread

class LlamaCppEngine(
    private val native: LlamaNative = LlamaNative(),
    private val nCtx: Int = 2048,
    private val nThreads: Int = 4
) : LocalLlmEngine {

    companion object {
        private const val TAG = "LlamaCppEngine"

        const val SYSTEM_PROMPT =
            "You are Dr. Aisha, an empathetic, helpful, and concise medical AI assistant. " +
            "Provide practical health guidance in 2 to 4 clear sentences. " +
            "Always advise the user to seek emergency medical attention if symptoms are severe or life-threatening. " +
            "Do not output reasoning or thinking tags."

        fun formatPrompt(userPrompt: String): String {
            return "<|im_start|>system\n$SYSTEM_PROMPT<|im_end|>\n" +
                   "<|im_start|>user\n${userPrompt.trim()}<|im_end|>\n" +
                   "<|im_start|>assistant\n"
        }
    }

    private val _isModelLoaded = MutableStateFlow(false)
    override val isModelLoaded: StateFlow<Boolean> = _isModelLoaded.asStateFlow()

    private val inferenceDispatcher: CoroutineDispatcher = Executors.newSingleThreadExecutor { runnable ->
        thread(start = false, name = "VoiceDoc-LLM") {
            try {
                native.logToAndroid()
                native.backendInit()
            } catch (t: Throwable) {
                Log.e(TAG, "Native backendInit failed", t)
            }
            runnable.run()
        }
    }.asCoroutineDispatcher()

    private var modelPtr: Long = 0L
    private var contextPtr: Long = 0L
    private var batchPtr: Long = 0L
    private var samplerPtr: Long = 0L

    override suspend fun loadModel(path: String): Result<Unit> = withContext(inferenceDispatcher) {
        if (_isModelLoaded.value) {
            return@withContext Result.success(Unit)
        }
        val file = File(path)
        if (!file.exists()) {
            return@withContext Result.failure(IllegalArgumentException("Model file does not exist: $path"))
        }

        try {
            Log.i(TAG, "Loading model: $path")
            modelPtr = native.loadModel(path)
            if (modelPtr == 0L) {
                return@withContext Result.failure(IllegalStateException("Native loadModel returned 0"))
            }

            contextPtr = native.newContext(modelPtr, nCtx, nThreads)
            if (contextPtr == 0L) {
                native.freeModel(modelPtr)
                modelPtr = 0L
                return@withContext Result.failure(IllegalStateException("Native newContext returned 0"))
            }

            batchPtr = native.newBatch(512, 0, 1)
            if (batchPtr == 0L) {
                native.freeContext(contextPtr)
                native.freeModel(modelPtr)
                contextPtr = 0L
                modelPtr = 0L
                return@withContext Result.failure(IllegalStateException("Native newBatch returned 0"))
            }

            samplerPtr = native.newSampler(0.0f, 0.9f)
            if (samplerPtr == 0L) {
                native.freeBatch(batchPtr)
                native.freeContext(contextPtr)
                native.freeModel(modelPtr)
                batchPtr = 0L
                contextPtr = 0L
                modelPtr = 0L
                return@withContext Result.failure(IllegalStateException("Native newSampler returned 0"))
            }

            _isModelLoaded.value = true
            Log.i(TAG, "Model loaded successfully: $path")
            Result.success(Unit)
        } catch (e: Throwable) {
            Log.e(TAG, "Exception while loading model", e)
            unloadInternal()
            Result.failure(e)
        }
    }

    override suspend fun unloadModel() = withContext(inferenceDispatcher) {
        unloadInternal()
    }

    private fun unloadInternal() {
        if (samplerPtr != 0L) {
            native.freeSampler(samplerPtr)
            samplerPtr = 0L
        }
        if (batchPtr != 0L) {
            native.freeBatch(batchPtr)
            batchPtr = 0L
        }
        if (contextPtr != 0L) {
            native.freeContext(contextPtr)
            contextPtr = 0L
        }
        if (modelPtr != 0L) {
            native.freeModel(modelPtr)
            modelPtr = 0L
        }
        _isModelLoaded.value = false
        Log.i(TAG, "Model unloaded and resources freed")
    }

    override fun generate(prompt: String, maxTokens: Int): Flow<String> = flow {
        if (!_isModelLoaded.value || contextPtr == 0L || batchPtr == 0L || samplerPtr == 0L) {
            Log.w(TAG, "Cannot generate: model not loaded")
            return@flow
        }

        val formattedPrompt = formatPrompt(prompt)
        val filter = ThinkingTagFilter()

        try {
            val promptTokens = native.completionInit(contextPtr, batchPtr, formattedPrompt, true)
            if (promptTokens <= 0) {
                Log.e(TAG, "completionInit failed with return code $promptTokens")
                return@flow
            }

            var nCur = promptTokens
            val limit = promptTokens + maxTokens

            while (nCur < limit && currentCoroutineContext().isActive) {
                val piece = native.completionLoop(contextPtr, batchPtr, samplerPtr, nCur, limit)
                if (piece == null) {
                    break
                }
                nCur++
                if (piece.isNotEmpty()) {
                    val filtered = filter.processChunk(piece)
                    if (filtered.isNotEmpty()) {
                        emit(filtered)
                    }
                }
            }

            val finalChunk = filter.finish()
            if (finalChunk.isNotEmpty()) {
                emit(finalChunk)
            }
        } finally {
            if (contextPtr != 0L) {
                native.kvCacheClear(contextPtr)
            }
        }
    }.flowOn(inferenceDispatcher)
}

