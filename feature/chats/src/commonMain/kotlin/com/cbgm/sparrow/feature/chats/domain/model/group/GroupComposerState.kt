package com.cbgm.sparrow.feature.chats.domain.model.group

enum class GroupComposerState(
    val isInputEnabled: Boolean,
    val isSendActionEnabled: Boolean,
    val sendsTypingIndicators: Boolean
) {
    READY(
        isInputEnabled = true,
        isSendActionEnabled = true,
        sendsTypingIndicators = true
    ),
    QUEUEING(
        isInputEnabled = true,
        isSendActionEnabled = true,
        sendsTypingIndicators = true
    ),
    DISABLED(
        isInputEnabled = false,
        isSendActionEnabled = false,
        sendsTypingIndicators = false
    )
}

fun GroupConversation?.resolveComposerState(): GroupComposerState {
    this ?: return GroupComposerState.DISABLED

    return when {
        isReady -> GroupComposerState.READY
        !isIncomingInvitation && state.canQueueMessagesWhilePreparing() -> GroupComposerState.QUEUEING
        else -> GroupComposerState.DISABLED
    }
}

private fun GroupConversationState.canQueueMessagesWhilePreparing(): Boolean =
    this == GroupConversationState.WAITING_FOR_MEMBERS ||
        this == GroupConversationState.DISTRIBUTING_KEYS
