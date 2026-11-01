package com.example.medical.safety

import com.example.medical.model.MedicalEvidence
import com.example.medical.model.MedicalResponseResult
import com.example.medical.model.SourceId
import java.util.regex.Pattern

/**
 * Validates, sanitizes, and verifies grounding of LLM output before it is ever
 * presented to the user or passed to Text-to-Speech.
 */
class MedicalResponseValidator {

    data class ValidationResult(
        val isValid: Boolean,
        val sanitizedText: String,
        val citedSources: List<SourceId>,
        val rejectionReason: String? = null
    )

    fun validateAndSanitize(
        candidateText: String,
        providedEvidence: List<MedicalEvidence>
    ): ValidationResult {
        // 1. Strip special tokens and reasoning tags
        var cleaned = candidateText
            .replace(Regex("<think>[\\s\\S]*?</think>"), "")
            .replace("<|im_start|>", "")
            .replace("<|im_end|>", "")
            .replace("<|endoftext|>", "")
            .trim()

        if (cleaned.isBlank()) {
            return ValidationResult(
                isValid = false,
                sanitizedText = SAFE_FALLBACK_TEXT,
                citedSources = emptyList(),
                rejectionReason = "Empty or whitespace-only model output"
            )
        }

        // 2. Length check (< 1200 characters)
        if (cleaned.length > 1200) {
            cleaned = cleaned.take(1200).substringBeforeLast(".") + "."
        }

        // 3. Check for forbidden prescriptive dosage
        if (DOSAGE_PATTERN.matcher(cleaned).find()) {
            return ValidationResult(
                isValid = false,
                sanitizedText = "For medication advice and dosage instructions, please consult a licensed physician or pharmacist. " +
                        "Self-medication without professional oversight may be unsafe.",
                citedSources = emptyList(),
                rejectionReason = "Model attempted to prescribe specific drug dosages"
            )
        }

        // 4. Check for forbidden definitive diagnosis
        if (DIAGNOSIS_PATTERN.matcher(cleaned).find()) {
            return ValidationResult(
                isValid = false,
                sanitizedText = SAFE_FALLBACK_TEXT,
                citedSources = emptyList(),
                rejectionReason = "Model attempted an assertive clinical diagnosis"
            )
        }

        // 5. Grounding check on source tags:
        // Citations are NEVER automatically assigned without explicit demonstrated support.
        val validSourceIds = providedEvidence.map { it.sourceId.value }.toSet()
        val foundSources = mutableListOf<SourceId>()
        val matcher = SOURCE_TAG_PATTERN.matcher(cleaned)
        var hasHallucinatedSource = false

        while (matcher.find()) {
            val tag = matcher.group(1)
            if (tag != null && tag in validSourceIds) {
                foundSources.add(SourceId(tag))
            } else {
                hasHallucinatedSource = true
            }
        }

        // If it hallucinated fake source IDs, remove invalid tags cleanly
        if (hasHallucinatedSource) {
            cleaned = matcher.replaceAll { mr ->
                val tag = mr.group(1)
                if (tag != null && tag in validSourceIds) mr.group(0) ?: "" else ""
            }.replace(Regex("\\s+"), " ").trim()
        }

        // Handle missing evidence conservatively:
        // When no verified evidence was provided, ensure conservative guidance
        if (providedEvidence.isEmpty()) {
            val lowerCleaned = cleaned.lowercase()
            if (!lowerCleaned.contains("doctor") && !lowerCleaned.contains("physician") && !lowerCleaned.contains("healthcare provider")) {
                cleaned = "$cleaned Please consult a healthcare professional for specific medical advice."
            }
        }

        return ValidationResult(
            isValid = true,
            sanitizedText = cleaned,
            citedSources = foundSources.distinct()
        )
    }

    companion object {
        const val SAFE_FALLBACK_TEXT =
            "I can share general health information, but I cannot provide an official medical diagnosis. " +
            "Please rest, stay hydrated, and consult a qualified healthcare provider if symptoms persist or worsen."

        private val DOSAGE_PATTERN = Pattern.compile(
            "\\b(take|dose|administer|inject)\\s+\\d+(\\.\\d+)?\\s*(mg|ml|mcg|grams?|tablets?|pills?|capsules?)\\b",
            Pattern.CASE_INSENSITIVE
        )

        private val DIAGNOSIS_PATTERN = Pattern.compile(
            "\\b(you have|i diagnose you with|you are diagnosed with|my diagnosis is)\\b",
            Pattern.CASE_INSENSITIVE
        )

        private val SOURCE_TAG_PATTERN = Pattern.compile("\\[([A-Z0-9_]{5,30})\\]")
    }
}
