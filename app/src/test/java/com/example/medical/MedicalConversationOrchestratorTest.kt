package com.example.medical

import com.example.medical.knowledge.KeywordMedicalRepository
import com.example.medical.model.MedicalResponseResult
import com.example.voice.llm.LocalLlmEngine
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class MedicalConversationOrchestratorTest {

    private lateinit var repository: KeywordMedicalRepository
    private lateinit var orchestrator: MedicalConversationOrchestrator

    @Before
    fun setup() {
        val topicsFile = File("src/main/assets/medical/topics.json")
        val json = if (topicsFile.exists()) {
            topicsFile.readText()
        } else {
            File("app/src/main/assets/medical/topics.json").readText()
        }
        repository = KeywordMedicalRepository(topicsJson = json)
    }

    @Test
    fun testEmergencyQueryEscalatesImmediatelyWithoutLlm() = runTest {
        orchestrator = MedicalConversationOrchestrator(
            knowledgeRepository = repository,
            llmEngine = null
        )

        val result = orchestrator.processQuery("I have severe crushing chest pain radiating to my left arm")
        assertTrue("Expected Escalation result for chest pain", result is MedicalResponseResult.Escalation)
        val escalation = result as MedicalResponseResult.Escalation
        assertEquals("RULE_RED_FLAG_CHEST_PAIN", escalation.reviewedTemplateId)
        assertTrue(escalation.message.contains("urgent", ignoreCase = true) || escalation.message.contains("emergency", ignoreCase = true))
    }

    @Test
    fun testUnclearQueryRequestsClarification() = runTest {
        orchestrator = MedicalConversationOrchestrator(
            knowledgeRepository = repository,
            llmEngine = null
        )

        val result = orchestrator.processQuery("help")
        assertTrue("Expected Clarification for single word input", result is MedicalResponseResult.Clarification)
    }

    @Test
    fun testSafeQueryWhenModelUnavailableReturnsGroundedFallback() = runTest {
        orchestrator = MedicalConversationOrchestrator(
            knowledgeRepository = repository,
            llmEngine = null
        )

        val result = orchestrator.processQuery("I have a sore throat when swallowing")
        assertTrue("Expected Answer from grounded knowledge when model unavailable", result is MedicalResponseResult.Answer)
        val answer = result as MedicalResponseResult.Answer
        assertTrue(answer.sources.any { it.sourceId.value == "MED_THROAT_003" })
        assertTrue(answer.text.contains("Sore Throat", ignoreCase = true))
    }

    @Test
    fun testSafeQueryWithMockLlmGeneratesSanitizedAnswer() = runTest {
        val mockLlm = object : LocalLlmEngine {
            override val isModelLoaded: StateFlow<Boolean> = MutableStateFlow(true)
            override suspend fun loadModel(path: String): Result<Unit> = Result.success(Unit)
            override suspend fun unloadModel() {}
            override fun generate(prompt: String, maxTokens: Int): Flow<String> {
                return flowOf(
                    "<think>Analyzing symptoms...</think>",
                    "For a common cold, getting plenty of rest and drinking warm fluids can help ease symptoms [MED_COLD_001]. ",
                    "Seek medical care if symptoms worsen after ten days."
                )
            }
        }

        orchestrator = MedicalConversationOrchestrator(
            knowledgeRepository = repository,
            llmEngine = mockLlm
        )

        val result = orchestrator.processQuery("I have a runny nose and mild cold")
        assertTrue("Expected Answer from LLM", result is MedicalResponseResult.Answer)
        val answer = result as MedicalResponseResult.Answer
        assertTrue("Think tags must be stripped", !answer.text.contains("<think>"))
        assertTrue("Source MED_COLD_001 should be cited", answer.sources.any { it.sourceId.value == "MED_COLD_001" })
        assertTrue(answer.text.contains("rest and drinking warm fluids"))
    }
}
