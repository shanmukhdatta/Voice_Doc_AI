package com.example.conversation

import com.example.rag.EvidenceBundle
import java.util.UUID

enum class UserSubject {
    SELF,
    CHILD,
    PARENT,
    SPOUSE,
    OTHER
}

enum class FactCategory {
    SYMPTOM,
    DURATION,
    SEVERITY,
    LOCATION,
    MEDICATION,
    ALLERGY,
    TRIGGER,
    NEGATIVE
}

enum class FactPolarity {
    AFFIRMATIVE,
    NEGATED,
    UNCERTAIN
}

data class UserFact(
    val id: String = UUID.randomUUID().toString(),
    val subject: UserSubject = UserSubject.SELF,
    val category: FactCategory,
    val value: String,
    val polarity: FactPolarity = FactPolarity.AFFIRMATIVE,
    val sourceSpan: String,
    val turnIndex: Int,
    val correctionOf: String? = null
)

data class PendingQuestion(
    val questionId: String = UUID.randomUUID().toString(),
    val text: String,
    val targetCategory: FactCategory,
    val turnAsked: Int
)

data class TurnRecord(
    val turnIndex: Int,
    val userUtterance: String,
    val doctorResponse: String,
    val actionType: String
)

data class SessionState(
    val facts: List<UserFact> = emptyList(),
    val recentTurns: List<TurnRecord> = emptyList(),
    val pendingQuestion: PendingQuestion? = null,
    val askedQuestions: Set<String> = emptySet(),
    val questionCountForCurrentTopic: Int = 0,
    val currentTopic: String? = null,
    val evidenceBundle: EvidenceBundle = EvidenceBundle.EMPTY,
    val evidenceModeEnabled: Boolean = true,
    val turnCount: Int = 0
) {
    val activeAffirmativeSymptoms: List<UserFact>
        get() = facts.filter {
            (it.category == FactCategory.SYMPTOM || it.category == FactCategory.LOCATION || it.category == FactCategory.DURATION) &&
                    it.polarity == FactPolarity.AFFIRMATIVE
        }

    val negatedSymptoms: List<UserFact>
        get() = facts.filter { it.polarity == FactPolarity.NEGATED }

    fun canAskAnotherQuestion(): Boolean = questionCountForCurrentTopic < 3
}
