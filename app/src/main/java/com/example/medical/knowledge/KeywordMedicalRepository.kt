package com.example.medical.knowledge

import android.content.res.AssetManager
import com.example.medical.model.MedicalEvidence
import com.example.medical.model.SourceId
import org.json.JSONArray
import java.io.BufferedReader
import java.io.InputStreamReader
import java.util.Locale

/**
 * In-memory keyword and lexical retrieval repository for curated medical knowledge.
 * Matches user query against topic title, keywords, and excerpt with weighted scoring
 * and a minimum threshold to prevent irrelevant hallucination grounding.
 */
class KeywordMedicalRepository(
    topicsJson: String? = null,
    assetManager: AssetManager? = null,
    assetPath: String = "medical/topics.json"
) : MedicalKnowledgeRepository {

    data class TopicEntry(
        val sourceId: String,
        val topicId: String,
        val title: String,
        val keywords: List<String>,
        val publisher: String,
        val sourceUrl: String,
        val publicationDate: String?,
        val license: String,
        val reviewStatus: String,
        val knowledgeVersion: String,
        val excerpt: String
    ) {
        fun toEvidence(): MedicalEvidence = MedicalEvidence(
            sourceId = SourceId(sourceId),
            topicId = topicId,
            title = title,
            excerpt = excerpt,
            publisher = publisher,
            sourceUrl = sourceUrl,
            publicationOrReviewDate = publicationDate,
            license = license,
            reviewStatus = reviewStatus,
            knowledgeVersion = knowledgeVersion
        )
    }

    private val entries: List<TopicEntry> = when {
        topicsJson != null -> parseJson(topicsJson)
        assetManager != null -> {
            try {
                val content = assetManager.open(assetPath).use { stream ->
                    BufferedReader(InputStreamReader(stream)).readText()
                }
                parseJson(content)
            } catch (e: Exception) {
                emptyList()
            }
        }
        else -> emptyList()
    }

    override fun getTopicCount(): Int = entries.size

    override fun retrieve(query: String, topK: Int): List<MedicalEvidence> {
        val normalizedQuery = query.lowercase(Locale.ROOT).trim()
        if (normalizedQuery.isBlank() || entries.isEmpty() || topK <= 0) {
            return emptyList()
        }

        val tokens = tokenize(normalizedQuery)
        if (tokens.isEmpty()) {
            return emptyList()
        }

        val scored = entries.mapNotNull { entry ->
            val score = calculateScore(entry, normalizedQuery, tokens)
            if (score >= MIN_SCORE_THRESHOLD) {
                entry to score
            } else {
                null
            }
        }.sortedByDescending { it.second }

        return scored.take(topK).map { it.first.toEvidence() }
    }

    private fun calculateScore(entry: TopicEntry, rawQuery: String, tokens: Set<String>): Double {
        var score = 0.0

        val lowerTitle = entry.title.lowercase(Locale.ROOT)
        val lowerExcerpt = entry.excerpt.lowercase(Locale.ROOT)
        val lowerKeywords = entry.keywords.map { it.lowercase(Locale.ROOT) }

        // 1. Exact phrase matches in keywords or title
        for (kw in lowerKeywords) {
            if (rawQuery.contains(kw) || kw.contains(rawQuery)) {
                score += 15.0
            }
        }
        if (rawQuery.contains(lowerTitle)) {
            score += 20.0
        }

        // 2. Token overlap matches
        for (token in tokens) {
            // Keyword token match
            val keywordMatch = lowerKeywords.any { kw ->
                kw.split("\\s+".toRegex()).any { it == token }
            }
            if (keywordMatch) {
                score += 5.0
            }

            // Title token match
            if (lowerTitle.contains(token)) {
                score += 4.0
            }

            // Excerpt token match
            if (lowerExcerpt.contains(token)) {
                score += 1.0
            }
        }

        return score
    }

    private fun tokenize(text: String): Set<String> {
        return text.replace(PUNCTUATION_REGEX, " ")
            .split("\\s+".toRegex())
            .filter { it.length > 2 && it !in STOPWORDS }
            .toSet()
    }

    companion object {
        private const val MIN_SCORE_THRESHOLD = 4.0
        private val PUNCTUATION_REGEX = Regex("[^a-zA-Z0-9\\s]")

        private val STOPWORDS = setOf(
            "the", "and", "for", "with", "that", "this", "have", "has", "had",
            "what", "when", "where", "which", "who", "whom", "whose", "why",
            "how", "can", "could", "should", "would", "about", "been", "being",
            "are", "was", "were", "you", "your", "they", "them", "some", "any",
            "from", "into", "over", "after", "before", "more", "most", "other",
            "such", "only", "same", "than", "too", "very", "just", "does", "did"
        )

        fun parseJson(jsonString: String): List<TopicEntry> {
            val list = mutableListOf<TopicEntry>()
            val array = JSONArray(jsonString)
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                val keywordsArray = obj.optJSONArray("keywords")
                val keywords = mutableListOf<String>()
                if (keywordsArray != null) {
                    for (k in 0 until keywordsArray.length()) {
                        keywords.add(keywordsArray.getString(k))
                    }
                }

                list.add(
                    TopicEntry(
                        sourceId = obj.getString("sourceId"),
                        topicId = obj.getString("topicId"),
                        title = obj.getString("title"),
                        keywords = keywords,
                        publisher = obj.optString("publisher", "MedlinePlus"),
                        sourceUrl = obj.optString("sourceUrl", ""),
                        publicationDate = if (obj.has("publicationDate") && !obj.isNull("publicationDate")) obj.getString("publicationDate") else null,
                        license = obj.optString("license", "Public Domain"),
                        reviewStatus = obj.optString("reviewStatus", "pending_qualified_review"),
                        knowledgeVersion = obj.optString("knowledgeVersion", "1.0"),
                        excerpt = obj.getString("excerpt")
                    )
                )
            }
            return list
        }
    }
}
