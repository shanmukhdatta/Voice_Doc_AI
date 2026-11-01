package com.example.viewmodel

import android.app.Application
import android.content.pm.PackageManager
import android.util.Log
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.ConversationRepository
import com.example.data.VoiceDoctorDatabase
import com.example.medical.MedicalConversationOrchestrator
import com.example.medical.input.MedicalInputProcessor
import com.example.medical.knowledge.KeywordMedicalRepository
import com.example.medical.knowledge.MedicalKnowledgeRepository
import com.example.medical.model.MedicalResponseResult
import com.example.medical.safety.MedicalSafetyEngine
import com.example.medical.safety.SafetyAction
import com.example.model.AppLanguage
import com.example.model.ChatMessage
import com.example.model.DoctorUiState
import com.example.model.MessageSender
import com.example.model.VoiceDoctorState
import com.example.modelmanager.IModelRepository
import com.example.modelmanager.ModelInstallState
import com.example.modelmanager.ModelRepository
import com.example.voice.AudioRecorder
import com.example.voice.SpeechToTextEngine
import com.example.voice.TextToSpeechManager
import com.example.voice.WhisperSpeechToTextEngine
import com.example.voice.llm.LlamaCppEngine
import com.example.voice.llm.LocalLlmEngine
import kotlinx.coroutines.CoroutineDispatcher
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
import java.util.UUID

