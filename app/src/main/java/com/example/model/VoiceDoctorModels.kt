package com.example.model

import com.example.data.ConversationEntity
import com.example.modelmanager.ModelInstallState
import java.util.UUID

enum class AppLanguage(val code: String, val label: String) {
    ENGLISH("EN", "English"),
    TELUGU("TE", "తెలుగు")
}

enum class MessageSender {
    USER,
    DOCTOR
}

data class ChatMessage(
    val id: String = UUID.randomUUID().toString(),
    val sender: MessageSender,
    val text: String,
    val time: String,
    val isRead: Boolean = true,
    val isVoice: Boolean = true,
    val timestamp: Long = System.currentTimeMillis()
)

sealed interface DoctorUiState {
    data object Idle : DoctorUiState
    data object Recording : DoctorUiState
    data object Transcribing : DoctorUiState
    data object Thinking : DoctorUiState
    data object Speaking : DoctorUiState
    data class Error(val message: String) : DoctorUiState
    data object PermissionRequired : DoctorUiState

    // Retained for Telugu demo mode
    data object RecordingDemo : DoctorUiState
    data object ProcessingDemo : DoctorUiState
    data object RespondingDemo : DoctorUiState
}

val DoctorUiState.isListening: Boolean
    get() = this is DoctorUiState.Recording || this is DoctorUiState.RecordingDemo

val DoctorUiState.isProcessing: Boolean
    get() = this is DoctorUiState.Transcribing || this is DoctorUiState.Thinking || this is DoctorUiState.ProcessingDemo

val DoctorUiState.isSpeaking: Boolean
    get() = this is DoctorUiState.Speaking || this is DoctorUiState.RespondingDemo

data class VoiceDoctorState(
    val messages: List<ChatMessage> = emptyList(),
    val currentLanguage: AppLanguage = AppLanguage.ENGLISH,
    val isSoundEnabled: Boolean = true,
    val isTextMode: Boolean = false,
    val isConversationExpanded: Boolean = false,
    val uiState: DoctorUiState = DoctorUiState.Idle,
    val inputText: String = "",
    val showQuickPrompts: Boolean = false,
    val showInfoDialog: Boolean = false,
    val statusMessage: String? = null,
    val recordingAmplitude: Float = 0f,
    val isOfflineVoiceAvailable: Boolean = true,
    val modelInstallState: ModelInstallState = ModelInstallState.NotInstalled,
    val showModelDownloadDialog: Boolean = false,
    val isHistoryEnabled: Boolean = false,
    val showHistorySheet: Boolean = false,
    val savedConversations: List<ConversationEntity> = emptyList()
)
