package com.example.voice.llm

/**
 * Filters out `<think>...</think>` tags and reasoning content emitted by reasoning models
 * like Qwen3 across streaming token chunks.
 */
class ThinkingTagFilter {

    private val openTag = "<think>"
    private val closeTag = "</think>"

    private var inThinking = false
    private var suppressLeadingWhitespace = false
    private val buffer = StringBuilder()

    fun processChunk(chunk: String): String {
        buffer.append(chunk)
        val output = StringBuilder()

        while (buffer.isNotEmpty()) {
            if (inThinking) {
                val closeIdx = buffer.indexOf(closeTag)
                if (closeIdx != -1) {
                    // Found close tag; discard thinking text up to and including </think>
                    buffer.delete(0, closeIdx + closeTag.length)
                    inThinking = false
                    suppressLeadingWhitespace = true
                } else {
                    // Check if the end of buffer could be a partial prefix of </think>
                    val maxPrefixLen = findLongestPrefixSuffix(buffer.toString(), closeTag)
                    if (maxPrefixLen > 0) {
                        // Keep only the potential prefix in buffer, discard the rest
                        val discardLen = buffer.length - maxPrefixLen
                        buffer.delete(0, discardLen)
                    } else {
                        buffer.clear()
                    }
                    break
                }
            } else if (suppressLeadingWhitespace) {
                // Consume any immediate whitespace/newlines after </think>
                var i = 0
                while (i < buffer.length && (buffer[i] == '\n' || buffer[i] == '\r' || buffer[i] == ' ' || buffer[i] == '\t')) {
                    i++
                }
                buffer.delete(0, i)
                if (buffer.isNotEmpty()) {
                    suppressLeadingWhitespace = false
                } else {
                    break
                }
            } else {
                val openIdx = buffer.indexOf(openTag)
                if (openIdx != -1) {
                    // Emit content before <think>
                    output.append(buffer.substring(0, openIdx))
                    buffer.delete(0, openIdx + openTag.length)
                    inThinking = true
                } else {
                    // Check if buffer ends with a prefix of <think>
                    val maxPrefixLen = findLongestPrefixSuffix(buffer.toString(), openTag)
                    if (maxPrefixLen > 0) {
                        val safeLen = buffer.length - maxPrefixLen
                        output.append(buffer.substring(0, safeLen))
                        buffer.delete(0, safeLen)
                        break
                    } else {
                        output.append(buffer.toString())
                        buffer.clear()
                    }
                }
            }
        }

        return output.toString()
    }

    fun finish(): String {
        val remaining = StringBuilder()
        if (!inThinking && buffer.isNotEmpty()) {
            var text = buffer.toString()
            if (suppressLeadingWhitespace) {
                text = text.trimStart()
            }
            remaining.append(text)
        }
        buffer.clear()
        inThinking = false
        suppressLeadingWhitespace = false
        return remaining.toString()
    }

    private fun findLongestPrefixSuffix(text: String, tag: String): Int {
        val maxLen = minOf(text.length, tag.length - 1)
        for (len in maxLen downTo 1) {
            if (text.endsWith(tag.substring(0, len))) {
                return len
            }
        }
        return 0
    }
}

