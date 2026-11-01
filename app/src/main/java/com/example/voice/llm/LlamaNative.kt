package com.example.voice.llm

import android.util.Log

class LlamaNative {
    companion object {
        private const val TAG = "LlamaNative"
        private var isLoaded = false

        init {
            try {
                System.loadLibrary("voice_llm")
                isLoaded = true
                Log.i(TAG, "libvoice_llm loaded successfully")
            } catch (e: UnsatisfiedLinkError) {
                Log.e(TAG, "Failed to load libvoice_llm", e)
            }
        }

        fun isLibraryLoaded(): Boolean = isLoaded
    }

    external fun logToAndroid()
    external fun systemInfo(): String
    external fun backendInit()
    external fun backendFree()
    external fun loadModel(path: String): Long
    external fun freeModel(modelPtr: Long)
    external fun newContext(modelPtr: Long, nCtx: Int, nThreads: Int): Long
    external fun freeContext(contextPtr: Long)
    external fun newBatch(nTokens: Int, embd: Int, nSeqMax: Int): Long
    external fun freeBatch(batchPtr: Long)
    external fun newSampler(temp: Float, topP: Float): Long
    external fun freeSampler(samplerPtr: Long)
    external fun completionInit(contextPtr: Long, batchPtr: Long, text: String, parseSpecial: Boolean): Int
    external fun completionLoop(contextPtr: Long, batchPtr: Long, samplerPtr: Long, nCur: Int, maxTokens: Int): String?
    external fun kvCacheClear(contextPtr: Long)
}

