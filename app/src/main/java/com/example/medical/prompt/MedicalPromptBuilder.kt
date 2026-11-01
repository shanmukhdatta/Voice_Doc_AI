package com.example.medical.prompt

import com.example.medical.model.MedicalEvidence
import com.example.medical.model.MedicalInput

/**
 * Builds formatted, evidence-aware prompts using the Qwen3 / Qwen ChatML chat template.
 * Enforces strict system grounding, concise responses, source citation requirements,
 * non-thinking direct inference, and context token budgeting.
 */
class MedicalPromptBuilder(
    private val maxContextTokens: Int = 2048,
    private val reserveResponseTokens: Int = 256
) {

    fun buildPrompt(
        medicalInput: MedicalInput,
        evidence: List<MedicalEvidence>,
        conversationHistory: List<Pair<String, String>> = emptyList() // (userText, assistantText)
    ): String {
        val systemPrompt = buildSystemPrompt()

        val evidenceBlock = if (evidence.isNotEmpty()) {
            val formatted = evidence.joinToString("\n\n") { ev ->
                "Source [${ev.sourceId.value}]: ${ev.title}\n${ev.excerpt}"
            }
            "Verified Medical Reference Excerpts:\n$formatted\n"
        } else {
            "Verified Medical Reference Excerpts: None available for this specific query.\n"
        }

        // Bounded recent history (last 2-3 turns)
        val recentHistory = conversationHistory.takeLast(3)
        val historyBlock = if (recentHistory.isNotEmpty()) {
            val formatted = recentHistory.joinToString("\n") { (user, assistant) ->
                "User: $user\nAssistant: $assistant"
            }
            "Recent Conversation History:\n$formatted\n\n"
        } else {
            ""
        }

        val userInstruction = buildUserInstruction(medicalInput, evidenceBlock, historyBlock)

        // Calculate approximate budget (approx 4 chars per token)
        val availableChars = (maxContextTokens - reserveResponseTokens) * 4
        val totalChars = systemPrompt.length + userInstruction.length

        val finalInstruction = if (totalChars > availableChars) {
            // Trim history if over budget
            val trimmedHistoryBlock = ""
            buildUserInstruction(medicalInput, evidenceBlock, trimmedHistoryBlock)
        } else {
            userInstruction
        }

        return "<|im_start|>system\n$systemPrompt<|im_end|>\n" +
                "<|im_start|>user\n$finalInstruction<|im_end|>\n" +
                "<|im_start|>assistant\n"
    }

    private fun buildSystemPrompt(): String {
        return "You are Voice Doctor AI, a compassionate, offline health-information companion.\n" +
                "You are NOT a licensed physician, cannot prescribe medication, and cannot make official clinical diagnoses.\n" +
                "Rules:\n" +
                "1. Ground your guidance STRICTLY on the provided Verified Medical Reference Excerpts.\n" +
                "2. When stating facts from an excerpt, cite its source tag (e.g. [MED_COLD_001]). Do NOT invent citation tags.\n" +
                "3. If no reference is provided or symptoms are unclear, give safe general self-care advice and suggest consulting a healthcare professional.\n" +
                "4. Keep your answer clear, empathetic, and concise: exactly 2 to 4 sentences.\n" +
                "5. Never output internal thoughts, chain-of-thought, or <think> tags. Provide your final answer directly."
    }

    private fun buildUserInstruction(
        medicalInput: MedicalInput,
        evidenceBlock: String,
        historyBlock: String
    ): String {
        val userDetails = buildString {
            append("User Query: \"${medicalInput.originalText}\"\n")
            if (medicalInput.explicitlyReportedSymptoms.isNotEmpty()) {
                append("Identified Symptoms: ${medicalInput.explicitlyReportedSymptoms.joinToString(", ")}\n")
            }
            if (medicalInput.explicitlyReportedDuration != null) {
                append("Duration: ${medicalInput.explicitlyReportedDuration}\n")
            }
            if (medicalInput.uncertaintyNotes.isNotEmpty()) {
                append("Notes/Observations: ${medicalInput.uncertaintyNotes.joinToString(", ")}\n")
            }
        }

        return "$evidenceBlock\n$historyBlock$userDetails\nPlease provide concise, supportive health guidance based on the excerpts."
    }
}
