package com.cbgm.sparrow.feature.safety.presentation.details.model

data class MessageSafetyDetailsUiState(
    val level: MessageSafetyWarningLevel = MessageSafetyWarningLevel.SUSPICIOUS,
    val reasons: List<MessageSafetyWarningReason> = emptyList(),
    val focusReason: MessageSafetyWarningReason? = null,
    val canBlockUser: Boolean = false,
    val isUserBlocked: Boolean = false,
    val isBlockingUser: Boolean = false,
    val blockError: String? = null
)
