package com.cbgm.sparrow.feature.chats.domain.model.direct

enum class DirectComposerState(
    val isInputEnabled: Boolean,
    val isSendActionEnabled: Boolean,
    val sendsIndicators: Boolean
) {
    READY(true, true, true),
    QUEUE_ALLOWED(true, true, false),
    DISABLED(false, false, false)
}

fun resolveDirectComposerState(
    hasConversation: Boolean,
    isChatAuthorized: Boolean,
    canQueueMessages: Boolean
): DirectComposerState =
    when {
        !hasConversation -> DirectComposerState.DISABLED
        isChatAuthorized -> DirectComposerState.READY
        canQueueMessages -> DirectComposerState.QUEUE_ALLOWED
        else -> DirectComposerState.DISABLED
    }
