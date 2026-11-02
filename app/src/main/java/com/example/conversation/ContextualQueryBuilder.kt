package com.example.conversation

class ContextualQueryBuilder {

    fun buildQuery(state: SessionState, currentUtterance: String): String {
        val cleanUtterance = currentUtterance.trim()
        val affirmativeSymptoms = state.activeAffirmativeSymptoms.map { it.value }

        // If user gave a short follow-up or answer to a pending question, merge with active symptoms
        if (cleanUtterance.split(Regex("\\s+")).size <= 4 && affirmativeSymptoms.isNotEmpty()) {
            val terms = mutableListOf<String>()
            terms.addAll(affirmativeSymptoms)
            if (!terms.contains(cleanUtterance.lowercase())) {
                terms.add(cleanUtterance)
            }
            return terms.distinct().joinToString(" ")
        }

        // If there are existing affirmative symptoms not explicitly in the current utterance, append them
        val terms = mutableListOf<String>()
        val utteranceLower = cleanUtterance.lowercase()

        for (sym in affirmativeSymptoms) {
            if (!utteranceLower.contains(sym.lowercase())) {
                terms.add(sym)
            }
        }

        terms.add(cleanUtterance)
        return terms.joinToString(" ").trim()
    }
}
