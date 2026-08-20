package com.cbgm.sparrow.feature.safety.presentation.model

data class MessageSafetyWarningUiModel(
    val level: MessageSafetyWarningLevel,
    val reasons: List<MessageSafetyWarningReason>
)
