package com.cbgm.sparrow.feature.safety.presentation.details.model

data class MessageSafetyWarningUi(
    val level: MessageSafetyWarningLevel,
    val reasons: List<MessageSafetyWarningReason>
)
