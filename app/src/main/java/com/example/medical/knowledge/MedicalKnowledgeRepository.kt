package com.example.medical.knowledge

import com.example.medical.model.MedicalEvidence

/**
 * Contract for retrieving curated, verified-source medical evidence.
 */
interface MedicalKnowledgeRepository {
    /**
     * Retrieves up to [topK] evidence items most relevant to [query].
     * Returns an empty list if no topic exceeds the relevance threshold.
     */
    fun retrieve(query: String, topK: Int = 3): List<MedicalEvidence>

    /**
     * Returns the total count of loaded knowledge topics.
     */
    fun getTopicCount(): Int
}
