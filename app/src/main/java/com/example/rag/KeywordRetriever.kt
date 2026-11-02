package com.example.rag

class KeywordRetriever(private val knowledgeStore: MedicalKnowledgeStore) {
    fun search(query: String, topK: Int = 5): List<EvidencePassage> {
        return knowledgeStore.searchFts(query, topK)
    }
}
