package com.cbgm.sparrow.feature.safety.domain.resolver

import com.cbgm.sparrow.feature.safety.domain.model.MessageSafetyAssessment
import com.cbgm.sparrow.feature.safety.domain.model.MessageSafetyReason
import com.cbgm.sparrow.feature.safety.domain.model.MessageSafetyRisk

class MessageSafetyRiskResolver {
    operator fun invoke(reasons: Set<MessageSafetyReason>): MessageSafetyAssessment {
        if (reasons.isEmpty()) return MessageSafetyAssessment.Safe

        val risk =
            when {
                MessageSafetyReason.PRIVATE_KEY_REQUEST in reasons -> MessageSafetyRisk.HIGH
                MessageSafetyReason.CREDENTIAL_REQUEST in reasons && MessageSafetyReason.SUSPICIOUS_LINK in reasons ->
                    MessageSafetyRisk.HIGH
                MessageSafetyReason.PAYMENT_REQUEST in reasons && MessageSafetyReason.URGENT_ACTION_REQUEST in reasons ->
                    MessageSafetyRisk.HIGH
                reasons.sumOf(::weight) >= HIGH_RISK_SCORE -> MessageSafetyRisk.HIGH
                reasons.sumOf(::weight) >= SUSPICIOUS_RISK_SCORE -> MessageSafetyRisk.SUSPICIOUS
                else -> MessageSafetyRisk.LOW
            }
        return MessageSafetyAssessment(risk = risk, reasons = reasons)
    }

    private fun weight(reason: MessageSafetyReason): Int =
        when (reason) {
            MessageSafetyReason.SUSPICIOUS_LINK -> 1
            MessageSafetyReason.LOOKALIKE_DOMAIN -> 3
            MessageSafetyReason.MIXED_SCRIPT_DOMAIN -> 3
            MessageSafetyReason.IP_ADDRESS_LINK -> 2
            MessageSafetyReason.URL_SHORTENER -> 1
            MessageSafetyReason.URGENT_ACTION_REQUEST -> 1
            MessageSafetyReason.CREDENTIAL_REQUEST -> 3
            MessageSafetyReason.PAYMENT_REQUEST -> 2
            MessageSafetyReason.PRIVATE_KEY_REQUEST -> 5
        }

    private companion object {
        const val HIGH_RISK_SCORE = 5
        const val SUSPICIOUS_RISK_SCORE = 2
    }
}
