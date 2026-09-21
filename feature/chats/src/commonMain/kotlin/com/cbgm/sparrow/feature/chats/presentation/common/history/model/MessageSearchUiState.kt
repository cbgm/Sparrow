package com.cbgm.sparrow.feature.chats.presentation.common.history.model

internal data class MessageSearchTargetState(
    val highlightedMessageId: String?,
    val isHandled: Boolean
)

internal data class MessageJumpState(
    val highlightedMessageId: String?,
    val jumpTo: (String) -> Unit
)
