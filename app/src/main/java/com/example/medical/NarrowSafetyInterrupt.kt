package com.example.medical

import java.util.Locale

data class SafetyInterruptResult(
    val isEmergency: Boolean,
    val message: String? = null
)

class NarrowSafetyInterrupt {

    companion object {
        const val EMERGENCY_GUIDANCE =
            "This sounds like a potential medical emergency. Please call your local emergency number " +
            "(such as 911 in the US/Canada, 112 in Europe and India, or 999 in the UK) or go to the " +
            "nearest emergency room immediately. Do not wait for an app response."
    }

    private val emergencyKeywords = listOf(
        "severe chest pain", "crushing chest pain", "chest pressure",
        "cannot breathe", "can't breathe", "choking", "turning blue",
        "face drooping", "arm weakness", "slurred speech", "stroke",
        "anaphylaxis", "throat closing", "throat swelling",
        "suicidal", "kill myself", "want to die", "commit suicide", "end my life",
        "massive bleeding", "unconscious", "passed out and not waking up"
    )

    fun check(utterance: String): SafetyInterruptResult {
        val lower = utterance.trim().lowercase(Locale.ROOT)

        // Ignore quotes or hypothetical queries
        if (isHypotheticalOrQuoted(lower)) {
            return SafetyInterruptResult(isEmergency = false)
        }

        // Check each emergency keyword, verifying it is NOT negated
        for (kw in emergencyKeywords) {
            if (lower.contains(kw) && !isNegated(lower, kw)) {
                return SafetyInterruptResult(
                    isEmergency = true,
                    message = EMERGENCY_GUIDANCE
                )
            }
        }

        return SafetyInterruptResult(isEmergency = false)
    }

    private fun isHypotheticalOrQuoted(lower: String): Boolean {
        return lower.startsWith("in a movie") ||
                lower.startsWith("hypothetically") ||
                lower.startsWith("what if in a book") ||
                lower.contains("he asked '") ||
                lower.contains("she asked '") ||
                lower.contains("the doctor said '")
    }

    private fun isNegated(text: String, keyword: String): Boolean {
        val idx = text.indexOf(keyword)
        if (idx <= 0) return false
        val prefix = text.substring(0, idx).takeLast(30)
        return prefix.contains("no ") ||
                prefix.contains("not ") ||
                prefix.contains("don't have ") ||
                prefix.contains("do not have ") ||
                prefix.contains("without ") ||
                prefix.contains("free of ")
    }
}
