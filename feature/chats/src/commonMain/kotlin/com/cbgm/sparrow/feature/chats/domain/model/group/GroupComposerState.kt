package com.cbgm.sparrow.feature.chats.domain.model.group

import com.cbgm.sparrow.feature.membership.domain.model.GroupConversationState

enum class GroupComposerState(
    val isInputEnabled: Boolean,
    val isSendActionEnabled: Boolean,
    val sendsIndicators: Boolean
) {
    READY(
        isInputEnabled = true,
        isSendActionEnabled = true,
        sendsIndicators = true
    ),
    QUEUEING(
        isInputEnabled = true,
        isSendActionEnabled = true,
        sendsIndicators = true
    ),
    DISABLED(
        isInputEnabled = false,
        isSendActionEnabled = false,
        sendsIndicators = false
    )
}

fun GroupConversation?.resolveComposerState(): GroupComposerState {
    this ?: return GroupComposerState.DISABLED

    return when {
        isReady -> GroupComposerState.READY
        state.canQueueMessagesWhilePreparing() -> GroupComposerState.QUEUEING
        else -> GroupComposerState.DISABLED
    }
}

private fun GroupConversationState.canQueueMessagesWhilePreparing(): Boolean =
    this == GroupConversationState.WAITING_FOR_MEMBERS ||
        this == GroupConversationState.DISTRIBUTING_KEYS
