package com.example.voice

import android.content.Context
import android.util.Log
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.Executors
import kotlin.system.measureTimeMillis

interface SpeechToTextEngine {
    suspend fun initialize(): Result<Unit>

    suspend fun transcribe(
        audio: FloatArray,
        language: String = "en"
    ): Result<String>

    fun release()
}

class WhisperSpeechToTextEngine(
    private val context: Context,
    private val modelAssetName: String = "models/ggml-tiny-q5_1.bin",
    private val inferenceDispatcher: CoroutineDispatcher = Executors.newSingleThreadExecutor().asCoroutineDispatcher()
) : SpeechToTextEngine {

    companion object {
        private const val TAG = "WhisperSTT"
        private const val EXPECTED_MODEL_FILENAME = "ggml-tiny-q5_1.bin"
    }

    private var contextHandle: Long = 0L
    private var isInitialized = false
    private val lock = Any()

    override suspend fun initialize(): Result<Unit> = withContext(inferenceDispatcher) {
        synchronized(lock) {
            if (isInitialized && contextHandle != 0L) {
                return@withContext Result.success(Unit)
            }

            if (!WhisperNative.isNativeLoaded()) {
                val errorMsg = "Whisper native library (libvoice_whisper.so) is not loaded."
                Log.e(TAG, errorMsg)
                return@withContext Result.failure(IllegalStateException(errorMsg))
            }

            try {
                // Ensure the model file is extracted to local storage for fast memory-mapped access
                val modelFile = ensureModelExtracted()
                if (!modelFile.exists() || modelFile.length() == 0L) {
                    val errorMsg = "Model file does not exist or is empty: ${modelFile.absolutePath}"
                    Log.e(TAG, errorMsg)
                    return@withContext Result.failure(IllegalStateException(errorMsg))
                }

                Log.i(TAG, "Initializing Whisper context from: ${modelFile.absolutePath} (${modelFile.length()} bytes)")
                val initTimeMs = measureTimeMillis {
                    contextHandle = WhisperNative.init(modelFile.absolutePath)
                }

                if (contextHandle == 0L) {
                    val errorMsg = "WhisperNative.init returned 0 (failed to initialize context)."
                    Log.e(TAG, errorMsg)
                    return@withContext Result.failure(IllegalStateException(errorMsg))
                }

                isInitialized = true
                Log.i(TAG, "Whisper model initialized successfully in $initTimeMs ms. Handle: $contextHandle")
                Result.success(Unit)
            } catch (e: Exception) {
                Log.e(TAG, "Exception during Whisper initialization", e)
                Result.failure(e)
            }
        }
    }

    override suspend fun transcribe(
        audio: FloatArray,
        language: String
    ): Result<String> = withContext(inferenceDispatcher) {
        synchronized(lock) {
            if (!isInitialized || contextHandle == 0L) {
                return@withContext Result.failure(IllegalStateException("Whisper engine is not initialized."))
            }

            if (audio.isEmpty()) {
                return@withContext Result.success("")
            }

            // Check if audio has minimal energy (avoid processing pure silence)
            var maxSample = 0f
            for (sample in audio) {
                val absVal = kotlin.math.abs(sample)
                if (absVal > maxSample) maxSample = absVal
            }

            if (maxSample < 0.01f) {
                Log.d(TAG, "Audio energy below threshold (max=$maxSample). Treating as silence.")
                return@withContext Result.success("")
            }

            try {
                val numCores = Runtime.getRuntime().availableProcessors()
                val inferenceThreads = (numCores / 2).coerceIn(2, 4)

                var recognizedText = ""
                val latencyMs = measureTimeMillis {
                    recognizedText = WhisperNative.transcribe(
                        contextHandle = contextHandle,
                        samples = audio,
                        language = language,
                        numThreads = inferenceThreads
                    )
                }

                val cleanText = recognizedText.trim()
                Log.i(TAG, "Inference completed in $latencyMs ms for ${audio.size} samples (${audio.size / 16000.0}s). Result: '$cleanText'")
                Result.success(cleanText)
            } catch (e: Exception) {
                Log.e(TAG, "Whisper transcription failed", e)
                Result.failure(e)
            }
        }
    }

    override fun release() {
        synchronized(lock) {
            if (contextHandle != 0L) {
                try {
                    WhisperNative.release(contextHandle)
                } catch (e: Exception) {
                    Log.e(TAG, "Error releasing Whisper context", e)
                }
                contextHandle = 0L
            }
            isInitialized = false
        }
    }

    private fun ensureModelExtracted(): File {
        val targetFile = File(context.filesDir, EXPECTED_MODEL_FILENAME)
        // Check if file already exists and is non-empty
        if (targetFile.exists() && targetFile.length() > 1024 * 1024) {
            return targetFile
        }

        Log.i(TAG, "Extracting model from assets/$modelAssetName to ${targetFile.absolutePath}...")
        context.assets.open(modelAssetName).use { input ->
            FileOutputStream(targetFile).use { output ->
                input.copyTo(output)
            }
        }
        Log.i(TAG, "Model extracted successfully. Size: ${targetFile.length()} bytes.")
        return targetFile
    }
}

