package com.example.conversation

import java.util.Locale
import java.util.UUID

class SessionStateManager {

    private val commonSymptoms = listOf(
        "headache", "fever", "cough", "sore throat", "stomach pain", "abdominal pain",
        "nausea", "vomiting", "diarrhea", "dizziness", "fatigue", "rash",
        "back pain", "chest pain", "shortness of breath", "runny nose", "congestion",
        "body ache", "chills", "ear pain", "joint pain", "muscle pain", "cramps"
    )

    fun applyUtterance(currentState: SessionState, utterance: String): SessionState {
        val cleanUtterance = utterance.trim()
        if (cleanUtterance.isEmpty()) return currentState

        val newTurnIndex = currentState.turnCount + 1
        val lower = cleanUtterance.lowercase(Locale.ROOT)
        val detectedSubject = detectSubject(lower)

        val updatedFacts = currentState.facts.toMutableList()

        // 1. Check if user is directly answering a pending question
        var resolvedPending = false
        if (currentState.pendingQuestion != null) {
            val pq = currentState.pendingQuestion
            val category = pq.targetCategory
            val fact = UserFact(
                id = UUID.randomUUID().toString(),
                subject = detectedSubject,
                category = category,
                value = cleanUtterance,
                polarity = if (isNegationUtterance(lower)) FactPolarity.NEGATED else FactPolarity.AFFIRMATIVE,
                sourceSpan = cleanUtterance,
                turnIndex = newTurnIndex
            )
            updatedFacts.add(fact)
            resolvedPending = true
        }

        // 2. Check for explicit corrections (e.g. "actually right, not left", "i meant fever")
        val correctionMatch = detectCorrection(lower)
        if (correctionMatch != null) {
            // Find existing fact to supersede
            val targetIdx = updatedFacts.indexOfLast { it.category == correctionMatch.first }
            val supersededId = if (targetIdx >= 0) updatedFacts[targetIdx].id else null
            val correctedFact = UserFact(
                id = UUID.randomUUID().toString(),
                subject = detectedSubject,
                category = correctionMatch.first,
                value = correctionMatch.second,
                polarity = FactPolarity.AFFIRMATIVE,
                sourceSpan = cleanUtterance,
                turnIndex = newTurnIndex,
                correctionOf = supersededId
            )
            if (targetIdx >= 0) {
                updatedFacts.removeAt(targetIdx)
            }
            updatedFacts.add(correctedFact)
        }

        // 3. Extract symptoms & negations from utterance text
        for (sym in commonSymptoms) {
            if (lower.contains(sym)) {
                val isNeg = hasNegationPrefix(lower, sym)
                // Check if already captured in this turn
                val alreadyPresent = updatedFacts.any { it.turnIndex == newTurnIndex && it.value.equals(sym, ignoreCase = true) }
                if (!alreadyPresent) {
                    updatedFacts.add(
                        UserFact(
                            id = UUID.randomUUID().toString(),
                            subject = detectedSubject,
                            category = FactCategory.SYMPTOM,
                            value = sym,
                            polarity = if (isNeg) FactPolarity.NEGATED else FactPolarity.AFFIRMATIVE,
                            sourceSpan = sym,
                            turnIndex = newTurnIndex
                        )
                    )
                }
            }
        }

        // Detect duration patterns (e.g. "for 2 days", "since yesterday", "3 weeks")
        val durationMatch = Regex("\\b(\\d+\\s*(?:day|days|week|weeks|hour|hours|month|months)|since yesterday|since last night)\\b").find(lower)
        if (durationMatch != null && !resolvedPending) {
            updatedFacts.add(
                UserFact(
                    id = UUID.randomUUID().toString(),
                    subject = detectedSubject,
                    category = FactCategory.DURATION,
                    value = durationMatch.value,
                    polarity = FactPolarity.AFFIRMATIVE,
                    sourceSpan = durationMatch.value,
                    turnIndex = newTurnIndex
                )
            )
        }

        return currentState.copy(
            facts = updatedFacts,
            pendingQuestion = if (resolvedPending) null else currentState.pendingQuestion,
            turnCount = newTurnIndex
        )
    }

    private fun detectSubject(lower: String): UserSubject {
        return when {
            lower.contains("my son") || lower.contains("my daughter") || lower.contains("my child") || lower.contains("my kid") || lower.contains("my baby") -> UserSubject.CHILD
            lower.contains("my mother") || lower.contains("my mom") || lower.contains("my father") || lower.contains("my dad") -> UserSubject.PARENT
            lower.contains("my wife") || lower.contains("my husband") || lower.contains("my partner") -> UserSubject.SPOUSE
            else -> UserSubject.SELF
        }
    }

    private fun isNegationUtterance(lower: String): Boolean {
        return lower.startsWith("no") || lower.startsWith("not") || lower.startsWith("none") || lower.contains("i don't") || lower.contains("i do not")
    }

    private fun hasNegationPrefix(text: String, keyword: String): Boolean {
        val idx = text.indexOf(keyword)
        if (idx <= 0) return false
        val prefix = text.substring(0, idx).takeLast(25)
        return prefix.contains("no ") || prefix.contains("not ") || prefix.contains("don't have ") || prefix.contains("without ") || prefix.contains("neither ")
    }

    private fun detectCorrection(lower: String): Pair<FactCategory, String>? {
        if (lower.contains("actually") || lower.contains("i meant") || lower.contains("correction:")) {
            if (lower.contains("right") || lower.contains("left") || lower.contains("upper") || lower.contains("lower")) {
                val loc = when {
                    lower.contains("upper right") -> "upper right"
                    lower.contains("lower right") -> "lower right"
                    lower.contains("upper left") -> "upper left"
                    lower.contains("lower left") -> "lower left"
                    lower.contains("right") -> "right"
                    lower.contains("left") -> "left"
                    else -> "specified location"
                }
                return Pair(FactCategory.LOCATION, loc)
            }
        }
        return null
    }
}
