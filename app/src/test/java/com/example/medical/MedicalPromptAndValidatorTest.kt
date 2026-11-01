package com.example.medical

import com.example.medical.model.HealthRequestType
import com.example.medical.model.MedicalEvidence
import com.example.medical.model.MedicalInput
import com.example.medical.model.SourceId
import com.example.medical.prompt.MedicalPromptBuilder
import com.example.medical.safety.MedicalResponseValidator
import com.example.medical.safety.MedicalSafetyEngine
import com.example.medical.safety.SafetyAction
import com.example.voice.llm.LlamaCppEngine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MedicalPromptAndValidatorTest {

    private val sampleEvidence = listOf(
        MedicalEvidence(
            sourceId = SourceId("MED_COLD_001"),
            topicId = "common_cold",
            title = "Common Cold Overview",
            excerpt = "Rest and hydration help recover from the common cold in 7 to 10 days.",
            publisher = "MedlinePlus",
            sourceUrl = "https://medlineplus.gov/commoncold.html",
            publicationOrReviewDate = "2024-05-10",
            license = "Public Domain",
            reviewStatus = "pending_qualified_review",
            knowledgeVersion = "1.0"
        )
    )

    @Test
    fun testPromptBuilderContainsSystemPromptAndEvidence() {
        val builder = MedicalPromptBuilder()
        val input = MedicalInput(
            originalText = "I have a runny nose for 3 days",
            normalizedText = "i have a runny nose for 3 days",
            requestType = HealthRequestType.SYMPTOM_INFORMATION,
            explicitlyReportedSymptoms = listOf("runny nose"),
            explicitlyReportedDuration = "3 days",
            uncertaintyNotes = emptyList()
        )

        val prompt = builder.buildPrompt(input, sampleEvidence)
        assertTrue(prompt.startsWith("<|im_start|>system\n"))
        assertTrue(prompt.contains("Voice Doctor AI"))
        assertTrue(prompt.contains("MED_COLD_001"))
        assertTrue(prompt.contains("runny nose"))
        assertTrue(prompt.contains("Duration: 3 days"))
        assertTrue(prompt.endsWith("<|im_start|>assistant\n"))
    }

    @Test
    fun testValidatorStripsThinkTags() {
        val validator = MedicalResponseValidator()
        val candidate = "<think>Let me analyze the symptoms. The patient has a cold.</think>Rest and drink fluids [MED_COLD_001]."
        val result = validator.validateAndSanitize(candidate, sampleEvidence)
        assertTrue(result.isValid)
        assertFalse(result.sanitizedText.contains("<think>"))
        assertEquals("Rest and drink fluids [MED_COLD_001].", result.sanitizedText)
        assertEquals(1, result.citedSources.size)
        assertEquals("MED_COLD_001", result.citedSources[0].value)
    }

    @Test
    fun testValidatorRejectsPrescriptiveDosage() {
        val validator = MedicalResponseValidator()
        val candidate = "You should take 500mg tablets of amoxicillin twice daily."
        val result = validator.validateAndSanitize(candidate, sampleEvidence)
        assertFalse(result.isValid)
        assertTrue(result.rejectionReason?.contains("dosage", ignoreCase = true) == true)
        assertTrue(result.sanitizedText.contains("consult a licensed physician", ignoreCase = true))
    }

    @Test
    fun testValidatorRejectsDefinitiveDiagnosis() {
        val validator = MedicalResponseValidator()
        val candidate = "You have acute viral pneumonia and must start treatment immediately."
        val result = validator.validateAndSanitize(candidate, sampleEvidence)
        assertFalse(result.isValid)
        assertTrue(result.rejectionReason?.contains("diagnosis", ignoreCase = true) == true)
    }

    @Test
    fun testValidatorRemovesHallucinatedSources() {
        val validator = MedicalResponseValidator()
        val candidate = "Rest and fluids help [MED_COLD_001]. Also see [MED_FAKE_999]."
        val result = validator.validateAndSanitize(candidate, sampleEvidence)
        assertTrue(result.isValid)
        assertFalse(result.sanitizedText.contains("MED_FAKE_999"))
        assertEquals(1, result.citedSources.size)
        assertEquals("MED_COLD_001", result.citedSources[0].value)
    }

    @Test
    fun testValidatorDoesNotAssignCitationsWithoutExplicitSupport() {
        val validator = MedicalResponseValidator()
        val candidate = "Rest, drink plenty of water, and stay in bed."
        val result = validator.validateAndSanitize(candidate, sampleEvidence)
        assertTrue(result.isValid)
        // Citations MUST NOT be automatically assigned without explicit demonstrated support
        assertTrue("Citations must be empty if not explicitly cited/supported", result.citedSources.isEmpty())
    }

    @Test
    fun testValidatorConservativeHandlingOfMissingEvidence() {
        val validator = MedicalResponseValidator()
        val candidate = "You should rest and drink warm broth [MED_FAKE_999]."
        val result = validator.validateAndSanitize(candidate, emptyList())
        assertTrue(result.isValid)
        assertFalse(result.sanitizedText.contains("MED_FAKE_999"))
        assertTrue(result.citedSources.isEmpty())
        assertTrue("Conservative disclaimer must be present when evidence is missing",
            result.sanitizedText.contains("consult a healthcare professional", ignoreCase = true) ||
            result.sanitizedText.contains("consult a doctor", ignoreCase = true))
    }

    @Test
    fun testDoubleChatTemplateRegressionFixed() {
        val builder = MedicalPromptBuilder()
        val input = MedicalInput(
            originalText = "What can I do for a mild cold?",
            normalizedText = "what can i do for a mild cold",
            requestType = HealthRequestType.SYMPTOM_INFORMATION,
            explicitlyReportedSymptoms = listOf("cold"),
            explicitlyReportedDuration = null,
            uncertaintyNotes = emptyList()
        )
        val promptFromBuilder = builder.buildPrompt(input, sampleEvidence)

        // Passing this preformatted prompt to LlamaCppEngine.formatPrompt MUST NOT double-wrap!
        val formattedEnginePrompt = LlamaCppEngine.formatPrompt(promptFromBuilder)

        val systemTagCount = Regex("<\\|im_start\\|>system").findAll(formattedEnginePrompt).count()
        val userTagCount = Regex("<\\|im_start\\|>user").findAll(formattedEnginePrompt).count()
        val assistantTagCount = Regex("<\\|im_start\\|>assistant").findAll(formattedEnginePrompt).count()

        assertEquals("System prompt tag must appear exactly once", 1, systemTagCount)
        assertEquals("User prompt tag must appear exactly once", 1, userTagCount)
        assertEquals("Assistant prompt tag must appear exactly once", 1, assistantTagCount)
    }

    @Test
    fun testFormatPromptWrapsRawPromptProperly() {
        val raw = "Hello, what should I eat when sick?"
        val formatted = LlamaCppEngine.formatPrompt(raw)
        assertTrue(formatted.startsWith("<|im_start|>system\n"))
        assertTrue(formatted.contains(LlamaCppEngine.SYSTEM_PROMPT))
        assertTrue(formatted.contains("<|im_start|>user\nHello, what should I eat when sick?<|im_end|>"))
        assertTrue(formatted.endsWith("<|im_start|>assistant\n"))
    }

    @Test
    fun testSafetyEngineHandlesHypotheticalAndQuoted() {
        val engine = MedicalSafetyEngine()
        val inputQuoted = MedicalInput(
            originalText = "What does the term 'stroke' refer to in medical textbooks?",
            normalizedText = "what does the term 'stroke' refer to in medical textbooks",
            requestType = HealthRequestType.GENERAL_HEALTH_INFORMATION,
            explicitlyReportedSymptoms = emptyList(),
            explicitlyReportedDuration = null,
            uncertaintyNotes = emptyList()
        )
        val decisionQuoted = engine.inspectInput(inputQuoted)
        assertEquals(SafetyAction.ALLOW, decisionQuoted.action)

        val inputHypothetical = MedicalInput(
            originalText = "Hypothetically, what causes chest pain in biology?",
            normalizedText = "hypothetically, what causes chest pain in biology",
            requestType = HealthRequestType.GENERAL_HEALTH_INFORMATION,
            explicitlyReportedSymptoms = emptyList(),
            explicitlyReportedDuration = null,
            uncertaintyNotes = emptyList()
        )
        val decisionHypothetical = engine.inspectInput(inputHypothetical)
        assertEquals(SafetyAction.ALLOW, decisionHypothetical.action)
    }

    @Test
    fun testSafetyEngineHandlesNegationBothPrefixAndSuffix() {
        val engine = MedicalSafetyEngine()
        val inputPrefixNegated = MedicalInput(
            originalText = "I have a mild runny nose, no chest pain or breathing issues.",
            normalizedText = "i have a mild runny nose, no chest pain or breathing issues",
            requestType = HealthRequestType.SYMPTOM_INFORMATION,
            explicitlyReportedSymptoms = listOf("runny nose"),
            explicitlyReportedDuration = null,
            uncertaintyNotes = emptyList()
        )
        val decisionPrefix = engine.inspectInput(inputPrefixNegated)
        assertEquals(SafetyAction.ALLOW, decisionPrefix.action)

        val inputSuffixNegated = MedicalInput(
            originalText = "Headache for 2 days. Chest pain: none.",
            normalizedText = "headache for 2 days. chest pain: none.",
            requestType = HealthRequestType.SYMPTOM_INFORMATION,
            explicitlyReportedSymptoms = listOf("headache"),
            explicitlyReportedDuration = "2 days",
            uncertaintyNotes = emptyList()
        )
        val decisionSuffix = engine.inspectInput(inputSuffixNegated)
        assertEquals(SafetyAction.ALLOW, decisionSuffix.action)
    }

    @Test
    fun testSafetyEngineEscalatesOnAffirmativeEmergencies() {
        val engine = MedicalSafetyEngine()
        val inputUrgent = MedicalInput(
            originalText = "I have crushing chest pain and feel like I am having a heart attack.",
            normalizedText = "i have crushing chest pain and feel like i am having a heart attack",
            requestType = HealthRequestType.POSSIBLE_URGENT_CONCERN,
            explicitlyReportedSymptoms = listOf("chest pain"),
            explicitlyReportedDuration = null,
            uncertaintyNotes = emptyList()
        )
        val decision = engine.inspectInput(inputUrgent)
        assertEquals(SafetyAction.ESCALATE, decision.action)
        assertEquals(MedicalSafetyEngine.RULE_RED_FLAG_CHEST_PAIN, decision.ruleId)
        assertTrue(decision.publicMessage?.contains("112") == true)
        assertTrue(decision.publicMessage?.contains("911") == true)
    }
}
