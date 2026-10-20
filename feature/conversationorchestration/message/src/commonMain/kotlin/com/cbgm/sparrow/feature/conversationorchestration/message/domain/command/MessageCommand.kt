package com.cbgm.sparrow.feature.conversationorchestration.message.domain.command

import com.cbgm.sparrow.feature.conversationorchestration.engine.machine.WorkflowCommand
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
     * Sends group messages that were written while no recipient was active, or interrupted on the way to
     * the outbox.
     */
    data class FlushQueuedGroupMessages(
        val groupId: String
    ) : MessageCommand
}
