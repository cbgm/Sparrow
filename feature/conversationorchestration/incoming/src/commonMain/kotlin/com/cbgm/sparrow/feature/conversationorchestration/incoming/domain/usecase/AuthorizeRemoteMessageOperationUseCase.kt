package com.cbgm.sparrow.feature.conversationorchestration.incoming.domain.usecase

import com.cbgm.sparrow.core.util.result.safeSuspendCall
import com.cbgm.sparrow.feature.conversationorchestration.incoming.domain.command.IncomingCommand
import com.cbgm.sparrow.feature.conversationorchestration.incoming.domain.error.rejectInvalid
import com.cbgm.sparrow.feature.conversationorchestration.incoming.domain.error.rejectUnless
import com.cbgm.sparrow.feature.conversationorchestration.incoming.domain.model.OperationConversation
import com.cbgm.sparrow.feature.conversationorchestration.message.domain.model.StoredMessage
import com.cbgm.sparrow.feature.conversationorchestration.message.domain.usecase.CheckMessageEditableUseCase
import com.cbgm.sparrow.feature.conversationorchestration.message.domain.usecase.CheckPollClosePermissionUseCase
import com.cbgm.sparrow.feature.conversationorchestration.message.domain.usecase.RequireMessageWithoutPaymentUseCase
import com.cbgm.sparrow.protocol.message.MessageOperation

/**
 * Who may apply a remote edit, delete or poll operation to which message. Only the original sender edits or
 * deletes, polls only exist in groups, and group operations other than reactions only touch user messages.
 * An operation that is not allowed fails with IncomingMessageRejectedException, so its envelope is dropped.
 */
class AuthorizeRemoteMessageOperationUseCase(
    private val findOperationTarget: FindOperationTargetUseCase,
    private val checkMessageEditable: CheckMessageEditableUseCase,
    private val requireMessageWithoutPayment: RequireMessageWithoutPaymentUseCase,
    private val checkPollClosePermission: CheckPollClosePermissionUseCase
) {
    /** The message [command] may change, or null when it has not arrived yet and the operation has to wait. */
    suspend fun findTarget(command: IncomingCommand.ApplyRemoteMessageOperation): Result<StoredMessage?> =
        safeSuspendCall(logFailure = false) {
            val operation = command.operation
            if (operation is MessageOperation.PollVote || operation is MessageOperation.PollClose) {
                rejectUnless(command.conversation == OperationConversation.GROUP) {
                    "Poll operations are only supported in groups"
                }
            }
            findOperationTarget(command.conversation, operation.messageId).getOrThrow()?.also { target ->
                requireInScope(target, command)
            }
        }

    suspend fun requireEditable(
        target: StoredMessage,
        command: IncomingCommand.ApplyRemoteMessageOperation
    ): Result<Unit> =
        safeSuspendCall(logFailure = false) {
            requireSenderOwns(target, command)
            val reason =
                when (command.conversation) {
                    OperationConversation.DIRECT -> checkMessageEditable.forDirectMessage(target.id)
                    OperationConversation.GROUP -> checkMessageEditable.forGroupMessage(target.id)
                }.getOrThrow()
            rejectUnless(reason == null) { reason.orEmpty() }
        }

    suspend fun requireDeletable(
        target: StoredMessage,
        command: IncomingCommand.ApplyRemoteMessageOperation
    ): Result<Unit> =
        safeSuspendCall(logFailure = false) {
            rejectInvalid { requireMessageWithoutPayment(target.id).getOrThrow() }
            requireSenderOwns(target, command)
        }

    suspend fun requirePollCloser(
        target: StoredMessage,
        command: IncomingCommand.ApplyRemoteMessageOperation
    ): Result<Unit> =
        safeSuspendCall(logFailure = false) {
            rejectUnless(
                checkPollClosePermission(command.conversationId, target, command.senderContactId).getOrThrow()
            ) {
                "Only the poll creator or a group admin can close the poll"
            }
        }

    private fun requireInScope(target: StoredMessage, command: IncomingCommand.ApplyRemoteMessageOperation) {
        rejectUnless(target.conversationId == command.conversationId) {
            "Operation target belongs to another conversation"
        }
        if (command.conversation == OperationConversation.GROUP && command.operation !is MessageOperation.Reaction) {
            rejectUnless(target.isGroupUserMessage) { "Operation target is not a user message" }
        }
    }

    private fun requireSenderOwns(target: StoredMessage, command: IncomingCommand.ApplyRemoteMessageOperation) {
        rejectUnless(!target.isMine && target.senderContactId == command.senderContactId) {
            "Only the original sender can change a message"
        }
    }
}
