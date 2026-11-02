package com.example.rag

enum class EvidenceStatus {
    RELEVANT,
    WEAK_OR_CONFLICTING,
    NONE,
    INDEX_UNAVAILABLE
}

data class EvidenceBundle(
    val passages: List<EvidencePassage> = emptyList(),
    val status: EvidenceStatus = EvidenceStatus.NONE,
    val retrievalQuery: String = "",
    val topScore: Float = 0f
) {
    val isUsable: Boolean
        get() = status == EvidenceStatus.RELEVANT && passages.isNotEmpty()

    fun formatForPrompt(maxPassages: Int = 3): String {
        if (passages.isEmpty() || status == EvidenceStatus.NONE || status == EvidenceStatus.INDEX_UNAVAILABLE) {
            return "[No verified offline medical evidence found for this query]"
        }

        val sb = StringBuilder()
        val selected = passages.take(maxPassages)
        for (p in selected) {
            sb.append("[Source: ").append(p.id).append(" - ").append(p.title).append("]\n")
            sb.append(p.text.trim()).append("\n\n")
        }
        return sb.toString().trim()
    }

    companion object {
        val EMPTY = EvidenceBundle(emptyList(), EvidenceStatus.NONE, "", 0f)
        val UNAVAILABLE = EvidenceBundle(emptyList(), EvidenceStatus.INDEX_UNAVAILABLE, "", 0f)
    }
}
