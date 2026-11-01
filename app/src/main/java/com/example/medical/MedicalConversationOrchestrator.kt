package com.example.medical

import com.example.medical.input.MedicalInputProcessor
import com.example.medical.knowledge.MedicalKnowledgeRepository
import com.example.medical.model.MedicalEvidence
import com.example.medical.model.MedicalInput
import com.example.medical.model.MedicalResponseResult
import com.example.medical.prompt.MedicalPromptBuilder
import com.example.medical.safety.MedicalResponseValidator
import com.example.medical.safety.MedicalSafetyEngine
import com.example.medical.safety.SafetyAction
import com.example.voice.llm.LocalLlmEngine

/**
 * Coordinates end-to-end medical conversation flow:
 * 1. Text normalization & intent classification
 * 2. Pre-generation safety engine triage (emergency bypass)
 * 3. Verified medical knowledge retrieval
 * 4. Bounded prompt construction
 * 5. Buffered local LLM token generation (never streamed directly)
 * 6. Post-generation output validation & grounding check
 */
class MedicalConversationOrchestrator(
    private val inputProcessor: MedicalInputProcessor = MedicalInputProcessor(),
    private val safetyEngine: MedicalSafetyEngine = MedicalSafetyEngine(),
    private val knowledgeRepository: MedicalKnowledgeRepository,
    private val promptBuilder: MedicalPromptBuilder = MedicalPromptBuilder(),
    private val responseValidator: MedicalResponseValidator = MedicalResponseValidator(),
    private val llmEngine: LocalLlmEngine? = null
) {

    suspend fun processQuery(
        userText: String,
        conversationHistory: List<Pair<String, String>> = emptyList()
    ): MedicalResponseResult {
        // Step 1: Input processing & symptom extraction
        val medicalInput = inputProcessor.process(userText)

        // Step 2: Safety triage (immediate bypass for emergency / out-of-scope)
        val decision = safetyEngine.inspectInput(medicalInput)
        when (decision.action) {
            SafetyAction.ESCALATE -> {
                return MedicalResponseResult.Escalation(
                    reviewedTemplateId = decision.ruleId,
                    message = decision.publicMessage ?: MedicalSafetyEngine.EMERGENCY_ESCALATION_TEMPLATE
                )
            }
            SafetyAction.CLARIFY -> {
                return MedicalResponseResult.Clarification(
                    question = decision.publicMessage ?: MedicalSafetyEngine.CLARIFICATION_TEMPLATE
                )
            }
            SafetyAction.OUT_OF_SCOPE -> {
                return MedicalResponseResult.Clarification(
                    question = decision.publicMessage ?: MedicalSafetyEngine.OUT_OF_SCOPE_TEMPLATE
                )
            }
            SafetyAction.ALLOW -> {
                // Proceed with knowledge retrieval and inference
            }
        }

        // Step 3: Verified Knowledge Retrieval
        val evidence = knowledgeRepository.retrieve(medicalInput.normalizedText, topK = 3)

        // Step 4: Context & Prompt Construction
        val prompt = promptBuilder.buildPrompt(medicalInput, evidence, conversationHistory)

        // Step 5: Check LLM engine availability
        val engine = llmEngine
        if (engine == null || !engine.isModelLoaded.value) {
            return if (evidence.isNotEmpty()) {
                val top = evidence.first()
                MedicalResponseResult.Answer(
                    text = "${top.title}: ${top.excerpt} Please consult a qualified doctor if symptoms persist.",
                    sources = listOf(top)
                )
            } else {
                MedicalResponseResult.Unavailable(
                    reason = "AI model is not currently loaded.",
                    safeMessage = "The on-device AI model is not installed. Please download or initialize the model in settings."
                )
            }
        }

        // Step 6: Buffered token generation (NEVER streamed to user or TTS before validation!)
        val rawCandidate = buildString {
            engine.generate(prompt, maxTokens = 256).collect { token ->
                append(token)
            }
        }

        // Step 7: Output safety validation & grounding check
        val validation = responseValidator.validateAndSanitize(rawCandidate, evidence)
        val citedEvidence = evidence.filter { ev -> validation.citedSources.any { it.value == ev.sourceId.value } }
        return MedicalResponseResult.Answer(
            text = validation.sanitizedText,
            sources = citedEvidence
        )
    }
}
