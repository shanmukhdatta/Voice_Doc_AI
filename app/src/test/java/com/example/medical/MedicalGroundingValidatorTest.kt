package com.example.medical

import com.example.conversation.FactCategory
import com.example.conversation.SessionState
import com.example.rag.EvidenceBundle
import com.example.rag.EvidencePassage
import com.example.rag.EvidenceStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MedicalGroundingValidatorTest {

    private val validator = MedicalGroundingValidator()

    @Test
    fun testAskActionSingleQuestionValidated() {
        val action = PlannerAction(
            type = PlannerActionType.ASK,
            content = "Where exactly do you feel the pain?",
            questionCategory = FactCategory.LOCATION
        )
        val state = SessionState()
        val bundle = EvidenceBundle.EMPTY

        val result = validator.validate(action, state, bundle)
        assertEquals(PlannerActionType.ASK, result.type)
        assertEquals("Where exactly do you feel the pain?", result.text)
        assertEquals(FactCategory.LOCATION, result.pendingCategory)
    }

    @Test
    fun testAskActionCeilingConvertsToAnswer() {
        val passage = EvidencePassage(
            id = "MP_001",
            topicId = "1",
            title = "Stomach Pain",
            section = "Summary",
            text = "Most stomach pain is not serious. Rest and fluids may help.",
            sourceUrl = "https://example.com"
        )
        val action = PlannerAction(
            type = PlannerActionType.ASK,
            content = "Is the pain sharp?",
            questionCategory = FactCategory.SEVERITY
        )
        // Already asked 3 questions on this topic
        val state = SessionState(
            questionCountForCurrentTopic = 3
        )
        val bundle = EvidenceBundle(
            passages = listOf(passage),
            status = EvidenceStatus.RELEVANT,
            retrievalQuery = "stomach pain",
            topScore = 0.8f
        )

        val result = validator.validate(action, state, bundle)
        assertEquals(PlannerActionType.ANSWER, result.type)
        assertTrue(result.wasRepaired)
    }

    @Test
    fun testAnswerWithHallucinatedCitationStripped() {
        val realPassage = EvidencePassage(
            id = "MP_1234_01",
            topicId = "1234",
            title = "Bronchitis",
            section = "Summary",
            text = "Bronchitis is an inflammation of the bronchial tubes.",
            sourceUrl = "https://example.com"
        )
        val bundle = EvidenceBundle(
            passages = listOf(realPassage),
            status = EvidenceStatus.RELEVANT,
            retrievalQuery = "bronchitis",
            topScore = 0.85f
        )
        val action = PlannerAction(
            type = PlannerActionType.ANSWER,
            content = "Bronchitis causes a cough [MP_1234_01]. Also see [MP_FAKE_99].",
            citationIds = listOf("MP_1234_01", "MP_FAKE_99")
        )
        val state = SessionState()

        val result = validator.validate(action, state, bundle)
        assertEquals(PlannerActionType.ANSWER, result.type)
        assertFalse(result.text.contains("MP_FAKE_99"))
        assertTrue(result.verifiedCitationIds.contains("MP_1234_01"))
        assertFalse(result.verifiedCitationIds.contains("MP_FAKE_99"))
    }

    @Test
    fun testDosageSelfAdministrationIsSanitized() {
        val passage = EvidencePassage(
            id = "MP_9999_01",
            topicId = "9999",
            title = "Pain Management",
            section = "Summary",
            text = "Over-the-counter pain relievers can reduce discomfort.",
            sourceUrl = "https://example.com"
        )
        val bundle = EvidenceBundle(
            passages = listOf(passage),
            status = EvidenceStatus.RELEVANT,
            retrievalQuery = "pain relief",
            topScore = 0.75f
        )
        val action = PlannerAction(
            type = PlannerActionType.ANSWER,
            content = "Take 500mg every 4 hours for relief. [MP_9999_01]",
            citationIds = listOf("MP_9999_01")
        )
        val state = SessionState()

        val result = validator.validate(action, state, bundle)
        assertFalse("Specific dosage instructions must be removed", result.text.contains("500mg"))
        assertTrue(result.text.contains("pharmacist") || result.text.contains("physician") || result.text.contains("dosage"))
    }

    @Test
    fun testNoEvidenceFallsBackToKnowledgeGap() {
        val action = PlannerAction(
            type = PlannerActionType.ANSWER,
            content = "You likely have a rare genetic syndrome."
        )
        val state = SessionState(evidenceModeEnabled = true)
        val bundle = EvidenceBundle(
            passages = emptyList(),
            status = EvidenceStatus.NONE
        )

        val result = validator.validate(action, state, bundle)
        assertEquals(PlannerActionType.KNOWLEDGE_GAP, result.type)
        assertTrue(result.text.contains("verified offline medical guidelines"))
    }
}
