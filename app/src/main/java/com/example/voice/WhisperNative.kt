package com.example.voice

import android.content.res.AssetManager
import android.util.Log

object WhisperNative {
    private const val TAG = "WhisperNative"
    private var isLoaded = false

    init {
        try {
            System.loadLibrary("voice_whisper")
            isLoaded = true
            Log.i(TAG, "libvoice_whisper.so loaded successfully.")
        } catch (e: UnsatisfiedLinkError) {
            Log.e(TAG, "Failed to load libvoice_whisper.so", e)
            isLoaded = false
        } catch (e: Exception) {
            Log.e(TAG, "Unexpected error loading libvoice_whisper.so", e)
            isLoaded = false
        }
    }

    fun isNativeLoaded(): Boolean = isLoaded

    external fun init(modelPath: String): Long

    external fun initFromAsset(assetManager: AssetManager, assetPath: String): Long

    external fun transcribe(
        contextHandle: Long,
        samples: FloatArray,
        language: String = "en",
        numThreads: Int = 4
    ): String

    external fun release(contextHandle: Long)

    external fun getSystemInfo(): String
}

