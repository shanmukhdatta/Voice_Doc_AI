package com.example.conversation

import org.junit.Assert.assertTrue
import org.junit.Test

class ContextualQueryBuilderTest {

    private val builder = ContextualQueryBuilder()

    @Test
    fun testEllipticalFollowUpExpandsWithPriorActiveSymptom() {
        val state = SessionState(
            facts = listOf(
                UserFact(category = FactCategory.SYMPTOM, value = "stomach pain", sourceSpan = "stomach pain", turnIndex = 1)
            ),
            turnCount = 1
        )

        val query = builder.buildQuery(state, "upper right")
        assertTrue(query.contains("stomach pain"))
        assertTrue(query.contains("upper right"))
    }

    @Test
    fun testStandaloneQueryPreserved() {
        val state = SessionState()
        val query = builder.buildQuery(state, "fever and persistent dry cough")
        assertTrue(query.contains("fever and persistent dry cough"))
    }
}
