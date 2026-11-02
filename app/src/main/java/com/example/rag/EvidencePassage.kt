package com.example.rag

data class EvidencePassage(
    val id: String,
    val topicId: String,
    val title: String,
    val section: String,
    val text: String,
    val sourceUrl: String,
    val sourceAttribution: String = "MedlinePlus, National Library of Medicine (NIH)",
    val score: Float = 0f
)
