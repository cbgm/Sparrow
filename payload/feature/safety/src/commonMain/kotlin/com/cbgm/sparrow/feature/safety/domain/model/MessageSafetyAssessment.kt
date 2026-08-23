package com.cbgm.sparrow.feature.safety.domain.model

data class MessageSafetyAssessment(
    val reasons: Set<MessageSafetyReason>
) {
    val isSafe: Boolean
        get() = reasons.isEmpty()

    companion object {
        val Safe = MessageSafetyAssessment(reasons = emptySet())
    }
}
