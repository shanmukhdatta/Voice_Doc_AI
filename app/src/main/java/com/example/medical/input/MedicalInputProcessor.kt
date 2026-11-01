package com.example.medical.input

import com.example.medical.model.HealthRequestType
import com.example.medical.model.MedicalInput
import java.util.Locale
import java.util.regex.Pattern

class MedicalInputProcessor {

    companion object {
        private val COMMON_SYMPTOMS = listOf(
            "headache", "fever", "cough", "sore throat", "nausea",
            "vomiting", "diarrhea", "chills", "fatigue", "dizziness",
            "congestion", "runny nose", "sneezing", "stomach ache",
            "abdominal pain", "muscle ache", "body ache", "rash",
            "earache", "back pain", "itchy eyes", "loss of appetite"
        )

        private val URGENT_SIGNALS = listOf(
            "chest pain", "crushing pain", "heart attack", "can't breathe",
            "cannot breathe", "difficulty breathing", "shortness of breath",
            "struggling to breathe", "severe breathlessness",
            "face drooping", "arm weakness", "slurred speech", "stroke",
            "unconscious", "unresponsive", "passed out", "fainting",
            "coughing blood", "vomiting blood", "throwing up blood",
            "severe allergic reaction", "anaphylaxis", "throat closing", "tongue swollen",
            "suicide", "suicidal", "kill myself", "end my life", "self harm",
            "worst headache of my life", "sudden thunderclap headache",
            "severe uncontrollable bleeding", "severe blood loss", "heavy bleeding",
            "seizure", "convulsing", "blue lips"
        )

        private val DIAGNOSIS_OR_PRESCRIPTION_PATTERNS = listOf(
            "diagnose me", "what is my diagnosis", "my diagnosis", "do i have",
            "prescribe me", "can you prescribe", "write a prescription", "prescription note", "prescription for",
            "give me a prescription", "what prescription drug", "antibiotics dosage",
            "what medicine should i take to cure", "order tests for me",
            "milligram dose", "dose of", "exact dose", "should i inject", "inject"
        )

        private val OUT_OF_SCOPE_PATTERNS = listOf(
            "ignore previous instructions", "disregard all prior instructions",
            "system prompt", "you are now a", "you are now dan", "jailbreak", "dan,", "mode enabled",
            "write python", "write code", "poem", "capital of",
            "stock market", "stock price", "crypto", "who won the", "election",
            "make a bomb", "bomb", "recipe", "cookies", "car transmission", "broken car", "translate",
            "quantum mechanics", "quantum", "essay", "bypass android", "sandbox", "sql injection", "exploit",
            "pretend to be", "evil ai", "hack a wifi", "forge prescriptions"
        )

        private val VAGUE_OR_AMBIGUOUS_PATTERNS = listOf(
            "feel bad", "feeling unwell", "not feeling good", "body hurts",
            "something is wrong", "what is this", "is it normal", "help me doctor",
            "tell me", "advice please", "can you check me", "it hurts"
        )

        private val DURATION_REGEXES = listOf(
            Pattern.compile("\\b(?:for|past|over|last)\\s+(\\d+|a|an|two|three|four|five|six|seven|several|few)\\s+(?:hours?|days?|weeks?|months?)\\b", Pattern.CASE_INSENSITIVE),
            Pattern.compile("\\bsince\\s+(yesterday|last\\s+night|this\\s+morning|last\\s+week|a\\s+few\\s+days\\s+ago|monday|tuesday|wednesday|thursday|friday|saturday|sunday)\\b", Pattern.CASE_INSENSITIVE),
            Pattern.compile("\\b(\\d+)\\s*(?:hours?|days?|weeks?|months?)\\s+ago\\b", Pattern.CASE_INSENSITIVE)
        )
    }

    fun process(rawText: String, language: String = "en"): MedicalInput {
        val normalized = normalize(rawText)
        val requestType = categorize(normalized)
        val symptoms = extractSymptoms(normalized)
        val duration = extractDuration(normalized)

        val uncertaintyNotes = mutableListOf<String>()
        if (requestType == HealthRequestType.UNCLEAR) {
            uncertaintyNotes.add("Input text is too short, ambiguous, or lacks clinical context.")
        }
        if (symptoms.isEmpty() && requestType == HealthRequestType.SYMPTOM_INFORMATION) {
            uncertaintyNotes.add("Subjective symptom query detected but specific symptoms could not be isolated.")
        }

        return MedicalInput(
            originalText = rawText,
            normalizedText = normalized,
            language = language,
            requestType = requestType,
            explicitlyReportedSymptoms = symptoms,
            explicitlyReportedDuration = duration,
            uncertaintyNotes = uncertaintyNotes
        )
    }

