package com.example.conversation

import com.example.rag.EvidenceStatus

enum class RetrievalDecision {
    RETRIEVE,
    REUSE_CACHE,
    SKIP,
    DEGRADED
}

class AdaptiveRetrievalController {

    private val socialGreetings = setOf(
        "hello", "hi", "hey", "good morning", "good afternoon", "good evening",
        "thanks", "thank you", "okay", "ok", "bye", "goodbye", "yes", "no"
    )

    fun decide(
        state: SessionState,
        utterance: String,
        isPackAvailable: Boolean
    ): RetrievalDecision {
        if (!isPackAvailable) {
            return RetrievalDecision.DEGRADED
        }

        val lower = utterance.trim().lowercase()

        // Pure greetings or social pleasantries without medical content
        if (socialGreetings.contains(lower) && state.facts.isEmpty()) {
            return RetrievalDecision.SKIP
        }

        // If user is answering a clarification question for an existing relevant topic
        if (state.evidenceBundle.status == EvidenceStatus.RELEVANT &&
            state.pendingQuestion != null &&
            lower.split(Regex("\\s+")).size <= 3
        ) {
            return RetrievalDecision.REUSE_CACHE
        }

        return RetrievalDecision.RETRIEVE
    }
}
