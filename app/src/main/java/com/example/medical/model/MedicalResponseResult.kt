package com.example.medical.model

sealed interface MedicalResponseResult {
    data class Answer(
        val text: String,
        val sources: List<MedicalEvidence>
    ) : MedicalResponseResult

    data class Clarification(
        val question: String
    ) : MedicalResponseResult

    data class Escalation(
        val reviewedTemplateId: String,
        val message: String
    ) : MedicalResponseResult

    data class Unavailable(
        val reason: String,
        val safeMessage: String
    ) : MedicalResponseResult
}
