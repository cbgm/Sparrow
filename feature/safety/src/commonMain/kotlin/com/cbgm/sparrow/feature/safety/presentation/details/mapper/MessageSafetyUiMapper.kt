package com.cbgm.sparrow.feature.safety.presentation.details.mapper

import com.cbgm.sparrow.feature.safety.domain.model.MessageSafetyAssessment
import com.cbgm.sparrow.feature.safety.domain.model.MessageSafetyReason
import com.cbgm.sparrow.feature.safety.presentation.details.model.MessageSafetyWarningLevel
import com.cbgm.sparrow.feature.safety.presentation.details.model.MessageSafetyWarningReason
import com.cbgm.sparrow.feature.safety.presentation.details.model.MessageSafetyWarningUi

fun MessageSafetyAssessment.toMessageSafetyWarningUi(): MessageSafetyWarningUi? {
    if (reasons.isEmpty()) return null

    return MessageSafetyWarningUi(
        level = warningLevel(),
        reasons = reasons.map(MessageSafetyReason::toMessageSafetyWarningReason)
    )
}

private fun MessageSafetyAssessment.warningLevel(): MessageSafetyWarningLevel =
    if (MessageSafetyReason.PRIVATE_KEY_REQUEST in reasons) {
        MessageSafetyWarningLevel.HIGH
    } else {
        MessageSafetyWarningLevel.SUSPICIOUS
    }

private fun MessageSafetyReason.toMessageSafetyWarningReason(): MessageSafetyWarningReason =
    when (this) {
        MessageSafetyReason.SUSPICIOUS_LINK -> MessageSafetyWarningReason.SUSPICIOUS_LINK
        MessageSafetyReason.LOOKALIKE_DOMAIN -> MessageSafetyWarningReason.LOOKALIKE_DOMAIN
        MessageSafetyReason.MIXED_SCRIPT_DOMAIN -> MessageSafetyWarningReason.MIXED_SCRIPT_DOMAIN
        MessageSafetyReason.IP_ADDRESS_LINK -> MessageSafetyWarningReason.IP_ADDRESS_LINK
        MessageSafetyReason.URL_SHORTENER -> MessageSafetyWarningReason.URL_SHORTENER
        MessageSafetyReason.URGENT_ACTION_REQUEST -> MessageSafetyWarningReason.URGENT_ACTION_REQUEST
        MessageSafetyReason.CREDENTIAL_REQUEST -> MessageSafetyWarningReason.CREDENTIAL_REQUEST
        MessageSafetyReason.PAYMENT_REQUEST -> MessageSafetyWarningReason.PAYMENT_REQUEST
        MessageSafetyReason.PRIVATE_KEY_REQUEST -> MessageSafetyWarningReason.PRIVATE_KEY_REQUEST
    }
