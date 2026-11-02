package com.example.medical

import com.example.conversation.AdaptiveRetrievalController
import com.example.conversation.ContextualQueryBuilder
import com.example.conversation.PendingQuestion
import com.example.conversation.RetrievalDecision
import com.example.conversation.SessionState
import com.example.conversation.SessionStateManager
import com.example.conversation.TurnRecord
import com.example.rag.EvidenceBundle
import com.example.rag.HybridRetriever
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

sealed interface OrchestratorUpdate {
    data class Token(val text: String) : OrchestratorUpdate
    data class Complete(
        val validatedAction: ValidatedAction,
        val evidenceBundle: EvidenceBundle,
        val updatedState: SessionState
    ) : OrchestratorUpdate
}

class ConversationalRagOrchestrator(
    private val safetyInterrupt: NarrowSafetyInterrupt = NarrowSafetyInterrupt(),
    private val sessionStateManager: SessionStateManager = SessionStateManager(),
    private val contextualQueryBuilder: ContextualQueryBuilder = ContextualQueryBuilder(),
    private val adaptiveRetrievalController: AdaptiveRetrievalController = AdaptiveRetrievalController(),
    private val planner: QwenConversationPlanner,
    private val validator: MedicalGroundingValidator = MedicalGroundingValidator(),
    private var hybridRetriever: HybridRetriever? = null
) {
    fun setRetriever(retriever: HybridRetriever) {
        this.hybridRetriever = retriever
    }

    suspend fun executeTurn(
        initialState: SessionState,
        userUtterance: String
    ): Flow<OrchestratorUpdate> = flow {
        val cleanUtterance = userUtterance.trim()

        // 1. Narrow Safety Check
        val safetyCheck = safetyInterrupt.check(cleanUtterance)
        if (safetyCheck.isEmergency) {
            val emergencyAction = ValidatedAction(
                type = PlannerActionType.POSSIBLE_URGENT,
                text = safetyCheck.message ?: NarrowSafetyInterrupt.EMERGENCY_GUIDANCE
            )
            val updatedState = initialState.copy(
                recentTurns = initialState.recentTurns + TurnRecord(
                    turnIndex = initialState.turnCount + 1,
                    userUtterance = cleanUtterance,
                    doctorResponse = emergencyAction.text,
                    actionType = emergencyAction.type.name
                ),
                turnCount = initialState.turnCount + 1
            )
            emit(OrchestratorUpdate.Complete(emergencyAction, EvidenceBundle.EMPTY, updatedState))
            return@flow
        }

        // 2. State update: apply user facts FIRST
        var state = sessionStateManager.applyUtterance(initialState, cleanUtterance)

        // 3. Adaptive retrieval decision
        val isPackReady = hybridRetriever != null
        val decision = adaptiveRetrievalController.decide(state, cleanUtterance, isPackReady)

        val bundle = when (decision) {
            RetrievalDecision.RETRIEVE -> {
                val query = contextualQueryBuilder.buildQuery(state, cleanUtterance)
                val retrieved = hybridRetriever?.retrieve(query) ?: EvidenceBundle.UNAVAILABLE
                state = state.copy(evidenceBundle = retrieved, currentTopic = query)
                retrieved
            }
            RetrievalDecision.REUSE_CACHE -> {
                state.evidenceBundle
            }
            RetrievalDecision.SKIP -> {
                EvidenceBundle.EMPTY
            }
            RetrievalDecision.DEGRADED -> {
                EvidenceBundle.UNAVAILABLE
            }
        }

        // 4. Construct prompt
        val prompt = planner.buildPrompt(state, cleanUtterance, bundle)

        // 5. Generate with Qwen
        val accumulated = StringBuilder()
        planner.generateStreaming(prompt).collect { token ->
            accumulated.append(token)
            emit(OrchestratorUpdate.Token(token))
        }

        // 6. Parse and Validate
        val parsed = planner.parseOutput(accumulated.toString(), bundle)
        val validated = validator.validate(parsed, state, bundle)

        // 7. Update conversation state
        val finalTurnRecord = TurnRecord(
            turnIndex = state.turnCount,
            userUtterance = cleanUtterance,
            doctorResponse = validated.text,
            actionType = validated.type.name
        )

        var finalState = state.copy(
            recentTurns = state.recentTurns + finalTurnRecord
        )

        if (validated.type == PlannerActionType.ASK) {
            val pq = PendingQuestion(
                text = validated.text,
                targetCategory = validated.pendingCategory ?: com.example.conversation.FactCategory.SYMPTOM,
                turnAsked = state.turnCount
            )
            finalState = finalState.copy(
                pendingQuestion = pq,
                questionCountForCurrentTopic = state.questionCountForCurrentTopic + 1,
                askedQuestions = state.askedQuestions + validated.text
            )
        } else {
            finalState = finalState.copy(
                pendingQuestion = null
            )
        }

        emit(OrchestratorUpdate.Complete(validated, bundle, finalState))
    }
}
