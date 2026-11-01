package com.example.voice

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import com.example.model.AppLanguage
import com.example.model.DoctorUiState
import com.example.model.MessageSender
import com.example.model.isListening
import com.example.model.isSpeaking
import com.example.viewmodel.VoiceDoctorViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class VoiceDoctorPipelineTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var app: Application

    // Test Stub STT Engine
    class FakeSpeechToTextEngine(var simulatedResult: String = "Hello doctor") : SpeechToTextEngine {
        var initialized = false
        var transcribeCalled = false

        override suspend fun initialize(): Result<Unit> {
            initialized = true
            return Result.success(Unit)
        }

        override suspend fun transcribe(audio: FloatArray, language: String): Result<String> {
            transcribeCalled = true
            return Result.success(simulatedResult)
        }

        override fun release() {
            initialized = false
        }
    }

    // Test Stub AudioRecorder
    class FakeAudioRecorder(private val sampleCount: Int = 16000) : VoiceRecorder {
        override val isRecording = kotlinx.coroutines.flow.MutableStateFlow(false)
        override val amplitude = kotlinx.coroutines.flow.MutableStateFlow(0f)
        var startCalled = false
        var stopCalled = false
        var cancelCalled = false

        override fun startRecording(scope: kotlinx.coroutines.CoroutineScope, onMaxDurationReached: (() -> Unit)?): Result<Unit> {
            startCalled = true
            isRecording.value = true
            return Result.success(Unit)
        }

        override fun stopRecording(): FloatArray {
            stopCalled = true
            isRecording.value = false
            return FloatArray(sampleCount) { 0.2f }
        }

        override fun cancelRecording() {
            cancelCalled = true
            isRecording.value = false
        }

        override fun release() {
            isRecording.value = false
        }
    }

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        app = ApplicationProvider.getApplicationContext()
        org.robolectric.Shadows.shadowOf(app).grantPermissions(android.Manifest.permission.RECORD_AUDIO)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun testLiveEnglishEchoPipeline() = runTest(testDispatcher) {
        val fakeStt = FakeSpeechToTextEngine("I need medical advice")
        val fakeRecorder = FakeAudioRecorder(16000)
        val ttsManager = TextToSpeechManager(app)

        val viewModel = VoiceDoctorViewModel(
            application = app,
            audioRecorder = fakeRecorder,
            sttEngine = fakeStt,
            ttsManager = ttsManager
        )

        val initialMessageCount = viewModel.uiState.value.messages.size

        // 1. User presses and holds mic
        // (In Robolectric, record permission is granted or tested in demo mode)
        viewModel.setLanguage(AppLanguage.ENGLISH)
        viewModel.startRecording()

        // 2. User releases mic
        viewModel.releaseRecording()
        advanceUntilIdle()

        // 3. Verify Whisper transcription is called
        assertTrue(fakeStt.transcribeCalled)

        // 4. Verify messages are posted (1 USER message + 1 DOCTOR echo message)
        val messages = viewModel.uiState.value.messages
        assertEquals(initialMessageCount + 2, messages.size)

        val userMessage = messages[messages.size - 2]
        val doctorEcho = messages[messages.size - 1]

        assertEquals(MessageSender.USER, userMessage.sender)
        assertEquals("I need medical advice", userMessage.text)

        assertEquals(MessageSender.DOCTOR, doctorEcho.sender)
        assertEquals("I need medical advice", doctorEcho.text)
    }

    @Test
    fun testCancellationDiscardsAudioAndReturnsToIdle() = runTest(testDispatcher) {
        val fakeRecorder = FakeAudioRecorder(16000)
        val fakeStt = FakeSpeechToTextEngine()

        val viewModel = VoiceDoctorViewModel(
            application = app,
            audioRecorder = fakeRecorder,
            sttEngine = fakeStt,
            ttsManager = TextToSpeechManager(app)
        )

        val initialCount = viewModel.uiState.value.messages.size
        viewModel.setLanguage(AppLanguage.TELUGU)
        viewModel.startRecording()
        assertTrue(viewModel.uiState.value.uiState.isListening)

        viewModel.cancelRecording()
        advanceUntilIdle()

        assertEquals(DoctorUiState.Idle, viewModel.uiState.value.uiState)
        assertEquals(initialCount, viewModel.uiState.value.messages.size)
        assertFalse(fakeStt.transcribeCalled)
    }

    @Test
    fun testEmptyOrTooShortAudioDoesNotFabricateTranscript() = runTest(testDispatcher) {
        // Return only 100 samples (< 4800 samples minimum for 0.3s)
        val shortRecorder = FakeAudioRecorder(100)
        val fakeStt = FakeSpeechToTextEngine("Fabricated Sentence")

        val viewModel = VoiceDoctorViewModel(
            application = app,
            audioRecorder = shortRecorder,
            sttEngine = fakeStt,
            ttsManager = TextToSpeechManager(app)
        )

        val initialCount = viewModel.uiState.value.messages.size
        viewModel.setLanguage(AppLanguage.ENGLISH)

        // Manually trigger release recording with short audio
        viewModel.startRecording()
        viewModel.releaseRecording()
        advanceUntilIdle()

        // Whisper transcribe should NOT be invoked
        assertFalse(fakeStt.transcribeCalled)
        assertEquals(initialCount, viewModel.uiState.value.messages.size)
    }

    @Test
    fun testTeluguDemoModeMaintainsSafeSeparation() = runTest(testDispatcher) {
        val fakeStt = FakeSpeechToTextEngine()
        val viewModel = VoiceDoctorViewModel(
            application = app,
            sttEngine = fakeStt,
            ttsManager = TextToSpeechManager(app)
        )

        viewModel.setLanguage(AppLanguage.TELUGU)
        assertEquals(AppLanguage.TELUGU, viewModel.uiState.value.currentLanguage)

        viewModel.startRecording()
        assertTrue(viewModel.uiState.value.uiState is DoctorUiState.RecordingDemo)

        viewModel.releaseRecording()
        advanceUntilIdle()

        // Real Whisper STT should not be invoked for Telugu
        assertFalse(fakeStt.transcribeCalled)

        // Demo response should be added
        val lastMessage = viewModel.uiState.value.messages.last()
        assertEquals(MessageSender.DOCTOR, lastMessage.sender)
    }

    @Test
    fun testMuteToggleStopsSpeech() = runTest(testDispatcher) {
        val viewModel = VoiceDoctorViewModel(
            application = app,
            ttsManager = TextToSpeechManager(app)
        )

        assertTrue(viewModel.uiState.value.isSoundEnabled)
        viewModel.toggleSound()
        assertFalse(viewModel.uiState.value.isSoundEnabled)

        viewModel.toggleSound()
        assertTrue(viewModel.uiState.value.isSoundEnabled)
    }
}