class VoiceDoctorViewModel(
    application: Application,
    private val audioRecorder: com.example.voice.VoiceRecorder = AudioRecorder(),
    private val sttEngine: SpeechToTextEngine = WhisperSpeechToTextEngine(application),
    private val ttsManager: TextToSpeechManager = TextToSpeechManager(application),
    private val modelRepository: IModelRepository = ModelRepository(application),
    private val llmEngine: LocalLlmEngine = LlamaCppEngine(),
    private val conversationRepository: ConversationRepository = ConversationRepository(
        VoiceDoctorDatabase.getInstance(application).conversationDao(),
        application
    ),
    private val medicalKnowledgeRepository: MedicalKnowledgeRepository = KeywordMedicalRepository(
        assetManager = application.assets
    ),
    private val orchestrator: MedicalConversationOrchestrator = MedicalConversationOrchestrator(
        knowledgeRepository = medicalKnowledgeRepository,
        llmEngine = llmEngine
    ),
    private val defaultDispatcher: CoroutineDispatcher = Dispatchers.Default
) : AndroidViewModel(application) {

    companion object {
        private const val TAG = "VoiceDoctorViewModel"
    }

    private var currentConversationId: String = UUID.randomUUID().toString()

    private val _uiState = MutableStateFlow(
        VoiceDoctorState(
            messages = getSeedMessages(AppLanguage.ENGLISH)
        )
    )
    val uiState: StateFlow<VoiceDoctorState> = _uiState.asStateFlow()

    private var activeJob: Job? = null
    private var amplitudeJob: Job? = null
    private var simulatedVoiceIndex = 0

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

        // Monitor LLM model install state and auto-load when installed
        viewModelScope.launch {
            modelRepository.installState.collect { installState ->
                _uiState.update { it.copy(modelInstallState = installState) }
                if (installState is ModelInstallState.Installed) {
                    if (!llmEngine.isModelLoaded.value) {
                        Log.i(TAG, "Model installed; loading into llama.cpp engine...")
                        llmEngine.loadModel(installState.path).onFailure { error ->
                            Log.e(TAG, "Failed to load llama model", error)
                        }
                    }
                } else if (installState is ModelInstallState.NotInstalled) {
                    if (llmEngine.isModelLoaded.value) {
                        llmEngine.unloadModel()
                    }
                }
            }
        }

        // Monitor history opt-in state
        viewModelScope.launch {
            conversationRepository.isHistoryEnabled.collect { enabled ->
                _uiState.update { it.copy(isHistoryEnabled = enabled) }
            }
        }

        // Monitor saved conversations
        viewModelScope.launch {
            conversationRepository.getAllConversations().collect { convs ->
                _uiState.update { it.copy(savedConversations = convs) }
            }
        }
    }

    fun hasRecordAudioPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            getApplication(),
            android.Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED
    }

    // --- Model Management Actions ---

    fun setModelDownloadDialog(show: Boolean) {
        _uiState.update { it.copy(showModelDownloadDialog = show) }
    }

    fun startModelDownload() {
        modelRepository.startDownload()
    }

    fun cancelModelDownload() {
        modelRepository.cancelDownload()
    }

    fun deleteInstalledModel() {
        viewModelScope.launch {
            llmEngine.unloadModel()
            modelRepository.removeInstalledModel()
            _uiState.update {
                it.copy(
                    statusMessage = "AI model removed from device"
                )
            }
            delay(2000)
            _uiState.update { it.copy(statusMessage = null) }
        }
    }

    // --- History Actions ---

    fun toggleHistoryOptIn(enabled: Boolean) {
        conversationRepository.setHistoryEnabled(enabled)
    }

    fun setHistorySheet(show: Boolean) {
        _uiState.update { it.copy(showHistorySheet = show) }
    }

    fun loadSavedConversation(conversationId: String) {
        viewModelScope.launch {
            val entities = conversationRepository.getMessagesList(conversationId)
            if (entities.isNotEmpty()) {
                currentConversationId = conversationId
                val loadedMessages = entities.map { entity ->
                    ChatMessage(
                        id = entity.id,
                        sender = if (entity.role == "USER") MessageSender.USER else MessageSender.DOCTOR,
                        text = entity.content,
                        time = SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date(entity.createdAt)),
                        isRead = true,
                        isVoice = entity.source == "VOICE",
                        timestamp = entity.createdAt
                    )
                }
                _uiState.update {
                    it.copy(
                        messages = loadedMessages,
                        showHistorySheet = false,
                        isConversationExpanded = true,
                        statusMessage = "Loaded saved conversation"
                    )
                }
                delay(1500)
                _uiState.update { it.copy(statusMessage = null) }
            }
        }
    }

    fun deleteSavedConversation(conversationId: String) {
        viewModelScope.launch {
            conversationRepository.deleteConversation(conversationId)
        }
    }

    fun clearAllSavedConversations() {
        viewModelScope.launch {
            conversationRepository.clearAllHistory()
        }
    }

    // --- Recording Actions ---

    fun startRecording() {
        activeJob?.cancel()
        ttsManager.stop()

        val isTelugu = _uiState.value.currentLanguage == AppLanguage.TELUGU

        if (isTelugu) {
            _uiState.update {
                it.copy(
                    uiState = DoctorUiState.RecordingDemo,
                    statusMessage = "తెలుగు వాయిస్ రికగ్నిషన్ డెమో మోడ్‌లో ఉంది"
                )
            }
            return
        }

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
            val list = teluguVoicePrompts
            val userVoiceText = list[simulatedVoiceIndex % list.size].also { simulatedVoiceIndex++ }
            processDemoMessage(userVoiceText, AppLanguage.TELUGU)
            return
        }

        val audioSamples = audioRecorder.stopRecording()
        _uiState.update {
            it.copy(
                uiState = DoctorUiState.Transcribing,
                recordingAmplitude = 0f
            )
        }

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

        activeJob = viewModelScope.launch(defaultDispatcher) {
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
                        processLiveEnglishExchange(cleanTranscript, isVoice = true)
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

    // --- Exchange Processing ---

    private fun processLiveEnglishExchange(userPrompt: String, isVoice: Boolean) {
        val currentTime = getCurrentFormattedTime()
        val userMessage = ChatMessage(
            sender = MessageSender.USER,
            text = userPrompt,
            time = currentTime,
            isRead = true,
            isVoice = isVoice
        )

        val doctorMsgId = UUID.randomUUID().toString()
        val initialDoctorMessage = ChatMessage(
            id = doctorMsgId,
            sender = MessageSender.DOCTOR,
            text = "",
            time = currentTime,
            isRead = true,
            isVoice = isVoice
        )

        // Pre-check safety escalation first (emergency cases bypass model download requirement)
        val inputProcessor = MedicalInputProcessor()
        val safetyEngine = MedicalSafetyEngine()
        val processedInput = inputProcessor.process(userPrompt)
        val safetyDecision = safetyEngine.inspectInput(processedInput)

        if (safetyDecision.action == SafetyAction.ESCALATE) {
            val escalationText = safetyDecision.publicMessage ?: MedicalSafetyEngine.EMERGENCY_ESCALATION_TEMPLATE
            val doctorReply = initialDoctorMessage.copy(text = escalationText)
            _uiState.update { state ->
                state.copy(
                    messages = state.messages + userMessage + doctorReply,
                    uiState = DoctorUiState.Idle
                )
            }
            if (_uiState.value.isSoundEnabled) {
                ttsManager.speak(
                    text = escalationText,
                    isSoundEnabled = true,
                    onStart = { _uiState.update { it.copy(uiState = DoctorUiState.Speaking) } },
                    onDone = { _uiState.update { it.copy(uiState = DoctorUiState.Idle) } },
                    onError = { _uiState.update { it.copy(uiState = DoctorUiState.Idle) } }
                )
            }
            return
        }

        // Check model installation status
        val installState = _uiState.value.modelInstallState
        if (installState !is ModelInstallState.Installed) {
            val doctorNotice = ChatMessage(
                sender = MessageSender.DOCTOR,
                text = "The on-device AI model is not installed yet. Please tap 'Download AI Model' to download it (~484 MB) for offline guidance.",
                time = currentTime,
                isRead = true,
                isVoice = false
            )

            _uiState.update { state ->
                state.copy(
                    messages = state.messages + userMessage + doctorNotice,
                    uiState = DoctorUiState.Idle,
                    showModelDownloadDialog = true
                )
            }
            return
        }

        _uiState.update { state ->
            state.copy(
                messages = state.messages + userMessage + initialDoctorMessage,
                uiState = DoctorUiState.Thinking
            )
        }

        activeJob = viewModelScope.launch(defaultDispatcher) {
            val history = _uiState.value.messages
                .filter { it.text.isNotBlank() && it.id != doctorMsgId }
                .chunked(2)
                .mapNotNull { chunk ->
                    if (chunk.size == 2 && chunk[0].sender == MessageSender.USER && chunk[1].sender == MessageSender.DOCTOR) {
                        chunk[0].text to chunk[1].text
                    } else null
                }

            val result = orchestrator.processQuery(userPrompt, history)
            val finalText = when (result) {
                is MedicalResponseResult.Answer -> result.text
                is MedicalResponseResult.Clarification -> result.question
                is MedicalResponseResult.Escalation -> result.message
                is MedicalResponseResult.Unavailable -> result.safeMessage
            }

            // Update doctor message with final text
            val completedDoctorMsg = initialDoctorMessage.copy(text = finalText)
            _uiState.update { state ->
                val updatedMessages = state.messages.map { msg ->
                    if (msg.id == doctorMsgId) completedDoctorMsg else msg
                }
                state.copy(messages = updatedMessages)
            }

            // Save conversation to Room if history is enabled
            if (conversationRepository.isHistoryEnabled.value) {
                conversationRepository.saveConversation(
                    conversationId = currentConversationId,
                    title = userPrompt.take(50),
                    messages = listOf(userMessage, completedDoctorMsg)
                )
            }

            // Offline TTS speak if sound is enabled
            if (_uiState.value.isSoundEnabled) {
                _uiState.update { it.copy(uiState = DoctorUiState.Speaking) }
                ttsManager.speak(
                    text = finalText,
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
            } else {
                _uiState.update { it.copy(uiState = DoctorUiState.Idle) }
            }
        }
    }

    fun onInputTextChanged(text: String) {
        _uiState.update { it.copy(inputText = text) }
    }

    fun submitTextMessage() {
        val query = _uiState.value.inputText.trim()
        if (query.isEmpty()) return
        _uiState.update { it.copy(inputText = "") }

        if (_uiState.value.currentLanguage == AppLanguage.TELUGU) {
            processDemoMessage(query, AppLanguage.TELUGU)
        } else {
            processLiveEnglishExchange(query, isVoice = false)
        }
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
                    "Language set to English (Offline LLM)"
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
        currentConversationId = UUID.randomUUID().toString()
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
        currentConversationId = UUID.randomUUID().toString()
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
        if (_uiState.value.currentLanguage == AppLanguage.TELUGU) {
            processDemoMessage(prompt, AppLanguage.TELUGU)
        } else {
            processLiveEnglishExchange(prompt, isVoice = false)
        }
    }

    private fun processDemoMessage(userText: String, language: AppLanguage) {
        activeJob?.cancel()
        val currentTime = getCurrentFormattedTime()

        val userMessage = ChatMessage(
            sender = MessageSender.USER,
            text = userText,
            time = currentTime,
            isRead = true,
            isVoice = false
        )

        _uiState.update { state ->
            state.copy(
                messages = state.messages + userMessage,
                uiState = DoctorUiState.ProcessingDemo
            )
        }

        activeJob = viewModelScope.launch {
            delay(750)
            val responseText = generateDoctorDemoResponse(userText)
            val doctorReply = ChatMessage(
                sender = MessageSender.DOCTOR,
                text = responseText,
                time = getCurrentFormattedTime(),
                isRead = true,
                isVoice = false
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

    private fun generateDoctorDemoResponse(userQuery: String): String {
        val q = userQuery.lowercase()
        return when {
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
        viewModelScope.launch {
            llmEngine.unloadModel()
        }
    }
}
