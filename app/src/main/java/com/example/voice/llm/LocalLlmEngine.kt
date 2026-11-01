package com.example.voice.llm

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

interface LocalLlmEngine {
    val isModelLoaded: StateFlow<Boolean>
    suspend fun loadModel(path: String): Result<Unit>
    suspend fun unloadModel()
    fun generate(prompt: String, maxTokens: Int = 256): Flow<String>
}

