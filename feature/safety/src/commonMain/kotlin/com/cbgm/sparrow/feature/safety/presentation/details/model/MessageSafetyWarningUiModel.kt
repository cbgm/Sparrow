package com.cbgm.sparrow.feature.safety.presentation.details.model

data class MessageSafetyWarningUiModel(
    val level: MessageSafetyWarningLevel,
    val reasons: List<MessageSafetyWarningReason>
)
