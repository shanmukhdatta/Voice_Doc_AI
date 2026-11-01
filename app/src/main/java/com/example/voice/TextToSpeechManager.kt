package com.example.voice

import android.content.Context
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.speech.tts.Voice
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale
import java.util.UUID

/**
 * TextToSpeechManager manages on-device English TTS playback, prioritizing offline voices.
 */
class TextToSpeechManager(
    private val context: Context
) : TextToSpeech.OnInitListener {

    companion object {
        private const val TAG = "TextToSpeechManager"
        private val PREFERRED_LOCALE = Locale("en", "IN")
        private val FALLBACK_LOCALES = listOf(
            Locale.US,
            Locale.UK,
            Locale.CANADA,
            Locale.ENGLISH
        )
    }

    private var tts: TextToSpeech? = null

    private val _isInitialized = MutableStateFlow(false)
    val isInitialized: StateFlow<Boolean> = _isInitialized.asStateFlow()

    private val _isSpeaking = MutableStateFlow(false)
    val isSpeaking: StateFlow<Boolean> = _isSpeaking.asStateFlow()

    private val _isOfflineVoiceAvailable = MutableStateFlow(false)
    val isOfflineVoiceAvailable: StateFlow<Boolean> = _isOfflineVoiceAvailable.asStateFlow()

    private var activeUtteranceId: String? = null
    private var onStartCallback: (() -> Unit)? = null
    private var onDoneCallback: (() -> Unit)? = null
    private var onErrorCallback: ((String) -> Unit)? = null

    init {
        try {
            tts = TextToSpeech(context.applicationContext, this)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to instantiate TextToSpeech", e)
        }
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            val engine = tts ?: return
            Log.i(TAG, "TextToSpeech engine initialized successfully.")

            // Configure utterance listener
            engine.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                override fun onStart(utteranceId: String?) {
                    if (utteranceId == activeUtteranceId) {
                        _isSpeaking.value = true
                        onStartCallback?.invoke()
                    }
                }

                override fun onDone(utteranceId: String?) {
                    if (utteranceId == activeUtteranceId) {
                        _isSpeaking.value = false
                        activeUtteranceId = null
                        onDoneCallback?.invoke()
                    }
                }

                @Deprecated("Deprecated in Java")
                override fun onError(utteranceId: String?) {
                    onError(utteranceId, TextToSpeech.ERROR)
                }

                override fun onError(utteranceId: String?, errorCode: Int) {
                    if (utteranceId == activeUtteranceId) {
                        _isSpeaking.value = false
                        activeUtteranceId = null
                        val errorDesc = "TTS error code: $errorCode"
                        Log.e(TAG, errorDesc)
                        onErrorCallback?.invoke(errorDesc)
                    }
                }
            })

            configureOfflineEnglishVoice(engine)
            _isInitialized.value = true
        } else {
            Log.e(TAG, "TextToSpeech initialization failed with status: $status")
            _isInitialized.value = false
        }
    }

    private fun configureOfflineEnglishVoice(engine: TextToSpeech) {
        try {
            val availableVoices = engine.voices
            var selectedVoice: Voice? = null

            if (availableVoices != null) {
                // 1. First priority: Offline voice for en-IN
                selectedVoice = availableVoices.firstOrNull { voice ->
                    val isEnglishIndia = voice.locale.language == "en" && voice.locale.country.equals("IN", ignoreCase = true)
                    val isOffline = !voice.isNetworkConnectionRequired
                    isEnglishIndia && isOffline
                }

                // 2. Second priority: Any offline English voice
                if (selectedVoice == null) {
                    selectedVoice = availableVoices.firstOrNull { voice ->
                        voice.locale.language == "en" && !voice.isNetworkConnectionRequired
                    }
                }

                // 3. Fallback: Any en-IN voice
                if (selectedVoice == null) {
                    selectedVoice = availableVoices.firstOrNull { voice ->
                        voice.locale.language == "en" && voice.locale.country.equals("IN", ignoreCase = true)
                    }
                }
            }

            if (selectedVoice != null) {
                engine.voice = selectedVoice
                val isOffline = !selectedVoice.isNetworkConnectionRequired
                _isOfflineVoiceAvailable.value = isOffline
                Log.i(TAG, "Selected voice: ${selectedVoice.name}, Locale: ${selectedVoice.locale}, Offline: $isOffline")
            } else {
                // Try setting language by Locale
                var langResult = engine.setLanguage(PREFERRED_LOCALE)
                if (langResult == TextToSpeech.LANG_MISSING_DATA || langResult == TextToSpeech.LANG_NOT_SUPPORTED) {
                    for (fallback in FALLBACK_LOCALES) {
                        langResult = engine.setLanguage(fallback)
                        if (langResult != TextToSpeech.LANG_MISSING_DATA && langResult != TextToSpeech.LANG_NOT_SUPPORTED) {
                            Log.i(TAG, "Fallback to locale: $fallback")
                            break
                        }
                    }
                }
                _isOfflineVoiceAvailable.value = (langResult != TextToSpeech.LANG_MISSING_DATA && langResult != TextToSpeech.LANG_NOT_SUPPORTED)
            }
        } catch (e: Exception) {
            Log.w(TAG, "Error selecting voice, applying default locale", e)
            engine.language = Locale.ENGLISH
        }
    }

    /**
     * Speaks the given text if sound is enabled and TTS is ready.
     */
    fun speak(
        text: String,
        isSoundEnabled: Boolean = true,
        onStart: (() -> Unit)? = null,
        onDone: (() -> Unit)? = null,
        onError: ((String) -> Unit)? = null
    ): Boolean {
        if (!isSoundEnabled) {
            Log.d(TAG, "Sound is muted. Skipping TTS playback.")
            onDone?.invoke()
            return false
        }

        val engine = tts
        if (engine == null || !_isInitialized.value) {
            Log.w(TAG, "TTS not ready for playback.")
            onError?.invoke("TTS not ready")
            return false
        }

        if (!_isOfflineVoiceAvailable.value) {
            Log.w(TAG, "No verified offline English voice available. Refusing online voice fallback.")
            onError?.invoke("Offline English voice not installed on device")
            return false
        }

        if (text.isBlank()) {
            onDone?.invoke()
            return false
        }

        // Interrupt any current speech
        stop()

        val utteranceId = UUID.randomUUID().toString()
        activeUtteranceId = utteranceId
        onStartCallback = onStart
        onDoneCallback = onDone
        onErrorCallback = onError

        val params = Bundle().apply {
            putFloat(TextToSpeech.Engine.KEY_PARAM_VOLUME, 1.0f)
        }

        val queueMode = TextToSpeech.QUEUE_FLUSH
        val result = engine.speak(text, queueMode, params, utteranceId)
        return result == TextToSpeech.SUCCESS
    }

    /**
     * Stops current speech immediately.
     */
    fun stop() {
        try {
            if (_isSpeaking.value) {
                tts?.stop()
                _isSpeaking.value = false
                activeUtteranceId = null
                onDoneCallback?.invoke()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping TTS", e)
        }
    }

    /**
     * Releases TTS resources.
     */
    fun shutdown() {
        try {
            stop()
            tts?.shutdown()
            tts = null
            _isInitialized.value = false
            _isSpeaking.value = false
        } catch (e: Exception) {
            Log.e(TAG, "Error shutting down TTS", e)
        }
    }
}

