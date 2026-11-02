package com.example.rag

import com.example.embedding.TextEmbeddingEngine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class HybridRetriever(
    private val embeddingEngine: TextEmbeddingEngine,
    private val vectorRetriever: VectorRetriever,
    private val keywordRetriever: KeywordRetriever,
    private val knowledgeStore: MedicalKnowledgeStore
) {
    companion object {
        const val VECTOR_HIGH_CONFIDENCE_THRESHOLD = 0.55f
        const val VECTOR_WEAK_THRESHOLD = 0.42f
        const val RRF_K = 60
    }

    suspend fun retrieve(query: String, topK: Int = 3): EvidenceBundle = withContext(Dispatchers.Default) {
        val cleanQuery = query.trim()
        if (cleanQuery.isEmpty()) {
            return@withContext EvidenceBundle.EMPTY
        }

        // 1. Vector Search
        val queryVector = try {
            embeddingEngine.embed(cleanQuery)
        } catch (_: Exception) {
            return@withContext EvidenceBundle.UNAVAILABLE
        }

        val vectorHits = vectorRetriever.search(queryVector, topK = 10)
        val topVectorScore = vectorHits.firstOrNull()?.second ?: 0f

        val status = when {
            topVectorScore >= VECTOR_HIGH_CONFIDENCE_THRESHOLD -> EvidenceStatus.RELEVANT
            topVectorScore >= VECTOR_WEAK_THRESHOLD -> EvidenceStatus.WEAK_OR_CONFLICTING
            else -> EvidenceStatus.NONE
        }

        if (status == EvidenceStatus.NONE) {
            return@withContext EvidenceBundle(
                passages = emptyList(),
                status = EvidenceStatus.NONE,
                retrievalQuery = cleanQuery,
                topScore = topVectorScore
            )
        }

        // 2. Keyword Search
        val keywordHits = keywordRetriever.search(cleanQuery, topK = 10)

        // 3. Reciprocal Rank Fusion (RRF)
        val rrfScores = mutableMapOf<String, Float>()

        vectorHits.forEachIndexed { rank, (passageId, _) ->
            val score = 1.0f / (RRF_K + (rank + 1))
            rrfScores[passageId] = (rrfScores[passageId] ?: 0f) + score
        }

        keywordHits.forEachIndexed { rank, passage ->
            val score = 1.0f / (RRF_K + (rank + 1))
            rrfScores[passage.id] = (rrfScores[passage.id] ?: 0f) + score
        }

        val sortedPassageIds = rrfScores.entries
            .sortedByDescending { it.value }
            .take(topK)
            .map { it.key }

        val passageMap = knowledgeStore.getPassages(sortedPassageIds)
        val vectorScoresMap = vectorHits.toMap()

        val finalPassages = sortedPassageIds.mapNotNull { id ->
            passageMap[id]?.copy(score = vectorScoresMap[id] ?: rrfScores[id] ?: 0f)
        }

        EvidenceBundle(
            passages = finalPassages,
            status = status,
            retrievalQuery = cleanQuery,
            topScore = topVectorScore
        )
    }
}
