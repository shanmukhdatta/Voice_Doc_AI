package com.example.medical.safety

import com.example.medical.model.HealthRequestType
import com.example.medical.model.MedicalInput

enum class SafetyAction {
    ALLOW,
    CLARIFY,
    ESCALATE,
    OUT_OF_SCOPE
}

data class SafetyDecision(
    val action: SafetyAction,
    val ruleId: String,
    val publicMessage: String? = null,
    val clinicalReviewStatus: String = "pending"
)

class MedicalSafetyEngine {

    companion object {
        const val POLICY_VERSION = "2026.4.1"

        const val RULE_RED_FLAG_CHEST_PAIN = "RULE_RED_FLAG_CHEST_PAIN"
        const val RULE_RED_FLAG_BREATHING = "RULE_RED_FLAG_BREATHING"
        const val RULE_RED_FLAG_STROKE = "RULE_RED_FLAG_STROKE"
        const val RULE_RED_FLAG_ANAPHYLAXIS = "RULE_RED_FLAG_ANAPHYLAXIS"
        const val RULE_RED_FLAG_SUICIDE = "RULE_RED_FLAG_SUICIDE"
        const val RULE_RED_FLAG_GENERIC = "RULE_RED_FLAG_GENERIC"
        const val RULE_PRESCRIPTION_DIAGNOSIS_REFUSAL = "RULE_PRESCRIPTION_DIAGNOSIS_REFUSAL"
        const val RULE_PROMPT_INJECTION_OUT_OF_SCOPE = "RULE_PROMPT_INJECTION_OUT_OF_SCOPE"
        const val RULE_UNCLEAR_INPUT = "RULE_UNCLEAR_INPUT"
        const val RULE_ALLOW_GENERAL_HEALTH = "RULE_ALLOW_GENERAL_HEALTH"

        const val EMERGENCY_ESCALATION_TEMPLATE =
            "Your symptoms may indicate an urgent medical situation. " +
            "Please call 911, 112, or your local emergency services, or proceed to the nearest emergency room immediately. " +
            "Do not wait or attempt self-treatment."

        const val SUICIDE_ESCALATION_TEMPLATE =
            "If you or someone you know is going through a tough time or having thoughts of self-harm, " +
            "please know that support is available. Call or text 988 (in the US & Canada) or contact your local mental health helpline immediately. " +
            "Free, confidential help is available 24/7."

        const val DIAGNOSIS_PRESCRIPTION_REFUSAL_TEMPLATE =
            "I am an offline general health companion and cannot provide formal medical diagnoses or prescribe medications. " +
            "Please consult a qualified healthcare provider for clinical evaluation, diagnostic testing, and prescriptions."

        const val OUT_OF_SCOPE_TEMPLATE =
            "Voice Doctor AI is designed exclusively for offline health information and wellness guidance. " +
            "I cannot assist with unrelated queries, code generation, or requests to bypass safety guidelines."

        const val CLARIFICATION_TEMPLATE =
            "Could you please share more details about what you are experiencing, such as the exact symptoms, location, and how long they have lasted?"
    }

