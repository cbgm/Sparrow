package com.cbgm.sparrow.feature.conversationorchestration.api.model

sealed interface ConversationResult {
    data object Done : ConversationResult

    /** [conversationId] is null when an invitation was started instead of opening a chat. */
    data class ConversationOpened(
        val conversationId: String?
    ) : ConversationResult

    data object DirectMessageSent : ConversationResult

    /** The message waits until the contact authorized the conversation. */
    data class DirectMessageQueued(
        val authorizationFailure: ConversationFailure? = null
    ) : ConversationResult

    data class GroupMessageSent(
        val messageId: String
    ) : ConversationResult

    /**
     * The group exists. [failedContactIds] are the contacts whose invitation failed; they are not members and can
     * be added again.
     */
    data class GroupCreated(
        val groupId: String,
        val failedContactIds: List<String> = emptyList()
    ) : ConversationResult
}
