package com.example.embedding

import java.io.BufferedReader
import java.io.InputStream
import java.io.InputStreamReader

data class TokenizedInput(
    val inputIds: LongArray,
    val attentionMask: LongArray,
    val tokenTypeIds: LongArray
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is TokenizedInput) return false
        return inputIds.contentEquals(other.inputIds) &&
                attentionMask.contentEquals(other.attentionMask) &&
                tokenTypeIds.contentEquals(other.tokenTypeIds)
    }

    override fun hashCode(): Int {
        var result = inputIds.contentHashCode()
        result = 31 * result + attentionMask.contentHashCode()
        result = 31 * result + tokenTypeIds.contentHashCode()
        return result
    }
}

class WordPieceTokenizer(
    private val vocab: Map<String, Long>,
    private val maxLength: Int = 256
) {
    companion object {
        const val CLS_TOKEN = "[CLS]"
        const val SEP_TOKEN = "[SEP]"
        const val UNK_TOKEN = "[UNK]"
        const val PAD_TOKEN = "[PAD]"

        const val CLS_ID = 101L
        const val SEP_ID = 102L
        const val UNK_ID = 100L
        const val PAD_ID = 0L

        fun fromInputStream(inputStream: InputStream, maxLength: Int = 256): WordPieceTokenizer {
            val map = HashMap<String, Long>(32000)
            BufferedReader(InputStreamReader(inputStream, Charsets.UTF_8)).use { reader ->
                var index = 0L
                var line: String? = reader.readLine()
                while (line != null) {
                    val token = line.trim()
                    if (token.isNotEmpty()) {
                        map[token] = index
                    }
                    index++
                    line = reader.readLine()
                }
            }
            return WordPieceTokenizer(map, maxLength)
        }
    }

    fun tokenize(text: String): TokenizedInput {
        val cleaned = cleanAndNormalize(text)
        val basicTokens = basicTokenize(cleaned)
        val wordPieceTokens = mutableListOf<String>()

        for (token in basicTokens) {
            val pieces = wordPieceTokenize(token)
            wordPieceTokens.addAll(pieces)
        }

        // Limit to maxLength - 2 to allow for [CLS] and [SEP]
        val maxTokens = (maxLength - 2).coerceAtLeast(0)
        val truncatedTokens = if (wordPieceTokens.size > maxTokens) {
            wordPieceTokens.subList(0, maxTokens)
        } else {
            wordPieceTokens
        }

        val tokenIds = LongArray(truncatedTokens.size + 2)
        val attentionMask = LongArray(truncatedTokens.size + 2)
        val tokenTypeIds = LongArray(truncatedTokens.size + 2)

        tokenIds[0] = vocab[CLS_TOKEN] ?: CLS_ID
        attentionMask[0] = 1L
        tokenTypeIds[0] = 0L

        for (i in truncatedTokens.indices) {
            val piece = truncatedTokens[i]
            val id = vocab[piece] ?: (vocab[UNK_TOKEN] ?: UNK_ID)
            tokenIds[i + 1] = id
            attentionMask[i + 1] = 1L
            tokenTypeIds[i + 1] = 0L
        }

        val sepIndex = truncatedTokens.size + 1
        tokenIds[sepIndex] = vocab[SEP_TOKEN] ?: SEP_ID
        attentionMask[sepIndex] = 1L
        tokenTypeIds[sepIndex] = 0L

        return TokenizedInput(tokenIds, attentionMask, tokenTypeIds)
    }

    private fun cleanAndNormalize(text: String): String {
        val sb = StringBuilder(text.length)
        for (ch in text) {
            if (ch == '\u0000' || ch == '\uFFFD' || isControl(ch)) {
                continue
            }
            if (isWhitespace(ch)) {
                sb.append(' ')
            } else {
                sb.append(ch.lowercaseChar())
            }
        }
        return sb.toString()
    }

    private fun basicTokenize(text: String): List<String> {
        val tokens = mutableListOf<String>()
        val currentToken = StringBuilder()

        for (ch in text) {
            if (isPunctuation(ch)) {
                if (currentToken.isNotEmpty()) {
                    tokens.add(currentToken.toString())
                    currentToken.clear()
                }
                tokens.add(ch.toString())
            } else if (ch.isWhitespace()) {
                if (currentToken.isNotEmpty()) {
                    tokens.add(currentToken.toString())
                    currentToken.clear()
                }
            } else {
                currentToken.append(ch)
            }
        }

        if (currentToken.isNotEmpty()) {
            tokens.add(currentToken.toString())
        }

        return tokens
    }

    private fun wordPieceTokenize(word: String): List<String> {
        if (word.length > 100) {
            return listOf(UNK_TOKEN)
        }

        var start = 0
        var isBad = false
        val subTokens = mutableListOf<String>()

        while (start < word.length) {
            var end = word.length
            var curSubStr: String? = null
            while (start < end) {
                var substr = word.substring(start, end)
                if (start > 0) {
                    substr = "##$substr"
                }
                if (vocab.containsKey(substr)) {
                    curSubStr = substr
                    break
                }
                end--
            }

            if (curSubStr == null) {
                isBad = true
                break
            }
            subTokens.add(curSubStr)
            start = end
        }

        return if (isBad) {
            listOf(UNK_TOKEN)
        } else {
            subTokens
        }
    }

    private fun isWhitespace(ch: Char): Boolean {
        return ch == ' ' || ch == '\t' || ch == '\n' || ch == '\r' || Character.isWhitespace(ch)
    }

    private fun isControl(ch: Char): Boolean {
        if (ch == '\t' || ch == '\n' || ch == '\r') {
            return false
        }
        val type = Character.getType(ch)
        return type == Character.CONTROL.toInt() || type == Character.FORMAT.toInt()
    }

    private fun isPunctuation(ch: Char): Boolean {
        val cp = ch.code
        // ASCII punctuation characters
        if ((cp in 33..47) || (cp in 58..64) || (cp in 91..96) || (cp in 123..126)) {
            return true
        }
        val type = Character.getType(ch)
        return type == Character.CONNECTOR_PUNCTUATION.toInt() ||
                type == Character.DASH_PUNCTUATION.toInt() ||
                type == Character.START_PUNCTUATION.toInt() ||
                type == Character.END_PUNCTUATION.toInt() ||
                type == Character.INITIAL_QUOTE_PUNCTUATION.toInt() ||
                type == Character.FINAL_QUOTE_PUNCTUATION.toInt() ||
                type == Character.OTHER_PUNCTUATION.toInt()
    }
}
