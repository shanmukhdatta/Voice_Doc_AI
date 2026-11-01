package com.example.model

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
    val isRead: Boolean = true
)

sealed interface DoctorUiState {
    data object Idle : DoctorUiState
    data object RecordingDemo : DoctorUiState
    data object ProcessingDemo : DoctorUiState
    data object RespondingDemo : DoctorUiState
}

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
    val statusMessage: String? = null
)
