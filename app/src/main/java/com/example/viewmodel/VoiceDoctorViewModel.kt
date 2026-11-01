package com.example.viewmodel

import android.app.Application
import android.content.pm.PackageManager
import android.util.Log
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.model.AppLanguage
import com.example.model.ChatMessage
import com.example.model.DoctorUiState
import com.example.model.MessageSender
import com.example.model.VoiceDoctorState
import com.example.voice.AudioRecorder
import com.example.voice.SpeechToTextEngine
import com.example.voice.TextToSpeechManager
import com.example.voice.WhisperSpeechToTextEngine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class VoiceDoctorViewModel(
    application: Application,
    private val audioRecorder: com.example.voice.VoiceRecorder = AudioRecorder(),
    private val sttEngine: SpeechToTextEngine = WhisperSpeechToTextEngine(application),
    private val ttsManager: TextToSpeechManager = TextToSpeechManager(application)
) : AndroidViewModel(application) {

    companion object {
        private const val TAG = "VoiceDoctorViewModel"
    }

    private val _uiState = MutableStateFlow(
        VoiceDoctorState(
            messages = getSeedMessages(AppLanguage.ENGLISH)
        )
    )
    val uiState: StateFlow<VoiceDoctorState> = _uiState.asStateFlow()

    private var activeJob: Job? = null
    private var amplitudeJob: Job? = null
    private var simulatedVoiceIndex = 0

    private val englishVoicePrompts = listOf(
        "It is mild.",
        "I feel a bit feverish and tired.",
        "My throat hurts when swallowing.",
        "Should I drink more warm fluids?",
        "How much rest is recommended?"
    )

    private val teluguVoicePrompts = listOf(
        "తేలికపాటి నొప్పి ఉంది.",
        "నాకు కొద్దిగా జ్వరం మరియు నీరసంగా అనిపిస్తోంది.",
        "మింగేటప్పుడు గొంతు నొప్పిగా ఉంది.",
        "నేను వేడి నీరు లేదా పానీయాలు ఎక్కువగా త్రాగవచ్చా?",
        "ఎంత విశ్రాంతి తీసుకోవడం మంచిది?"
    )

    init {
        // Pre-initialize Whisper STT engine asynchronously
        viewModelScope.launch {
            sttEngine.initialize().onFailure { error ->
                Log.e(TAG, "Failed to initialize Whisper engine at startup", error)
            }
        }

        // Monitor TTS voice availability
        viewModelScope.launch {
            ttsManager.isOfflineVoiceAvailable.collect { available ->
                _uiState.update { it.copy(isOfflineVoiceAvailable = available) }
            }
        }
    }

    fun hasRecordAudioPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            getApplication(),
            android.Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED
    }

    fun startRecording() {
        activeJob?.cancel()
        ttsManager.stop()

        val isTelugu = _uiState.value.currentLanguage == AppLanguage.TELUGU

        if (isTelugu) {
            // Telugu Demo Mode
            _uiState.update {
                it.copy(
                    uiState = DoctorUiState.RecordingDemo,
                    statusMessage = "తెలుగు వాయిస్ రికగ్నిషన్ డెమో మోడ్‌లో ఉంది"
                )
            }
            return
        }

        // English Live Mode
        if (!hasRecordAudioPermission()) {
            _uiState.update {
                it.copy(
                    uiState = DoctorUiState.PermissionRequired,
                    statusMessage = "Microphone permission required for offline speech recognition"
                )
            }
            return
        }

        val startResult = audioRecorder.startRecording(
            scope = viewModelScope,
            onMaxDurationReached = {
                releaseRecording()
            }
        )

        if (startResult.isFailure) {
            val errorMsg = startResult.exceptionOrNull()?.message ?: "Failed to initialize microphone"
            Log.e(TAG, "Recording start failure: $errorMsg")
            _uiState.update {
                it.copy(
                    uiState = DoctorUiState.Error(errorMsg),
                    statusMessage = errorMsg
                )
            }
            return
        }

        _uiState.update {
            it.copy(
                uiState = DoctorUiState.Recording,
                statusMessage = null,
                recordingAmplitude = 0f
            )
        }

        // Feed microphone amplitude to UI
        amplitudeJob?.cancel()
        amplitudeJob = viewModelScope.launch {
            audioRecorder.amplitude.collect { amp ->
                _uiState.update { it.copy(recordingAmplitude = amp) }
            }
        }
    }

    fun cancelRecording() {
        activeJob?.cancel()
        amplitudeJob?.cancel()

        if (_uiState.value.uiState is DoctorUiState.Recording) {
            audioRecorder.cancelRecording()
        }

        _uiState.update {
            it.copy(
                uiState = DoctorUiState.Idle,
                recordingAmplitude = 0f
            )
        }
    }

    fun releaseRecording() {
        amplitudeJob?.cancel()

        val currentState = _uiState.value.uiState
        if (currentState !is DoctorUiState.Recording && currentState !is DoctorUiState.RecordingDemo) {
            return
        }

        val lang = _uiState.value.currentLanguage

        if (lang == AppLanguage.TELUGU) {
            // Telugu Demo Mode: simulated response
            val list = teluguVoicePrompts
            val userVoiceText = list[simulatedVoiceIndex % list.size].also { simulatedVoiceIndex++ }
            processDemoMessage(userVoiceText, AppLanguage.TELUGU)
            return
        }

        // English Live Pipeline: AudioRecord -> Whisper.cpp -> Recognized Text -> Android TTS
        val audioSamples = audioRecorder.stopRecording()
        _uiState.update {
            it.copy(
                uiState = DoctorUiState.Transcribing,
                recordingAmplitude = 0f
            )
        }

        // Validate minimum sample count (at least 0.3s of audio = 4800 samples)
        if (audioSamples.size < 4800) {
            Log.i(TAG, "Audio too short (${audioSamples.size} samples). Returning to idle.")
            _uiState.update {
                it.copy(
                    uiState = DoctorUiState.Idle,
                    statusMessage = "Audio too short. Please hold to talk."
                )
            }
            viewModelScope.launch {
                delay(2000)
                _uiState.update { it.copy(statusMessage = null) }
            }
            return
        }

        activeJob = viewModelScope.launch(Dispatchers.Default) {
            // Ensure model is loaded
            sttEngine.initialize()

            val result = sttEngine.transcribe(audioSamples, "en")
            result.onSuccess { transcript ->
                val cleanTranscript = transcript.trim()
                if (cleanTranscript.isEmpty()) {
                    withContext(Dispatchers.Main) {
                        _uiState.update {
                            it.copy(
                                uiState = DoctorUiState.Idle,
                                statusMessage = "No speech detected. Please speak clearly."
                            )
                        }
                    }
                    delay(2000)
                    withContext(Dispatchers.Main) {
                        _uiState.update { it.copy(statusMessage = null) }
                    }
                } else {
                    withContext(Dispatchers.Main) {
                        handleRecognizedSpeechEcho(cleanTranscript)
                    }
                }
            }.onFailure { error ->
                Log.e(TAG, "Transcription failed", error)
                withContext(Dispatchers.Main) {
                    _uiState.update {
                        it.copy(
                            uiState = DoctorUiState.Idle,
                            statusMessage = "Transcription error: ${error.localizedMessage ?: "Unknown"}"
                        )
                    }
                }
                delay(2500)
                withContext(Dispatchers.Main) {
                    _uiState.update { it.copy(statusMessage = null) }
                }
            }
        }
    }

    private fun handleRecognizedSpeechEcho(recognizedText: String) {
        val currentTime = getCurrentFormattedTime()

        // 1. Display user message with recognized speech
        val userMessage = ChatMessage(
            sender = MessageSender.USER,
            text = recognizedText,
            time = currentTime,
            isRead = true
        )

        // 2. Display doctor echo response
        val doctorEchoMessage = ChatMessage(
            sender = MessageSender.DOCTOR,
            text = recognizedText,
            time = currentTime,
            isRead = true
        )

        _uiState.update { state ->
            state.copy(
                messages = state.messages + userMessage + doctorEchoMessage,
                uiState = if (state.isSoundEnabled) DoctorUiState.Speaking else DoctorUiState.Idle
            )
        }

        // 3. Android TTS speaks recognized speech offline
        if (_uiState.value.isSoundEnabled) {
            ttsManager.speak(
                text = recognizedText,
                isSoundEnabled = true,
                onStart = {
                    _uiState.update { it.copy(uiState = DoctorUiState.Speaking) }
                },
                onDone = {
                    _uiState.update { it.copy(uiState = DoctorUiState.Idle) }
                },
                onError = {
                    _uiState.update { it.copy(uiState = DoctorUiState.Idle) }
                }
            )
        }
    }

    fun onInputTextChanged(text: String) {
        _uiState.update { it.copy(inputText = text) }
    }

    fun submitTextMessage() {
        val query = _uiState.value.inputText.trim()
        if (query.isEmpty()) return
        _uiState.update { it.copy(inputText = "") }
        processTextMessage(query)
    }

    fun toggleTextMode() {
        _uiState.update { it.copy(isTextMode = !it.isTextMode) }
    }

    fun toggleSound() {
        val newSound = !_uiState.value.isSoundEnabled
        if (!newSound) {
            ttsManager.stop()
        }
        _uiState.update {
            it.copy(
                isSoundEnabled = newSound,
                uiState = if (!newSound && it.uiState is DoctorUiState.Speaking) DoctorUiState.Idle else it.uiState,
                statusMessage = if (newSound) "Sound enabled" else "Sound muted"
            )
        }
        viewModelScope.launch {
            delay(2000)
            _uiState.update { it.copy(statusMessage = null) }
        }
    }

    fun setLanguage(language: AppLanguage) {
        if (_uiState.value.currentLanguage == language) return
        ttsManager.stop()
        _uiState.update {
            it.copy(
                currentLanguage = language,
                messages = getSeedMessages(language),
                statusMessage = if (language == AppLanguage.TELUGU) {
                    "భాష తెలుగులోకి మార్చబడింది (డెమో మోడ్)"
                } else {
                    "Language set to English (Offline Voice)"
                }
            )
        }
        viewModelScope.launch {
            delay(2000)
            _uiState.update { it.copy(statusMessage = null) }
        }
    }

    fun setConversationExpanded(expanded: Boolean) {
        _uiState.update { it.copy(isConversationExpanded = expanded) }
    }

    fun toggleQuickPrompts() {
        _uiState.update { it.copy(showQuickPrompts = !it.showQuickPrompts) }
    }

    fun setInfoDialog(show: Boolean) {
        _uiState.update { it.copy(showInfoDialog = show) }
    }

    fun clearConversation() {
        activeJob?.cancel()
        ttsManager.stop()
        _uiState.update {
            it.copy(
                messages = emptyList(),
                isConversationExpanded = false,
                uiState = DoctorUiState.Idle,
                inputText = "",
                statusMessage = if (it.currentLanguage == AppLanguage.TELUGU) "సంభాషణ క్లియర్ చేయబడింది" else "Conversation cleared"
            )
        }
        viewModelScope.launch {
            delay(2000)
            _uiState.update { it.copy(statusMessage = null) }
        }
    }

    fun loadSampleConversation() {
        activeJob?.cancel()
        ttsManager.stop()
        _uiState.update {
            it.copy(
                messages = getSeedMessages(it.currentLanguage),
                isConversationExpanded = false,
                uiState = DoctorUiState.Idle
            )
        }
    }

    fun selectQuickPrompt(prompt: String) {
        _uiState.update { it.copy(showQuickPrompts = false) }
        processTextMessage(prompt)
    }

    private fun processTextMessage(userText: String) {
        activeJob?.cancel()
        val currentTime = getCurrentFormattedTime()

        val userMessage = ChatMessage(
            sender = MessageSender.USER,
            text = userText,
            time = currentTime,
            isRead = true
        )

        _uiState.update { state ->
            state.copy(
                messages = state.messages + userMessage,
                uiState = DoctorUiState.Transcribing
            )
        }

        activeJob = viewModelScope.launch {
            delay(400)
            val lang = _uiState.value.currentLanguage
            val responseText = generateDoctorResponse(userText, lang)
            val doctorReply = ChatMessage(
                sender = MessageSender.DOCTOR,
                text = responseText,
                time = getCurrentFormattedTime(),
                isRead = true
            )

            _uiState.update { state ->
                state.copy(
                    messages = state.messages + doctorReply,
                    uiState = if (state.isSoundEnabled) DoctorUiState.Speaking else DoctorUiState.Idle
                )
            }

            if (_uiState.value.isSoundEnabled && lang == AppLanguage.ENGLISH) {
                ttsManager.speak(
                    text = responseText,
                    isSoundEnabled = true,
                    onDone = { _uiState.update { it.copy(uiState = DoctorUiState.Idle) } },
                    onError = { _uiState.update { it.copy(uiState = DoctorUiState.Idle) } }
                )
            } else {
                delay(1200)
                _uiState.update { it.copy(uiState = DoctorUiState.Idle) }
            }
        }
    }

    private fun processDemoMessage(userText: String, language: AppLanguage) {
        activeJob?.cancel()
        val currentTime = getCurrentFormattedTime()

        val userMessage = ChatMessage(
            sender = MessageSender.USER,
            text = userText,
            time = currentTime,
            isRead = true
        )

        _uiState.update { state ->
            state.copy(
                messages = state.messages + userMessage,
                uiState = DoctorUiState.ProcessingDemo
            )
        }

        activeJob = viewModelScope.launch {
            delay(750)
            val responseText = generateDoctorResponse(userText, language)
            val doctorReply = ChatMessage(
                sender = MessageSender.DOCTOR,
                text = responseText,
                time = getCurrentFormattedTime(),
                isRead = true
            )

            _uiState.update { state ->
                state.copy(
                    messages = state.messages + doctorReply,
                    uiState = DoctorUiState.RespondingDemo
                )
            }

            delay(1400)
            _uiState.update { it.copy(uiState = DoctorUiState.Idle) }
        }
    }

    private fun generateDoctorResponse(userQuery: String, language: AppLanguage): String {
        val q = userQuery.lowercase()
        return if (language == AppLanguage.TELUGU) {
            when {
                q.contains("నొప్పి") || q.contains("తేలికపాటి") || q.contains("తలనొప్పి") ->
                    "విశ్రాంతి తీసుకోండి మరియు తగినంత నీరు త్రాగండి. లక్షణాలు కొనసాగితే వైద్య సలహా తీసుకోండి."
                q.contains("జ్వరం") ->
                    "తగినంత విశ్రాంతి తీసుకోండి, ద్రవాహారం ఎక్కువగా తీసుకోండి మరియు ఉష్ణోగ్రతను గమనిస్తూ ఉండండి."
                q.contains("గొంతు") ->
                    "గోరువెచ్చని ఉప్పు నీటితో పుక్కిలించడం వల్ల ఉపశమనం లభిస్తుంది. చల్లటి పదార్థాలకు దూరంగా ఉండండి."
                q.contains("నీరు") || q.contains("పానీయాలు") ->
                    "అవును, శరీరాన్ని హైడ్రేటెడ్‌గా ఉంచుకోవడానికి రోజంతా క్రమం తప్పకుండా నీరు త్రాగడం చాలా మంచిది."
                else ->
                    "మీ సమాచారానికి ధన్యవాదాలు. మీరు తగినంత విశ్రాంతి తీసుకోవడం ముఖ్యం. పరిస్థితి ఇబ్బందిగా అనిపిస్తే వైద్యుడిని సంప్రదించండి."
            }
        } else {
            when {
                q.contains("mild") || q.contains("headache") || q.contains("pain") ->
                    "Try to rest and stay hydrated. Seek medical advice if symptoms persist."
                q.contains("fever") || q.contains("tired") ->
                    "Ensure adequate hydration and restful sleep. Monitor your body temperature and consult a physician if fever rises."
                q.contains("throat") || q.contains("cough") ->
                    "Warm saltwater gargles and herbal teas with honey can soothe irritation. Avoid cold food items."
                q.contains("water") || q.contains("fluids") || q.contains("drink") ->
                    "Yes, staying well-hydrated helps your body recover faster. Aim for 8-10 glasses of water daily."
                q.contains("thank") ->
                    "You're very welcome! Feel free to ask if you notice any other symptoms. Take good care!"
                else ->
                    "Thank you for sharing. For general wellness, maintain good hydration and rest. Please consult a doctor for a medical diagnosis."
            }
        }
    }

    private fun getCurrentFormattedTime(): String {
        return SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date())
    }

    private fun getSeedMessages(language: AppLanguage): List<ChatMessage> {
        return if (language == AppLanguage.TELUGU) {
            listOf(
                ChatMessage(
                    sender = MessageSender.USER,
                    text = "ఉదయం నుండి నాకు తలనొప్పిగా ఉంది.",
                    time = "5:16 PM"
                ),
                ChatMessage(
                    sender = MessageSender.DOCTOR,
                    text = "నేను మీకు సహాయం చేయగలను. నొప్పి రకం మరియు తీవ్రత గురించి మరింత చెప్పగలరా?",
                    time = "5:16 PM"
                )
            )
        } else {
            listOf(
                ChatMessage(
                    sender = MessageSender.USER,
                    text = "I have a headache since morning.",
                    time = "5:16 PM"
                ),
                ChatMessage(
                    sender = MessageSender.DOCTOR,
                    text = "I'm here to help. Can you tell me more about the pain, like its type and intensity?",
                    time = "5:16 PM"
                )
            )
        }
    }

    override fun onCleared() {
        super.onCleared()
        audioRecorder.release()
        sttEngine.release()
        ttsManager.shutdown()
    }
}