    fun normalize(text: String): String {
        return text
            .replace('“', '"')
            .replace('”', '"')
            .replace('’', '\'')
            .replace('‘', '\'')
            .replace(Regex("\\s+"), " ")
            .trim()
    }

    private fun categorize(text: String): HealthRequestType {
        val lower = text.lowercase(Locale.ENGLISH)

        // 1. Check prompt injection / out of scope first
        for (pattern in OUT_OF_SCOPE_PATTERNS) {
            if (lower.contains(pattern)) {
                return HealthRequestType.OUT_OF_SCOPE
            }
        }

        // 2. Red-flag urgent concerns (conservative affirmative check)
        for (urgent in URGENT_SIGNALS) {
            val idx = lower.indexOf(urgent)
            if (idx != -1) {
                val prefix = lower.substring(maxOf(0, idx - 25), idx)
                val isNeg = prefix.contains("no ") || prefix.contains("not ") ||
                        prefix.contains("never ") || prefix.contains("without ") ||
                        prefix.contains("don't have ") || prefix.contains("dont have ")
                if (!isNeg) {
                    return HealthRequestType.POSSIBLE_URGENT_CONCERN
                }
            }
        }

        // 3. Demands for diagnosis or prescription drugs
        for (dp in DIAGNOSIS_OR_PRESCRIPTION_PATTERNS) {
            if (lower.contains(dp)) {
                return HealthRequestType.REQUESTS_DIAGNOSIS_OR_PRESCRIPTION
            }
        }

        // 4. Too brief / unclear or ambiguous
        val words = lower.split(Regex("[^a-zA-Z0-9]+")).filter { it.isNotBlank() }
        if (words.size <= 1 && !COMMON_SYMPTOMS.any { lower.contains(it) }) {
            return HealthRequestType.UNCLEAR
        }
        if (VAGUE_OR_AMBIGUOUS_PATTERNS.any { lower.contains(it) } && !COMMON_SYMPTOMS.any { lower.contains(it) }) {
            return HealthRequestType.UNCLEAR
        }

        // 5. Symptom reports
        for (symptom in COMMON_SYMPTOMS) {
            if (lower.contains(symptom)) {
                return HealthRequestType.SYMPTOM_INFORMATION
            }
        }

        // 6. General medical/wellness question
        if (lower.contains("how") || lower.contains("what") || lower.contains("why") ||
            lower.contains("tips") || lower.contains("water") || lower.contains("sleep") ||
            lower.contains("diet") || lower.contains("exercise") || lower.contains("prevention") ||
            lower.contains("care") || lower.contains("cold") || lower.contains("allergy")) {
            return HealthRequestType.GENERAL_HEALTH_INFORMATION
        }

        return HealthRequestType.GENERAL_HEALTH_INFORMATION
    }

    private fun extractSymptoms(text: String): List<String> {
        val lower = text.lowercase(Locale.ENGLISH)
        val detected = mutableListOf<String>()

        for (symptom in COMMON_SYMPTOMS) {
            val idx = lower.indexOf(symptom)
            if (idx != -1) {
                // Check for immediate negation in preceding 20 chars
                val prefix = lower.substring(maxOf(0, idx - 20), idx)
                val isNegated = prefix.contains("no ") || prefix.contains("not ") ||
                        prefix.contains("never ") || prefix.contains("without ") ||
                        prefix.contains("don't have ") || prefix.contains("dont have ") ||
                        prefix.contains("haven't had ") || prefix.contains("havent had ")

                if (!isNegated) {
                    detected.add(symptom)
                }
            }
        }

        return detected.distinct()
    }

    private fun extractDuration(text: String): String? {
        for (pattern in DURATION_REGEXES) {
            val matcher = pattern.matcher(text)
            if (matcher.find()) {
                return matcher.group(0)?.trim()
            }
        }
        return null
    }
}
