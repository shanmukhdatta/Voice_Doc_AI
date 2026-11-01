package com.example.medical

import com.example.medical.model.HealthRequestType
import com.example.medical.model.MedicalEvidence
import com.example.medical.model.MedicalInput
import com.example.medical.model.SourceId
import com.example.medical.prompt.MedicalPromptBuilder
import com.example.medical.safety.MedicalResponseValidator
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
}
