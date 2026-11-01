package com.example.data

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.model.ChatMessage
import com.example.model.MessageSender
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.UUID

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ConversationRepositoryTest {

    private lateinit var db: VoiceDoctorDatabase
    private lateinit var repository: ConversationRepository
    private lateinit var context: Context

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        db = Room.inMemoryDatabaseBuilder(context, VoiceDoctorDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repository = ConversationRepository(db.conversationDao(), context)
        // Ensure starting state is default false
        repository.setHistoryEnabled(false)
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun testDefaultHistoryDisabledDoesNotSave() = runTest {
        assertFalse(repository.isHistoryEnabled.value)

        val convId = UUID.randomUUID().toString()
        val messages = listOf(
            ChatMessage(sender = MessageSender.USER, text = "I have a headache", time = "10:00 AM"),
            ChatMessage(sender = MessageSender.DOCTOR, text = "Drink water and rest", time = "10:00 AM")
        )

        repository.saveConversation(convId, "Headache consultation", messages)

        val convs = repository.getAllConversations().first()
        assertTrue("No conversations should be saved when history is disabled", convs.isEmpty())
    }

    @Test
    fun testHistoryOptInSavesAndLoadsConversations() = runTest {
        repository.setHistoryEnabled(true)
        assertTrue(repository.isHistoryEnabled.value)

        val convId = UUID.randomUUID().toString()
        val messages = listOf(
            ChatMessage(sender = MessageSender.USER, text = "I have a sore throat", time = "10:00 AM"),
            ChatMessage(sender = MessageSender.DOCTOR, text = "Gargle warm salt water", time = "10:01 AM")
        )

        repository.saveConversation(convId, "Sore throat", messages)

        val convs = repository.getAllConversations().first()
        assertEquals(1, convs.size)
        assertEquals("Sore throat", convs[0].title)

        val loadedMessages = repository.getMessagesList(convId)
        assertEquals(2, loadedMessages.size)
        assertEquals("USER", loadedMessages[0].role)
        assertEquals("I have a sore throat", loadedMessages[0].content)
        assertEquals("DOCTOR", loadedMessages[1].role)
        assertEquals("Gargle warm salt water", loadedMessages[1].content)
    }

    @Test
    fun testMultiTurnConversationsPreservedWithoutCascadeLoss() = runTest {
        repository.setHistoryEnabled(true)
        val convId = UUID.randomUUID().toString()

        // Turn 1
        val turn1 = listOf(
            ChatMessage(id = "msg1", sender = MessageSender.USER, text = "I have a cough", time = "10:00 AM", timestamp = 1000L),
            ChatMessage(id = "msg2", sender = MessageSender.DOCTOR, text = "How long has it lasted?", time = "10:01 AM", timestamp = 1001L)
        )
        repository.saveConversation(convId, "Cough evaluation", turn1)

        // Turn 2
        val turn2 = listOf(
            ChatMessage(id = "msg3", sender = MessageSender.USER, text = "Since yesterday", time = "10:02 AM", timestamp = 1002L),
            ChatMessage(id = "msg4", sender = MessageSender.DOCTOR, text = "Stay hydrated and rest", time = "10:03 AM", timestamp = 1003L)
        )
        repository.saveConversation(convId, "Cough evaluation", turn2)

        // Turn 3
        val turn3 = listOf(
            ChatMessage(id = "msg5", sender = MessageSender.USER, text = "Can I take honey?", time = "10:04 AM", timestamp = 1004L),
            ChatMessage(id = "msg6", sender = MessageSender.DOCTOR, text = "Honey can soothe your throat", time = "10:05 AM", timestamp = 1005L)
        )
        repository.saveConversation(convId, "Cough evaluation", turn3)

        val convs = repository.getAllConversations().first()
        assertEquals(1, convs.size)

        val allMessages = repository.getMessagesList(convId)
        assertEquals("All 6 messages across 3 turns must be preserved", 6, allMessages.size)
        assertEquals("msg1", allMessages[0].id)
        assertEquals("msg2", allMessages[1].id)
        assertEquals("msg3", allMessages[2].id)
        assertEquals("msg4", allMessages[3].id)
        assertEquals("msg5", allMessages[4].id)
        assertEquals("msg6", allMessages[5].id)
    }

    @Test
    fun testDeleteAndClearHistory() = runTest {
        repository.setHistoryEnabled(true)

        val id1 = UUID.randomUUID().toString()
        val id2 = UUID.randomUUID().toString()

        repository.saveConversation(id1, "Chat 1", listOf(ChatMessage(sender = MessageSender.USER, text = "Hi", time = "1:00 PM")))
        repository.saveConversation(id2, "Chat 2", listOf(ChatMessage(sender = MessageSender.USER, text = "Hey", time = "1:05 PM")))

        assertEquals(2, repository.getAllConversations().first().size)

        repository.deleteConversation(id1)
        val remaining = repository.getAllConversations().first()
        assertEquals(1, remaining.size)
        assertEquals(id2, remaining[0].id)

        repository.clearAllHistory()
        assertTrue(repository.getAllConversations().first().isEmpty())
    }
}

