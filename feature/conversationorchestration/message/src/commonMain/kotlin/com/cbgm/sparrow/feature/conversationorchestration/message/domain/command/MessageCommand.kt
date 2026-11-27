package com.cbgm.sparrow.feature.conversationorchestration.message.domain.command

import com.cbgm.sparrow.feature.conversationorchestration.api.model.ConversationFailure
import com.cbgm.sparrow.feature.conversationorchestration.engine.machine.WorkflowCommand
import com.cbgm.sparrow.feature.conversationorchestration.message.domain.model.DirectConversationTarget
import com.cbgm.sparrow.feature.conversationorchestration.message.domain.model.OutgoingMessageContent

/**
 * Every message job orchestration starts internally, from a runtime observer or another workflow.
 * Submit one with `engine.run(command)`; the engine finds the workflow that handles it.
 */
sealed interface MessageCommand : WorkflowCommand {
    /** Sends validated content into an existing, authorized direct conversation. */
    data class DeliverDirectMessage(
        val conversationId: String,
        val content: OutgoingMessageContent,
        val replyToMessageId: String?
    ) : MessageCommand

    /**
     * Stores a message for a contact without direct authorization as waiting, under the contact's release lock, so a
     * release that runs meanwhile cannot miss it. Completes with
     * [com.cbgm.sparrow.feature.conversationorchestration.api.model.ConversationResult.DirectMessageQueued], or with
     * `Done` and nothing stored when the contact became authorized meanwhile and the message can be sent instead.
     */
    data class StoreWaitingDirectMessage(
        val contactId: String,
        val conversationId: String,
        val content: OutgoingMessageContent,
        val replyToMessageId: String?,
        val authorizationFailure: ConversationFailure?
    ) : MessageCommand

    /**
     * Edits an own direct message that was never sent, under the contact's release lock, so a waiting message is
     * never edited while it is released with its old text.
     */
    data class EditOwnDirectMessage(
        val target: DirectConversationTarget,
        val messageId: String,
        val text: String
    ) : MessageCommand

    /**
     * Deletes an own direct message that was never sent, under the contact's release lock, so a waiting message is
     * never removed while it is released.
     */
    data class DeleteOwnDirectMessage(
        val target: DirectConversationTarget,
        val messageId: String
    ) : MessageCommand

    /** Ensures the authorized contact has a visible chat and releases its waiting messages. */
    data class ActivateAuthorizedConversation(
        val contactId: String
    ) : MessageCommand

    /** The contact declined the invitation; messages written for it are dropped. */
    data class DiscardWaitingDirectMessages(
        val contactId: String
    ) : MessageCommand

    /** The contact authorized the conversation; messages written before may now be sent. */
    data class ReleaseWaitingDirectMessages(
        val contactId: String
    ) : MessageCommand

    /**
     * Edits an own group message nobody has received yet, under the group send lock, so a running flush never
     * sends the text from before the edit.
     */
    data class EditUnsentGroupMessage(
        val groupId: String,
        val messageId: String,
        val text: String
    ) : MessageCommand

    /**
     * Deletes an own group message nobody has received yet, under the group send lock, so a running flush never
     * sends it.
     */
    data class DeleteUnsentGroupMessage(
        val groupId: String,
        val messageId: String
    ) : MessageCommand

    /**
     * Sends group messages that were written while no recipient was active, or interrupted on the way to
     * the outbox.
     */
    data class FlushQueuedGroupMessages(
        val groupId: String
    ) : MessageCommand
}
