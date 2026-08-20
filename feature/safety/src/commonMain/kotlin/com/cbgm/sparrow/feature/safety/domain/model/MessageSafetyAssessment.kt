package com.cbgm.sparrow.feature.safety.domain.model

data class MessageSafetyAssessment(
    val risk: MessageSafetyRisk,
    val reasons: Set<MessageSafetyReason>
) {
    companion object {
        val Safe = MessageSafetyAssessment(
            risk = MessageSafetyRisk.NONE,
            reasons = emptySet()
        )
    }
}
