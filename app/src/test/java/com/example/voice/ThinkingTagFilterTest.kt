package com.example.voice

import com.example.voice.llm.ThinkingTagFilter
import org.junit.Assert.assertEquals
import org.junit.Test

class ThinkingTagFilterTest {

    @Test
    fun testNormalTextWithoutThinkingTags() {
        val filter = ThinkingTagFilter()
        val out1 = filter.processChunk("Hello, ")
        val out2 = filter.processChunk("how can I help you today?")
        val outFinal = filter.finish()

        assertEquals("Hello, ", out1)
        assertEquals("how can I help you today?", out2)
        assertEquals("", outFinal)
    }

    @Test
    fun testFiltersThinkingTagsSingleChunk() {
        val filter = ThinkingTagFilter()
        val text = "<think>The user has a headache. I should recommend hydration.</think>You should drink plenty of water."
        val out = filter.processChunk(text) + filter.finish()
        assertEquals("You should drink plenty of water.", out)
    }

    @Test
    fun testFiltersThinkingTagsAcrossChunkBoundaries() {
        val filter = ThinkingTagFilter()
        val chunks = listOf(
            "Here is advice: ",
            "<th",
            "ink>",
            "Let me think about fever causes. ",
            "It could be viral.",
            "</th",
            "ink>",
            "\n\nRest and monitor your temperature."
        )

        val sb = StringBuilder()
        for (chunk in chunks) {
            sb.append(filter.processChunk(chunk))
        }
        sb.append(filter.finish())

        assertEquals("Here is advice: Rest and monitor your temperature.", sb.toString().trim())
    }

    @Test
    fun testSuppressesLeadingNewlinesAfterClosingTag() {
        val filter = ThinkingTagFilter()
        val out = filter.processChunk("<think>Internal monologue</think>\n\n\nTake deep breaths.") + filter.finish()
        assertEquals("Take deep breaths.", out)
    }

    @Test
    fun testPartialPrefixAtEndFlushesCleanlyIfNeverCompleted() {
        val filter = ThinkingTagFilter()
        val out = filter.processChunk("Normal text with <") + filter.finish()
        assertEquals("Normal text with <", out)
    }
}

