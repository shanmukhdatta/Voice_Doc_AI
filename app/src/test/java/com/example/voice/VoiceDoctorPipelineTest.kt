package com.example.voice

import android.app.Application
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.ConversationRepository
import com.example.data.VoiceDoctorDatabase
import com.example.model.AppLanguage
import com.example.model.DoctorUiState
import com.example.model.MessageSender
import com.example.model.isListening
import com.example.modelmanager.IModelRepository
import com.example.modelmanager.ModelInstallState
import com.example.viewmodel.VoiceDoctorViewModel
import com.example.voice.llm.LocalLlmEngine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
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
    private lateinit var db: VoiceDoctorDatabase

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
        override val isRecording = MutableStateFlow(false)
        override val amplitude = MutableStateFlow(0f)
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

    // Test Stub LLM Engine
    class FakeLocalLlmEngine(var generatedTokens: List<String> = listOf("Drink ", "warm ", "water ", "and ", "rest.")) : LocalLlmEngine {
        private val _isModelLoaded = MutableStateFlow(true)
        override val isModelLoaded: StateFlow<Boolean> = _isModelLoaded.asStateFlow()
        var generateCalled = false
        var lastPrompt: String? = null

        override suspend fun loadModel(path: String): Result<Unit> {
            _isModelLoaded.value = true
            return Result.success(Unit)
        }

        override suspend fun unloadModel() {
            _isModelLoaded.value = false
        }

        override fun generate(prompt: String, maxTokens: Int): Flow<String> = flow {
            generateCalled = true
            lastPrompt = prompt
            for (token in generatedTokens) {
                emit(token)
            }
        }
    }

    // Test Stub Model Repository
    class FakeModelRepository(initialState: ModelInstallState = ModelInstallState.NotInstalled) : IModelRepository {
        private val _installState = MutableStateFlow(initialState)
        override val installState: StateFlow<ModelInstallState> = _installState.asStateFlow()
        var downloadStarted = false
        var downloadCancelled = false

        fun setState(state: ModelInstallState) {
            _installState.value = state
        }

        override fun checkExistingModel(): ModelInstallState = _installState.value

        override fun startDownload() {
            downloadStarted = true
            _installState.value = ModelInstallState.Downloading(100L, 1000L)
        }

        override fun cancelDownload() {
            downloadCancelled = true
            _installState.value = ModelInstallState.NotInstalled
        }

        override fun removeInstalledModel(): Boolean {
            _installState.value = ModelInstallState.NotInstalled
            return true
        }
    }

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        app = ApplicationProvider.getApplicationContext()
        org.robolectric.Shadows.shadowOf(app).grantPermissions(android.Manifest.permission.RECORD_AUDIO)
        db = Room.inMemoryDatabaseBuilder(app, VoiceDoctorDatabase::class.java)
            .allowMainThreadQueries()
            .build()
    }

    @After
    fun tearDown() {
        db.close()
        Dispatchers.resetMain()
    }

    @Test
    fun testLiveEnglishPipelineWithModelInstalled() = runTest(testDispatcher) {
        val fakeStt = FakeSpeechToTextEngine("I feel feverish and tired")
        val fakeRecorder = FakeAudioRecorder(16000)
        val fakeLlm = FakeLocalLlmEngine(listOf("Please ", "rest ", "and ", "stay ", "hydrated."))
        val modelRepo = FakeModelRepository(ModelInstallState.Installed("/data/mock/model.gguf", 484220000L))
        val convRepo = ConversationRepository(db.conversationDao(), app)

        val viewModel = VoiceDoctorViewModel(
            application = app,
            audioRecorder = fakeRecorder,
            sttEngine = fakeStt,
            ttsManager = TextToSpeechManager(app),
            modelRepository = modelRepo,
            llmEngine = fakeLlm,
            conversationRepository = convRepo,
            defaultDispatcher = testDispatcher
        )
        advanceUntilIdle()

        val initialMessageCount = viewModel.uiState.value.messages.size

        // 1. User records voice in English
        viewModel.setLanguage(AppLanguage.ENGLISH)
        viewModel.startRecording()

        // 2. User releases mic
        viewModel.releaseRecording()
        advanceUntilIdle()

        // 3. Verify Whisper transcription is called
        assertTrue(fakeStt.transcribeCalled)

        // 4. Verify LLM receives transcript
        assertTrue(fakeLlm.generateCalled)
        assertTrue(fakeLlm.lastPrompt?.contains("I feel feverish and tired") == true)

        // 5. Verify messages: 1 USER message + 1 DOCTOR generated message
        val messages = viewModel.uiState.value.messages
        assertEquals(initialMessageCount + 2, messages.size)

        val userMessage = messages[messages.size - 2]
        val doctorReply = messages[messages.size - 1]

        assertEquals(MessageSender.USER, userMessage.sender)
        assertEquals("I feel feverish and tired", userMessage.text)

        assertEquals(MessageSender.DOCTOR, doctorReply.sender)
        assertEquals("Please rest and stay hydrated.", doctorReply.text)
    }

    @Test
    fun testLiveEnglishPipelinePromptsDownloadWhenModelMissing() = runTest(testDispatcher) {
        val fakeStt = FakeSpeechToTextEngine("What should I do for a burn?")
        val fakeRecorder = FakeAudioRecorder(16000)
        val fakeLlm = FakeLocalLlmEngine()
        val modelRepo = FakeModelRepository(ModelInstallState.NotInstalled)

        val viewModel = VoiceDoctorViewModel(
            application = app,
            audioRecorder = fakeRecorder,
            sttEngine = fakeStt,
            ttsManager = TextToSpeechManager(app),
            modelRepository = modelRepo,
            llmEngine = fakeLlm,
            conversationRepository = ConversationRepository(db.conversationDao(), app),
            defaultDispatcher = testDispatcher
        )
        advanceUntilIdle()

        viewModel.setLanguage(AppLanguage.ENGLISH)
        viewModel.startRecording()
        viewModel.releaseRecording()
        advanceUntilIdle()

        // STT ran
        assertTrue(fakeStt.transcribeCalled)

        // LLM did NOT run because model is not installed
        assertFalse(fakeLlm.generateCalled)

        // Doctor message states model not installed
        val doctorMsg = viewModel.uiState.value.messages.last()
        assertTrue(doctorMsg.text.contains("not installed"))
        assertTrue(viewModel.uiState.value.showModelDownloadDialog)
    }

    @Test
    fun testOptInHistorySavesExchangesToDatabase() = runTest(testDispatcher) {
        val fakeLlm = FakeLocalLlmEngine(listOf("Take ", "plenty ", "of ", "fluids."))
        val modelRepo = FakeModelRepository(ModelInstallState.Installed("/data/mock/model.gguf", 484220000L))
        val convRepo = ConversationRepository(db.conversationDao(), app)
        convRepo.setHistoryEnabled(true)

        val viewModel = VoiceDoctorViewModel(
            application = app,
            sttEngine = FakeSpeechToTextEngine(),
            ttsManager = TextToSpeechManager(app),
            modelRepository = modelRepo,
            llmEngine = fakeLlm,
            conversationRepository = convRepo,
            defaultDispatcher = testDispatcher
        )
        advanceUntilIdle()

        viewModel.setLanguage(AppLanguage.ENGLISH)
        viewModel.onInputTextChanged("How to cure dehydration?")
        viewModel.submitTextMessage()
        advanceUntilIdle()

        val savedList = convRepo.getAllConversations().first()
        assertEquals(1, savedList.size)
        assertEquals("How to cure dehydration?", savedList[0].title)
    }

    @Test
    fun testCancellationDiscardsAudioAndReturnsToIdle() = runTest(testDispatcher) {
        val fakeRecorder = FakeAudioRecorder(16000)
        val fakeStt = FakeSpeechToTextEngine()

        val viewModel = VoiceDoctorViewModel(
            application = app,
            audioRecorder = fakeRecorder,
            sttEngine = fakeStt,
            ttsManager = TextToSpeechManager(app),
            defaultDispatcher = testDispatcher
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
        val shortRecorder = FakeAudioRecorder(100)
        val fakeStt = FakeSpeechToTextEngine("Fabricated Sentence")

        val viewModel = VoiceDoctorViewModel(
            application = app,
            audioRecorder = shortRecorder,
            sttEngine = fakeStt,
            ttsManager = TextToSpeechManager(app),
            defaultDispatcher = testDispatcher
        )

        val initialCount = viewModel.uiState.value.messages.size
        viewModel.setLanguage(AppLanguage.ENGLISH)

        viewModel.startRecording()
        viewModel.releaseRecording()
        advanceUntilIdle()

        assertFalse(fakeStt.transcribeCalled)
        assertEquals(initialCount, viewModel.uiState.value.messages.size)
    }

    @Test
    fun testTeluguDemoModeMaintainsSafeSeparation() = runTest(testDispatcher) {
        val fakeStt = FakeSpeechToTextEngine()
        val fakeLlm = FakeLocalLlmEngine()

        val viewModel = VoiceDoctorViewModel(
            application = app,
            sttEngine = fakeStt,
            llmEngine = fakeLlm,
            ttsManager = TextToSpeechManager(app),
            defaultDispatcher = testDispatcher
        )

        viewModel.setLanguage(AppLanguage.TELUGU)
        assertEquals(AppLanguage.TELUGU, viewModel.uiState.value.currentLanguage)

        viewModel.startRecording()
        assertEquals(DoctorUiState.RecordingDemo, viewModel.uiState.value.uiState)

        viewModel.releaseRecording()
        advanceUntilIdle()

        assertFalse(fakeStt.transcribeCalled)
        assertFalse(fakeLlm.generateCalled)
    }
}
