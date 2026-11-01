package com.example.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.model.AppLanguage
import com.example.model.ChatMessage
import com.example.model.DoctorUiState
import com.example.model.MessageSender
import com.example.model.VoiceDoctorState
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class VoiceDoctorViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(
        VoiceDoctorState(
            messages = getSeedMessages(AppLanguage.ENGLISH)
        )
    )
    val uiState: StateFlow<VoiceDoctorState> = _uiState.asStateFlow()

    private var activeJob: Job? = null
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

    fun startRecording() {
        activeJob?.cancel()
        _uiState.update {
            it.copy(
                uiState = DoctorUiState.RecordingDemo,
                statusMessage = null
            )
        }
    }

    fun cancelRecording() {
        activeJob?.cancel()
        _uiState.update {
            it.copy(uiState = DoctorUiState.Idle)
        }
    }

    fun releaseRecording() {
        if (_uiState.value.uiState !is DoctorUiState.RecordingDemo) return

        val lang = _uiState.value.currentLanguage
        val userVoiceText = if (lang == AppLanguage.TELUGU) {
            val list = teluguVoicePrompts
            list[simulatedVoiceIndex % list.size].also { simulatedVoiceIndex++ }
        } else {
            val list = englishVoicePrompts
            list[simulatedVoiceIndex % list.size].also { simulatedVoiceIndex++ }
        }

        processUserMessage(userVoiceText)
    }

    fun onInputTextChanged(text: String) {
        _uiState.update { it.copy(inputText = text) }
    }

    fun submitTextMessage() {
        val query = _uiState.value.inputText.trim()
        if (query.isEmpty()) return
        _uiState.update { it.copy(inputText = "") }
        processUserMessage(query)
    }

    fun toggleTextMode() {
        _uiState.update { it.copy(isTextMode = !it.isTextMode) }
    }

    fun toggleSound() {
        _uiState.update {
            val newSound = !it.isSoundEnabled
            it.copy(
                isSoundEnabled = newSound,
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
        _uiState.update {
            it.copy(
                currentLanguage = language,
                messages = getSeedMessages(language),
                statusMessage = if (language == AppLanguage.TELUGU) "భాష తెలుగులోకి మార్చబడింది" else "Language changed to English"
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
        processUserMessage(prompt)
    }

    private fun processUserMessage(userText: String) {
        activeJob?.cancel()
        val currentTime = getCurrentFormattedTime()

        // Add user message immediately
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
            // Simulated medical processing delay
            delay(850)

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
                    uiState = DoctorUiState.RespondingDemo
                )
            }

            // Responding display duration
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
}
