package com.example.medical.model

@JvmInline
value class SourceId(val value: String)

data class MedicalEvidence(
    val sourceId: SourceId,
    val topicId: String,
    val title: String,
    val excerpt: String,
    val publisher: String,
    val sourceUrl: String,
    val publicationOrReviewDate: String?,
    val license: String,
    val reviewStatus: String,
    val knowledgeVersion: String
)
