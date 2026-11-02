package com.example.embedding

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class WordPieceTokenizerTest {

    @Test
    fun testWordPieceBasicAndPunctuation() {
        val vocab = mapOf(
            "[PAD]" to 0L,
            "[UNK]" to 100L,
            "[CLS]" to 101L,
            "[SEP]" to 102L,
            "i" to 1045L,
            "have" to 2031L,
            "stomach" to 4308L,
            "pain" to 3255L,
            "discomfort" to 17964L,
            "upper" to 3374L,
            "right" to 2157L,
            "un" to 4895L,
            "##well" to 4705L
        )

        val tokenizer = WordPieceTokenizer(vocab, maxLength = 32)
        val result = tokenizer.tokenize("I have stomach pain.")

        // Should start with [CLS] (101) and end with [SEP] (102)
        assertEquals(101L, result.inputIds.first())
        assertEquals(102L, result.inputIds.last())

        // Check tokens
        assertTrue(result.inputIds.contains(1045L)) // i
        assertTrue(result.inputIds.contains(2031L)) // have
        assertTrue(result.inputIds.contains(4308L)) // stomach
        assertTrue(result.inputIds.contains(3255L)) // pain

        // Attention mask should be all 1s
        for (mask in result.attentionMask) {
            assertEquals(1L, mask)
        }
    }

    @Test
    fun testSubwordContinuation() {
        val vocab = mapOf(
            "[PAD]" to 0L,
            "[UNK]" to 100L,
            "[CLS]" to 101L,
            "[SEP]" to 102L,
            "un" to 4895L,
            "##well" to 4705L
        )

        val tokenizer = WordPieceTokenizer(vocab)
        val result = tokenizer.tokenize("unwell")

        assertEquals(4, result.inputIds.size) // [CLS], un, ##well, [SEP]
        assertEquals(101L, result.inputIds[0])
        assertEquals(4895L, result.inputIds[1])
        assertEquals(4705L, result.inputIds[2])
        assertEquals(102L, result.inputIds[3])
    }

    @Test
    fun testUnknownTokenFallback() {
        val vocab = mapOf(
            "[PAD]" to 0L,
            "[UNK]" to 100L,
            "[CLS]" to 101L,
            "[SEP]" to 102L
        )

        val tokenizer = WordPieceTokenizer(vocab)
        val result = tokenizer.tokenize("xyznonexistentword")

        assertTrue(result.inputIds.contains(100L))
    }
}
