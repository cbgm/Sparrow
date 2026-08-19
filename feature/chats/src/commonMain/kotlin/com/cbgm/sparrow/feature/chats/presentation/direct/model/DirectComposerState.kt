package com.cbgm.sparrow.feature.chats.presentation.direct.model

enum class DirectComposerState(
    val isInputEnabled: Boolean,
    val isSendActionEnabled: Boolean,
    val sendsTypingIndicators: Boolean
) {
    READY(
        isInputEnabled = true,
        isSendActionEnabled = true,
        sendsTypingIndicators = true
    ),
    REINVITE_REQUIRED(
        isInputEnabled = true,
        isSendActionEnabled = true,
        sendsTypingIndicators = false
    ),
    REINVITE_PENDING(
        isInputEnabled = true,
        isSendActionEnabled = true,
        sendsTypingIndicators = false
    ),
    DISABLED(
        isInputEnabled = false,
        isSendActionEnabled = false,
        sendsTypingIndicators = false
    )
}
