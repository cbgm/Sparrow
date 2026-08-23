package com.cbgm.sparrow.feature.safety.domain.model

data class MessageSafetyAssessment(
    val reasons: Set<MessageSafetyReason>
) {
    companion object {
        val Safe = MessageSafetyAssessment(reasons = emptySet())
    }
}
