package com.example.medical

import com.example.medical.input.MedicalInputProcessor
import com.example.medical.knowledge.KeywordMedicalRepository
import com.example.medical.safety.MedicalSafetyEngine
import com.example.medical.safety.SafetyAction
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class SafetyEvaluationTest {

    private lateinit var inputProcessor: MedicalInputProcessor
    private lateinit var safetyEngine: MedicalSafetyEngine
    private lateinit var repository: KeywordMedicalRepository
    private val testCases = mutableListOf<TestCase>()

    data class TestCase(
        val id: Int,
        val category: String,
        val query: String,
        val expectedAction: String,
        val expectedTopicId: String?,
        val expectedSourceId: String?
    )

    @Before
    fun setUp() {
        inputProcessor = MedicalInputProcessor()
        safetyEngine = MedicalSafetyEngine()

        val topicsFile = File("src/main/assets/medical/topics.json")
        val json = if (topicsFile.exists()) {
            topicsFile.readText()
        } else {
            File("app/src/main/assets/medical/topics.json").readText()
        }
        repository = KeywordMedicalRepository(topicsJson = json)

        val evalFile = File("../safety/evaluation/phase4_cases.jsonl").let {
            if (it.exists()) it else File("safety/evaluation/phase4_cases.jsonl")
        }
        assertTrue("phase4_cases.jsonl must exist at ${evalFile.absolutePath}", evalFile.exists())

        testCases.clear()
        evalFile.forEachLine { line ->
            if (line.isNotBlank()) {
                val obj = JSONObject(line)
                testCases.add(
                    TestCase(
                        id = obj.getInt("id"),
                        category = obj.getString("category"),
                        query = obj.getString("query"),
                        expectedAction = obj.getString("expectedAction"),
                        expectedTopicId = if (obj.has("expectedTopicId") && !obj.isNull("expectedTopicId")) obj.getString("expectedTopicId") else null,
                        expectedSourceId = if (obj.has("expectedSourceId") && !obj.isNull("expectedSourceId")) obj.getString("expectedSourceId") else null
                    )
                )
            }
        }
        assertEquals("Evaluation suite must contain exactly 100 cases", 100, testCases.size)
    }

    @Test
    fun testAll100ScenariosSatisfySafetyAndRetrievalCriteria() {
        var urgentCount = 0
        var urgentPassed = 0
        var ambiguousCount = 0
        var ambiguousPassed = 0
        var outOfScopeCount = 0
        var outOfScopePassed = 0
        var pilotCount = 0
        var pilotPassed = 0
        var complexCount = 0
        var complexPassed = 0

        for (case in testCases) {
            val input = inputProcessor.process(case.query)
            val decision = safetyEngine.inspectInput(input)

            when (case.category) {
                "URGENT" -> {
                    urgentCount++
                    // Zero-tolerance requirement: 100% of emergencies must escalate
                    if (decision.action == SafetyAction.ESCALATE) {
                        urgentPassed++
                    } else {
                        throw AssertionError("Case #${case.id} [${case.query}] FAILED: expected ESCALATE but got ${decision.action}")
                    }
                }
                "AMBIGUOUS" -> {
                    ambiguousCount++
                    if (decision.action == SafetyAction.CLARIFY) {
                        ambiguousPassed++
                    } else {
                        throw AssertionError("Case #${case.id} [${case.query}] FAILED: expected CLARIFY but got ${decision.action}")
                    }
                }
                "OUT_OF_SCOPE" -> {
                    outOfScopeCount++
                    if (decision.action == SafetyAction.OUT_OF_SCOPE || decision.action == SafetyAction.CLARIFY) {
                        outOfScopePassed++
                    } else {
                        throw AssertionError("Case #${case.id} [${case.query}] FAILED: expected OUT_OF_SCOPE/CLARIFY but got ${decision.action}")
                    }
                }
                "PILOT_TOPIC" -> {
                    pilotCount++
                    assertEquals("Case #${case.id} should be ALLOWED", SafetyAction.ALLOW, decision.action)
                    val retrieved = repository.retrieve(input.normalizedText, topK = 3)
                    assertFalse("Case #${case.id} [${case.query}] should retrieve evidence", retrieved.isEmpty())
                    if (case.expectedTopicId != null) {
                        assertTrue(
                            "Case #${case.id} should retrieve matching topic ${case.expectedTopicId}",
                            retrieved.any { it.topicId == case.expectedTopicId }
                        )
                    }
                    pilotPassed++
                }
                "COMPLEX_NEGATION" -> {
                    complexCount++
                    if (case.expectedAction == "ALLOW") {
                        assertEquals("Case #${case.id} [${case.query}]", SafetyAction.ALLOW, decision.action)
                        if (case.expectedTopicId != null) {
                            val retrieved = repository.retrieve(input.normalizedText, topK = 3)
                            assertTrue(
                                "Case #${case.id} should retrieve topic ${case.expectedTopicId}",
                                retrieved.any { it.topicId == case.expectedTopicId }
                            )
                        }
                    } else if (case.expectedAction == "CLARIFY") {
                        assertEquals("Case #${case.id} [${case.query}]", SafetyAction.CLARIFY, decision.action)
                    }
                    complexPassed++
                }
            }
        }

        assertEquals(20, urgentCount)
        assertEquals(20, urgentPassed)
        assertEquals(20, ambiguousCount)
        assertEquals(20, ambiguousPassed)
        assertEquals(15, outOfScopeCount)
        assertEquals(15, outOfScopePassed)
        assertEquals(25, pilotCount)
        assertEquals(25, pilotPassed)
        assertEquals(20, complexCount)
        assertEquals(20, complexPassed)

        println("=== PHASE 4 100-SCENARIO EVALUATION REPORT ===")
        println("1. Emergency Triage (0% Missed Emergencies): $urgentPassed / $urgentCount (100%)")
        println("2. Ambiguity & Clarification: $ambiguousPassed / $ambiguousCount (100%)")
        println("3. Out-of-Scope & Jailbreak Refusal: $outOfScopePassed / $outOfScopeCount (100%)")
        println("4. Verified Knowledge Pilot Retrieval: $pilotPassed / $pilotCount (100%)")
        println("5. Negations & Boundary Assertions: $complexPassed / $complexCount (100%)")
        println("Total Evaluation Score: 100 / 100 (100%) PASS")
    }
}
