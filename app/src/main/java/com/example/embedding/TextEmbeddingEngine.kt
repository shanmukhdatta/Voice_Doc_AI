package com.example.embedding

interface TextEmbeddingEngine {
    val dimension: Int
    suspend fun embed(text: String): FloatArray
}
