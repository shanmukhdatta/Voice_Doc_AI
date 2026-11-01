package com.example.medical

import com.example.medical.knowledge.KeywordMedicalRepository
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
class KeywordMedicalRepositoryTest {

    private lateinit var repository: KeywordMedicalRepository

    @Before
    fun setup() {
        // Read actual topics.json file directly from assets directory in project
        val topicsFile = File("src/main/assets/medical/topics.json")
        val json = if (topicsFile.exists()) {
            topicsFile.readText()
        } else {
            // Fallback for relative path difference if run from different root
            File("app/src/main/assets/medical/topics.json").readText()
        }
        repository = KeywordMedicalRepository(topicsJson = json)
    }

    @Test
    fun testRepositoryLoadsAll25Topics() {
        assertEquals(25, repository.getTopicCount())
    }

    @Test
    fun testQuerySoreThroatRetrievesSoreThroatEvidence() {
        val results = repository.retrieve("I have a sore throat and it hurts when swallowing", topK = 2)
        assertFalse("Results should not be empty for sore throat query", results.isEmpty())
        val top = results.first()
        assertEquals("MED_THROAT_003", top.sourceId.value)
        assertEquals("sore_throat", top.topicId)
        assertTrue(top.excerpt.contains("gargling", ignoreCase = true) || top.excerpt.contains("throat", ignoreCase = true))
    }

    @Test
    fun testQueryHeadacheRetrievesHeadacheTopic() {
        val results = repository.retrieve("tension headache pain in forehead", topK = 1)
        assertEquals(1, results.size)
        assertEquals("MED_HEADACHE_004", results[0].sourceId.value)
    }

    @Test
    fun testQueryMildBurnRetrievesMinorBurnEvidence() {
        val results = repository.retrieve("I touched a hot pan and got a minor burn", topK = 1)
        assertEquals(1, results.size)
        assertEquals("MED_BURN_014", results[0].sourceId.value)
    }

    @Test
    fun testIrrelevantQueryReturnsEmptyList() {
        val results = repository.retrieve("quantum physics string theory particle accelerator", topK = 3)
        assertTrue("Irrelevant query should return empty list due to score threshold", results.isEmpty())
    }

    @Test
    fun testBlankQueryReturnsEmptyList() {
        val results = repository.retrieve("   ", topK = 3)
        assertTrue(results.isEmpty())
    }

    @Test
    fun testTopKRespected() {
        val results = repository.retrieve("cold fever cough and congestion", topK = 2)
        assertTrue("Should return at most 2 results", results.size in 1..2)
    }
}
