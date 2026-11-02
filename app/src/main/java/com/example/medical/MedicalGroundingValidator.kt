package com.example.medical

import com.example.conversation.FactCategory
import com.example.conversation.SessionState
import com.example.rag.EvidenceBundle
import com.example.rag.EvidenceStatus

data class ValidatedAction(
    val type: PlannerActionType,
    val text: String,
    val pendingCategory: FactCategory? = null,
    val verifiedCitationIds: List<String> = emptyList(),
    val wasRepaired: Boolean = false
)

class MedicalGroundingValidator {

    companion object {
        const val KNOWLEDGE_GAP_FALLBACK =
            "I do not have verified offline medical guidelines for this specific topic in my database. " +
            "For your safety, please consult a healthcare professional for clinical advice."
    }

    fun validate(
        action: PlannerAction,
        state: SessionState,
        evidenceBundle: EvidenceBundle
    ): ValidatedAction {
        return when (action.type) {
            PlannerActionType.ASK -> validateAskAction(action, state, evidenceBundle)
            PlannerActionType.ANSWER -> validateAnswerAction(action, state, evidenceBundle)
            PlannerActionType.KNOWLEDGE_GAP -> validateKnowledgeGapAction(action)
            PlannerActionType.POSSIBLE_URGENT -> validateUrgentAction(action)
        }
    }

    private fun validateAskAction(
        action: PlannerAction,
        state: SessionState,
        evidenceBundle: EvidenceBundle
    ): ValidatedAction {
        // Enforce max question limit
        if (!state.canAskAnotherQuestion()) {
            return fallbackToAnswer(evidenceBundle)
        }

        val questionText = action.content.trim()

        // Check if question was already asked
        val alreadyAsked = state.askedQuestions.any {
            it.equals(questionText, ignoreCase = true) ||
                    (action.questionCategory != null && it.contains(action.questionCategory.name, ignoreCase = true))
        }

        if (alreadyAsked) {
            return fallbackToAnswer(evidenceBundle)
        }

        // Single question check: if multiple question marks, take the first question
        val firstQuestion = if (questionText.count { it == '?' } > 1) {
            questionText.substringBefore('?') + "?"
        } else {
            questionText
        }

        return ValidatedAction(
            type = PlannerActionType.ASK,
            text = firstQuestion,
            pendingCategory = action.questionCategory ?: FactCategory.SYMPTOM,
            wasRepaired = firstQuestion != questionText
        )
    }

    private fun validateAnswerAction(
        action: PlannerAction,
        state: SessionState,
        evidenceBundle: EvidenceBundle
    ): ValidatedAction {
        // Strip any residual thinking tags
        var cleanedText = action.content
            .replace(Regex("<think>[\\s\\S]*?</think>"), "")
            .replace(Regex("<\\/?think>"), "")
            .trim()

        // If evidence mode is enabled but no evidence is available, convert to KNOWLEDGE_GAP
        if (state.evidenceModeEnabled &&
            (evidenceBundle.status == EvidenceStatus.NONE || evidenceBundle.passages.isEmpty())
        ) {
            return ValidatedAction(
                type = PlannerActionType.KNOWLEDGE_GAP,
                text = KNOWLEDGE_GAP_FALLBACK,
                wasRepaired = true
            )
        }

        val availableIds = evidenceBundle.passages.map { it.id }.toSet()
        val validCitations = mutableListOf<String>()

        // Check each cited ID
        for (id in action.citationIds) {
            if (availableIds.contains(id)) {
                validCitations.add(id)
            } else {
                // Remove hallucinated citation from text
                cleanedText = cleanedText.replace("[$id]", "")
            }
        }

        // If evidence is present but model cited nothing, append top passage citation if grounded
        if (state.evidenceModeEnabled && validCitations.isEmpty() && evidenceBundle.passages.isNotEmpty()) {
            val top = evidenceBundle.passages.first()
            validCitations.add(top.id)
            if (!cleanedText.contains("[${top.id}]")) {
                cleanedText = "$cleanedText [${top.id}]"
            }
        }

        // Check for specific dosage self-administration recommendations (e.g. "take 500mg")
        if (cleanedText.contains(Regex("\\b\\d+\\s*(?:mg|milligrams|pills|tablets)\\b", RegexOption.IGNORE_CASE))) {
            cleanedText = cleanedText.replace(
                Regex("\\b(?:take|ingest|consume)\\s+\\d+\\s*(?:mg|milligrams|pills|tablets)[^.]*\\.", RegexOption.IGNORE_CASE),
                "Consult your pharmacist or physician for specific medication dosage."
            )
        }

        return ValidatedAction(
            type = PlannerActionType.ANSWER,
            text = cleanedText.trim(),
            verifiedCitationIds = validCitations,
            wasRepaired = cleanedText != action.content
        )
    }

    private fun validateKnowledgeGapAction(action: PlannerAction): ValidatedAction {
        val text = action.content.ifBlank { KNOWLEDGE_GAP_FALLBACK }
        return ValidatedAction(
            type = PlannerActionType.KNOWLEDGE_GAP,
            text = text
        )
    }

    private fun validateUrgentAction(action: PlannerAction): ValidatedAction {
        val text = action.content.ifBlank {
            "Your symptoms suggest you should be evaluated by a medical doctor promptly."
        }
        return ValidatedAction(
            type = PlannerActionType.POSSIBLE_URGENT,
            text = text
        )
    }

    private fun fallbackToAnswer(bundle: EvidenceBundle): ValidatedAction {
        if (bundle.passages.isNotEmpty()) {
            val top = bundle.passages.first()
            val summary = top.text.take(300).substringBeforeLast(".") + "."
            return ValidatedAction(
                type = PlannerActionType.ANSWER,
                text = "$summary [${top.id}]",
                verifiedCitationIds = listOf(top.id),
                wasRepaired = true
            )
        }
        return ValidatedAction(
            type = PlannerActionType.KNOWLEDGE_GAP,
            text = KNOWLEDGE_GAP_FALLBACK,
            wasRepaired = true
        )
    }
}
