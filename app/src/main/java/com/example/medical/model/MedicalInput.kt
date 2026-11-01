package com.example.medical.model

enum class HealthRequestType {
    GENERAL_HEALTH_INFORMATION,
    SYMPTOM_INFORMATION,
    REQUESTS_DIAGNOSIS_OR_PRESCRIPTION,
    POSSIBLE_URGENT_CONCERN,
    UNCLEAR,
    OUT_OF_SCOPE
}

data class MedicalInput(
    val originalText: String,
    val normalizedText: String,
    val language: String = "en",
    val requestType: HealthRequestType,
    val explicitlyReportedSymptoms: List<String>,
    val explicitlyReportedDuration: String?,
    val uncertaintyNotes: List<String>
)
