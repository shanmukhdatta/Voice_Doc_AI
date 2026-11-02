package com.example.embedding

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

class EmbeddingAssetManager(private val context: Context) {

    companion object {
        private const val TAG = "EmbeddingAssetManager"
        const val MODEL_ASSET = "minilm/model_quantized.onnx"
        const val VOCAB_ASSET = "minilm/vocab.txt"
    }

    private val minilmDir: File
        get() = File(context.filesDir, "minilm").apply { mkdirs() }

    fun getModelFile(): File = File(minilmDir, "model_quantized.onnx")
    fun getVocabFile(): File = File(minilmDir, "vocab.txt")

    suspend fun ensureExtracted(): Boolean = withContext(Dispatchers.IO) {
        try {
            extractAssetIfNeeded(MODEL_ASSET, getModelFile())
            extractAssetIfNeeded(VOCAB_ASSET, getVocabFile())
            getModelFile().exists() && getVocabFile().exists()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to extract embedding assets", e)
            false
        }
    }

    private fun extractAssetIfNeeded(assetPath: String, targetFile: File) {
        val assetManager = context.assets
        val assetFd = try {
            assetManager.openFd(assetPath)
        } catch (_: Exception) {
            null
        }

        val expectedSize = assetFd?.length ?: -1L
        assetFd?.close()

        if (targetFile.exists() && (expectedSize <= 0 || targetFile.length() == expectedSize)) {
            return
        }

        Log.i(TAG, "Extracting asset $assetPath -> ${targetFile.absolutePath}")
        targetFile.parentFile?.mkdirs()
        val tempFile = File(targetFile.parentFile, targetFile.name + ".tmp")
        assetManager.open(assetPath).use { input ->
            FileOutputStream(tempFile).use { output ->
                input.copyTo(output)
            }
        }
        if (targetFile.exists()) {
            targetFile.delete()
        }
        if (!tempFile.renameTo(targetFile)) {
            tempFile.copyTo(targetFile, overwrite = true)
            tempFile.delete()
        }
    }

    suspend fun createEmbeddingEngine(): MiniLmOnnxEmbeddingEngine = withContext(Dispatchers.IO) {
        ensureExtracted()
        val vocabFile = getVocabFile()
        val tokenizer = vocabFile.inputStream().use { stream ->
            WordPieceTokenizer.fromInputStream(stream)
        }
        MiniLmOnnxEmbeddingEngine(getModelFile(), tokenizer)
    }
}