    fun inspectInput(input: MedicalInput): SafetyDecision {
        val lower = input.normalizedText.lowercase()

        // 1. Suicidal ideation / crisis check
        if (lower.contains("suicide") || lower.contains("suicidal") || lower.contains("kill myself") || lower.contains("killing myself") || lower.contains("end my life") || lower.contains("ending my life") || lower.contains("self harm") || lower.contains("self harming")) {
            return SafetyDecision(
                action = SafetyAction.ESCALATE,
                ruleId = RULE_RED_FLAG_SUICIDE,
                publicMessage = SUICIDE_ESCALATION_TEMPLATE
            )
        }

        // 2. Stroke red flag
        if (matchesAffirmative(lower, listOf("face drooping", "arm weakness", "slurred speech", "stroke", "numbness on one side"))) {
            return SafetyDecision(
                action = SafetyAction.ESCALATE,
                ruleId = RULE_RED_FLAG_STROKE,
                publicMessage = EMERGENCY_ESCALATION_TEMPLATE
            )
        }

        // 3. Chest pain red flag
        if (matchesAffirmative(lower, listOf("chest pain", "crushing pain", "heart attack", "stabbing chest", "chest pressure"))) {
            return SafetyDecision(
                action = SafetyAction.ESCALATE,
                ruleId = RULE_RED_FLAG_CHEST_PAIN,
                publicMessage = EMERGENCY_ESCALATION_TEMPLATE
            )
        }

        // 4. Severe breathing red flag
        if (matchesAffirmative(lower, listOf("can't breathe", "cannot breathe", "difficulty breathing", "shortness of breath", "struggling to breathe", "gasping for air"))) {
            return SafetyDecision(
                action = SafetyAction.ESCALATE,
                ruleId = RULE_RED_FLAG_BREATHING,
                publicMessage = EMERGENCY_ESCALATION_TEMPLATE
            )
        }

        // 5. Severe allergic reaction / anaphylaxis
        if (matchesAffirmative(lower, listOf("anaphylaxis", "throat closing", "tongue swollen", "severe allergic", "throat is closing", "tongue is swollen"))) {
            return SafetyDecision(
                action = SafetyAction.ESCALATE,
                ruleId = RULE_RED_FLAG_ANAPHYLAXIS,
                publicMessage = EMERGENCY_ESCALATION_TEMPLATE
            )
        }

        // 6. Generic urgent flag
        val genericUrgentSignals = listOf(
            "coughing up bright red blood", "vomiting blood", "bright red blood", "coughing blood",
            "thunderclap", "worst headache of life", "loss of vision",
            "unresponsive", "board-like", "fever of 105"
        )
        if (matchesAffirmative(lower, genericUrgentSignals) || input.requestType == HealthRequestType.POSSIBLE_URGENT_CONCERN) {
            return SafetyDecision(
                action = SafetyAction.ESCALATE,
                ruleId = RULE_RED_FLAG_GENERIC,
                publicMessage = EMERGENCY_ESCALATION_TEMPLATE
            )
        }

        // 7. Prompt injection & out of scope
        if (input.requestType == HealthRequestType.OUT_OF_SCOPE) {
            return SafetyDecision(
                action = SafetyAction.OUT_OF_SCOPE,
                ruleId = RULE_PROMPT_INJECTION_OUT_OF_SCOPE,
                publicMessage = OUT_OF_SCOPE_TEMPLATE
            )
        }

        // 8. Diagnosis or prescription demands
        if (input.requestType == HealthRequestType.REQUESTS_DIAGNOSIS_OR_PRESCRIPTION) {
            return SafetyDecision(
                action = SafetyAction.CLARIFY,
                ruleId = RULE_PRESCRIPTION_DIAGNOSIS_REFUSAL,
                publicMessage = DIAGNOSIS_PRESCRIPTION_REFUSAL_TEMPLATE
            )
        }

        // 9. Unclear / ambiguous
        if (input.requestType == HealthRequestType.UNCLEAR) {
            return SafetyDecision(
                action = SafetyAction.CLARIFY,
                ruleId = RULE_UNCLEAR_INPUT,
                publicMessage = CLARIFICATION_TEMPLATE
            )
        }

        // 10. Allow general and symptom queries into evidence retrieval
        return SafetyDecision(
            action = SafetyAction.ALLOW,
            ruleId = RULE_ALLOW_GENERAL_HEALTH
        )
    }

    private fun matchesAffirmative(text: String, keywords: List<String>): Boolean {
        for (kw in keywords) {
            val idx = text.indexOf(kw)
            if (idx != -1) {
                val prefix = text.substring(maxOf(0, idx - 25), idx)
                val isNegated = prefix.contains("no ") || prefix.contains("not ") ||
                        prefix.contains("never ") || prefix.contains("without ") ||
                        prefix.contains("don't have ") || prefix.contains("dont have ") ||
                        prefix.contains("haven't had ") || prefix.contains("havent had ")
                if (!isNegated) {
                    return true
                }
            }
        }
        return false
    }
}
