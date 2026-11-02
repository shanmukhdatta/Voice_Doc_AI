package com.example.medical

import com.example.conversation.FactCategory
import com.example.conversation.SessionState
import com.example.rag.EvidenceBundle
import com.example.rag.EvidenceStatus
import com.example.voice.llm.LocalLlmEngine
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

enum class PlannerActionType {
    ASK,
    ANSWER,
    KNOWLEDGE_GAP,
    POSSIBLE_URGENT
}

data class PlannerAction(
    val type: PlannerActionType,
    val content: String,
    val questionCategory: FactCategory? = null,
    val citationIds: List<String> = emptyList()
)

class QwenConversationPlanner(
    private val llmEngine: LocalLlmEngine
) {
    companion object {
        const val MAX_QUESTIONS_PER_TOPIC = 3
    }

    fun buildPrompt(state: SessionState, utterance: String, bundle: EvidenceBundle): String {
        val evidenceText = if (state.evidenceModeEnabled) {
            bundle.formatForPrompt()
        } else {
            "[Evidence Mode is OFF - LLM Only]"
        }

        val factsSummary = if (state.facts.isEmpty()) {
            "None reported yet."
        } else {
            state.facts.joinToString("; ") { "${it.category.name}: ${it.value} (${it.polarity.name})" }
        }

        val historyLines = state.recentTurns.takeLast(2).joinToString("\n") { turn ->
            "User: ${turn.userUtterance}\nDoctor: ${turn.doctorResponse}"
        }

        val canAsk = state.canAskAnotherQuestion()

        val prompt = StringBuilder()
        prompt.append("<|im_start|>system\n")
        prompt.append("You are Voice Doctor AI, an offline medical health information assistant.\n")
        prompt.append("Provide safe, helpful medical information based ONLY on verified evidence.\n")
        prompt.append("Respond directly and concisely without <think> tags.\n\n")
        prompt.append("VERIFIED MEDICAL EVIDENCE:\n")
        prompt.append(evidenceText).append("\n\n")
        prompt.append("INSTRUCTIONS:\n")
        if (canAsk) {
            prompt.append("- If you need key clarification (location, duration, or severity) to understand symptoms, format as:\n")
            prompt.append("  ASK: <one single clarifying question>\n")
        } else {
            prompt.append("- You have already asked clarifying questions. Now provide final summary guidance:\n")
        }
        prompt.append("- When providing information based on evidence, cite sources using [MP_xxxx_xx]:\n")
        prompt.append("  ANSWER: <concise summary citing sources>\n")
        prompt.append("- If verified offline evidence is missing or insufficient:\n")
        prompt.append("  KNOWLEDGE_GAP: <brief statement of offline knowledge limitation and advice to see a doctor>\n")
        prompt.append("- If symptoms suggest urgent care:\n")
        prompt.append("  POSSIBLE_URGENT: <guidance to seek urgent medical evaluation>\n")
        prompt.append("<|im_end|>\n")

        if (historyLines.isNotEmpty()) {
            prompt.append("<|im_start|>user\nPrevious context:\n")
            prompt.append(historyLines).append("\n<|im_end|>\n")
        }

        prompt.append("<|im_start|>user\n")
        prompt.append("Confirmed user facts: ").append(factsSummary).append("\n")
        prompt.append("User message: ").append(utterance.trim()).append("\n")
        prompt.append("<|im_end|>\n")
        prompt.append("<|im_start|>assistant\n")

        return prompt.toString()
    }

    fun generateStreaming(prompt: String): Flow<String> {
        return llmEngine.generate(prompt)
    }

    fun parseOutput(rawOutput: String, evidenceBundle: EvidenceBundle): PlannerAction {
        val clean = rawOutput.replace(Regex("<think>[\\s\\S]*?</think>"), "").trim()

        val citations = extractCitations(clean)

        return when {
            clean.startsWith("ASK:", ignoreCase = true) -> {
                val qText = clean.substringAfter(":").trim()
                val cat = guessQuestionCategory(qText)
                PlannerAction(PlannerActionType.ASK, qText, cat)
            }
            clean.startsWith("KNOWLEDGE_GAP:", ignoreCase = true) -> {
                val text = clean.substringAfter(":").trim()
                PlannerAction(PlannerActionType.KNOWLEDGE_GAP, text)
            }
            clean.startsWith("POSSIBLE_URGENT:", ignoreCase = true) -> {
                val text = clean.substringAfter(":").trim()
                PlannerAction(PlannerActionType.POSSIBLE_URGENT, text)
            }
            clean.startsWith("ANSWER:", ignoreCase = true) -> {
                val text = clean.substringAfter(":").trim()
                PlannerAction(PlannerActionType.ANSWER, text, citationIds = citations)
            }
            clean.endsWith("?") && !clean.contains(".") -> {
                // Inferred question
                PlannerAction(PlannerActionType.ASK, clean, guessQuestionCategory(clean))
            }
            evidenceBundle.status == EvidenceStatus.NONE && evidenceBundle.retrievalQuery.isNotBlank() -> {
                // If evidence was NONE and model didn't use prefix, treat as KNOWLEDGE_GAP if ungrounded
                PlannerAction(PlannerActionType.KNOWLEDGE_GAP, clean)
            }
            else -> {
                PlannerAction(PlannerActionType.ANSWER, clean, citationIds = citations)
            }
        }
    }

    private fun extractCitations(text: String): List<String> {
        val regex = Regex("\\[(MP_[A-Za-z0-9_]+)\\]")
        return regex.findAll(text).map { it.groupValues[1] }.distinct().toList()
    }

    private fun guessQuestionCategory(questionText: String): FactCategory {
        val lower = questionText.lowercase()
        return when {
            lower.contains("where") || lower.contains("location") || lower.contains("which side") || lower.contains("which part") -> FactCategory.LOCATION
            lower.contains("how long") || lower.contains("when did") || lower.contains("duration") -> FactCategory.DURATION
            lower.contains("how bad") || lower.contains("how severe") || lower.contains("scale") || lower.contains("mild") -> FactCategory.SEVERITY
            else -> FactCategory.SYMPTOM
        }
    }
}
