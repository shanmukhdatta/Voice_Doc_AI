package com.example.data

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.example.model.ChatMessage
import com.example.model.MessageSender
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.UUID

class ConversationRepository(
    private val conversationDao: ConversationDao,
    context: Context
) {
    companion object {
        private const val TAG = "ConversationRepo"
        private const val PREFS_NAME = "voice_doctor_history_prefs"
        private const val KEY_HISTORY_ENABLED = "opt_in_history_enabled"
    }

    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val _isHistoryEnabled = MutableStateFlow(prefs.getBoolean(KEY_HISTORY_ENABLED, false))
    val isHistoryEnabled: StateFlow<Boolean> = _isHistoryEnabled.asStateFlow()

    fun setHistoryEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_HISTORY_ENABLED, enabled).apply()
        _isHistoryEnabled.value = enabled
        Log.i(TAG, "Conversation history opt-in toggled to: $enabled")
    }

    suspend fun saveConversation(conversationId: String, title: String, messages: List<ChatMessage>) {
        if (!_isHistoryEnabled.value) {
            return
        }
        try {
            val now = System.currentTimeMillis()
            val conversation = ConversationEntity(
                id = conversationId,
                title = title.take(60),
                createdAt = now,
                updatedAt = now
            )
            conversationDao.insertConversation(conversation)

            val entities = messages.map { msg ->
                ChatMessageEntity(
                    id = msg.id.ifEmpty { UUID.randomUUID().toString() },
                    conversationId = conversationId,
                    role = if (msg.sender == MessageSender.USER) "USER" else "DOCTOR",
                    content = msg.text,
                    source = if (msg.isVoice) "VOICE" else "TEXT",
                    createdAt = msg.timestamp
                )
            }
            conversationDao.insertMessages(entities)
            Log.i(TAG, "Saved conversation $conversationId with ${entities.size} messages")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to save conversation to Room", e)
        }
    }

    fun getAllConversations(): Flow<List<ConversationEntity>> {
        return conversationDao.getAllConversations()
    }

    fun getMessagesForConversation(conversationId: String): Flow<List<ChatMessageEntity>> {
        return conversationDao.getMessagesForConversation(conversationId)
    }

    suspend fun getMessagesList(conversationId: String): List<ChatMessageEntity> {
        return conversationDao.getMessagesList(conversationId)
    }

    suspend fun deleteConversation(conversationId: String) {
        conversationDao.deleteConversation(conversationId)
    }

    suspend fun clearAllHistory() {
        conversationDao.deleteAllConversations()
    }
}

