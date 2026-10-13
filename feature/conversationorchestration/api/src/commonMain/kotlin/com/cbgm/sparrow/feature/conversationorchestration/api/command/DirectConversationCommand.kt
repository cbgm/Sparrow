package com.cbgm.sparrow.feature.conversationorchestration.api.command

import com.cbgm.sparrow.core.messagepart.domain.model.MessagePart
import com.cbgm.sparrow.feature.conversationorchestration.api.model.ConversationIndicator

sealed interface DirectConversationCommand : ConversationCommand {
    /** Opens an authorized chat, or starts an invitation when none is available. */
    data class OpenConversation(
        val contactId: String
    ) : DirectConversationCommand

    /** Explicitly requests a fresh invitation while keeping the existing chat. */
    data class ReconnectConversation(
        val contactId: String
    ) : DirectConversationCommand

    data class DeleteConversation(
        val conversationId: String
    ) : DirectConversationCommand

    data class SendMessage(
        val contactId: String,
        val parts: List<MessagePart>,
        val replyToMessageId: String? = null,
        val conversationId: String? = null
    ) : DirectConversationCommand

    data class EditMessage(
        val conversationId: String,
        val messageId: String,
        val text: String
    ) : DirectConversationCommand

    data class DeleteMessage(
        val conversationId: String,
        val messageId: String
    ) : DirectConversationCommand

    data class ToggleReaction(
        val conversationId: String,
        val messageId: String,
        val emoji: String
    ) : DirectConversationCommand

    data class RetryMessage(
        val messageId: String
    ) : DirectConversationCommand

    data class MarkConversationRead(
        val conversationId: String
    ) : DirectConversationCommand

    data class SendIndicator(
        val contactId: String,
        val indicator: ConversationIndicator
    ) : DirectConversationCommand
}
