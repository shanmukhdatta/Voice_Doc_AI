package com.example.conversation

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SessionStateManagerTest {

    private val manager = SessionStateManager()

    @Test
    fun testInitialSymptomExtraction() {
        val state = SessionState()
        val next = manager.applyUtterance(state, "I have had a fever and cough since yesterday")

        assertEquals(1, next.turnCount)
        val symptoms = next.activeAffirmativeSymptoms
        assertTrue(symptoms.any { it.value.equals("fever", ignoreCase = true) })
        assertTrue(symptoms.any { it.value.equals("cough", ignoreCase = true) })
    }

    @Test
    fun testPendingQuestionResolutionWithEllipticalAnswer() {
        // Doctor asks: Where is the pain? (targetCategory: LOCATION)
        val initial = SessionState(
            facts = listOf(
                UserFact(category = FactCategory.SYMPTOM, value = "stomach pain", sourceSpan = "stomach pain", turnIndex = 1)
            ),
            pendingQuestion = PendingQuestion(
                text = "Where do you feel the pain?",
                targetCategory = FactCategory.LOCATION,
                turnAsked = 1
            ),
            turnCount = 1
        )

        // User answers elliptically: "upper right"
        val next = manager.applyUtterance(initial, "upper right")

        assertEquals(2, next.turnCount)
        assertNull("Pending question should be resolved", next.pendingQuestion)

        val locFact = next.facts.find { it.category == FactCategory.LOCATION }
        assertNotNull("Should have extracted LOCATION fact", locFact)
        assertEquals("upper right", locFact?.value)
        assertEquals(FactPolarity.AFFIRMATIVE, locFact?.polarity)
    }

    @Test
    fun testNegationExtraction() {
        val state = SessionState()
        val next = manager.applyUtterance(state, "I have a headache but no fever")

        val headache = next.facts.find { it.value.equals("headache", ignoreCase = true) }
        val fever = next.facts.find { it.value.equals("fever", ignoreCase = true) }

        assertNotNull(headache)
        assertEquals(FactPolarity.AFFIRMATIVE, headache?.polarity)

        assertNotNull(fever)
        assertEquals(FactPolarity.NEGATED, fever?.polarity)
    }

    @Test
    fun testSubjectDetection() {
        val state = SessionState()
        val next = manager.applyUtterance(state, "My daughter has a cough and fever")

        assertTrue(next.facts.isNotEmpty())
        for (f in next.facts) {
            assertEquals(UserSubject.CHILD, f.subject)
        }
    }

    @Test
    fun testExplicitCorrection() {
        val initial = SessionState(
            facts = listOf(
                UserFact(id = "fact_loc_1", category = FactCategory.LOCATION, value = "left", sourceSpan = "left", turnIndex = 1)
            ),
            turnCount = 1
        )

        val next = manager.applyUtterance(initial, "Actually right, not left")

        val currentLoc = next.facts.find { it.category == FactCategory.LOCATION }
        assertNotNull(currentLoc)
        assertEquals("right", currentLoc?.value)
        assertEquals("fact_loc_1", currentLoc?.correctionOf)
    }
}
