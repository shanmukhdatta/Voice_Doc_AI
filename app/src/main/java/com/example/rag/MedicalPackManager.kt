package com.example.rag

import android.content.Context
import android.util.Log
import com.example.embedding.EmbeddingAssetManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.Closeable
import java.io.File
import java.io.FileOutputStream

class MedicalPackManager(
    private val context: Context,
    private val embeddingAssetManager: EmbeddingAssetManager = EmbeddingAssetManager(context)
) : Closeable {

    companion object {
        private const val TAG = "MedicalPackManager"
        const val PACK_ASSET_DIR = "medical_pack"
        const val DB_FILENAME = "medical_knowledge.db"
        const val EMBEDDINGS_FILENAME = "medical_embeddings.bin"
        const val PASSAGE_IDS_FILENAME = "passage_ids.json"
        const val MANIFEST_FILENAME = "knowledge_pack_manifest.json"
    }

    private val packDir: File
        get() = File(context.filesDir, "medical_pack").apply { mkdirs() }

    val dbFile: File get() = File(packDir, DB_FILENAME)
    val embeddingsFile: File get() = File(packDir, EMBEDDINGS_FILENAME)
    val passageIdsFile: File get() = File(packDir, PASSAGE_IDS_FILENAME)
    val manifestFile: File get() = File(packDir, MANIFEST_FILENAME)

    private var knowledgeStore: MedicalKnowledgeStore? = null
    private var vectorRetriever: VectorRetriever? = null
    private var keywordRetriever: KeywordRetriever? = null
    private var hybridRetriever: HybridRetriever? = null

    suspend fun initialize(): Result<HybridRetriever> = withContext(Dispatchers.IO) {
        try {
            extractAssetIfNeeded("$PACK_ASSET_DIR/$DB_FILENAME", dbFile)
            extractAssetIfNeeded("$PACK_ASSET_DIR/$EMBEDDINGS_FILENAME", embeddingsFile)
            extractAssetIfNeeded("$PACK_ASSET_DIR/$PASSAGE_IDS_FILENAME", passageIdsFile)
            extractAssetIfNeeded("$PACK_ASSET_DIR/$MANIFEST_FILENAME", manifestFile)

            val embeddingEngine = embeddingAssetManager.createEmbeddingEngine()
            val store = MedicalKnowledgeStore(dbFile)
            knowledgeStore = store

            val vecRetriever = VectorRetriever(embeddingsFile, passageIdsFile)
            vectorRetriever = vecRetriever

            val kwRetriever = KeywordRetriever(store)
            keywordRetriever = kwRetriever

            val hybrid = HybridRetriever(
                embeddingEngine = embeddingEngine,
                vectorRetriever = vecRetriever,
                keywordRetriever = kwRetriever,
                knowledgeStore = store
            )
            hybridRetriever = hybrid
            Result.success(hybrid)
        } catch (t: Throwable) {
            Log.w(TAG, "Medical pack initialization failed (running in fallback/degraded mode): ${t.message}")
            Result.failure(t)
        }
    }

    fun getHybridRetriever(): HybridRetriever? = hybridRetriever

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

        Log.i(TAG, "Extracting medical pack asset $assetPath -> ${targetFile.absolutePath}")
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

    override fun close() {
        try {
            knowledgeStore?.close()
        } catch (_: Throwable) {}
    }
}
