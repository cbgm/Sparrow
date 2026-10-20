package com.cbgm.sparrow.feature.conversationorchestration.incoming.domain.workflow.operation

import com.cbgm.sparrow.core.base.messagepart.domain.model.Poll
import com.cbgm.sparrow.core.base.messagepart.domain.model.PollPolicy
import com.cbgm.sparrow.feature.attachments.domain.repository.MessageAttachmentOperationsRepository
import com.cbgm.sparrow.feature.conversationorchestration.engine.machine.Transition
import com.cbgm.sparrow.feature.conversationorchestration.engine.machine.WorkflowStateMachine
import com.cbgm.sparrow.feature.conversationorchestration.engine.machine.WorkflowStep
import com.cbgm.sparrow.feature.conversationorchestration.incoming.domain.command.IncomingCommand
import com.cbgm.sparrow.feature.conversationorchestration.incoming.domain.model.OperationConversation
import com.cbgm.sparrow.feature.conversationorchestration.incoming.domain.usecase.FindOperationTargetUseCase
import com.cbgm.sparrow.feature.conversationorchestration.message.domain.model.StoredMessage
import com.cbgm.sparrow.feature.conversationorchestration.message.domain.repository.DirectMessageRepository
import com.cbgm.sparrow.feature.conversationorchestration.message.domain.repository.GroupMessageRepository
import com.cbgm.sparrow.feature.conversationorchestration.message.domain.repository.MessageReactionRepository
import com.cbgm.sparrow.feature.conversationorchestration.message.domain.usecase.DiscardDirectMessagesUseCase
import com.cbgm.sparrow.feature.conversationorchestration.message.domain.usecase.DiscardGroupMessagesUseCase
import com.cbgm.sparrow.feature.conversationorchestration.message.domain.usecase.FindGroupPollUseCase
import com.cbgm.sparrow.feature.conversationorchestration.message.domain.usecase.RequireMessageWithoutPaymentUseCase
import com.cbgm.sparrow.feature.membership.domain.repository.GroupMembershipRepository
import com.cbgm.sparrow.protocol.message.MessageOperation
import kotlinx.coroutines.flow.first
import kotlin.reflect.KClass

/**
 * FindTarget, then by operation:
 * Edit:      CheckEditable -> ReplaceText
 * Delete:    CheckSender -> DeleteMessage
 * Reaction:  SaveReaction | RemoveReaction
 * PollVote:  FindPoll -> SavePoll
 * PollClose: CheckPollCloser -> FindPoll -> SavePoll
 *
 * Only the original sender edits or deletes; group operations only touch user messages. A missing direct
 * target or reaction target is ignored, a missing group target is an error.
 */
class ApplyRemoteMessageOperationWorkflow internal constructor(
    private val findOperationTarget: FindOperationTargetUseCase,
    private val directMessageRepository: DirectMessageRepository,
    private val groupMessageRepository: GroupMessageRepository,
    private val attachmentRepository: MessageAttachmentOperationsRepository,
    private val reactionRepository: MessageReactionRepository,
    private val discardDirectMessages: DiscardDirectMessagesUseCase,
    private val discardGroupMessages: DiscardGroupMessagesUseCase,
    private val findGroupPoll: FindGroupPollUseCase,
    private val membershipRepository: GroupMembershipRepository,
    private val requireMessageWithoutPayment: RequireMessageWithoutPaymentUseCase
) : WorkflowStateMachine<
        IncomingCommand.ApplyRemoteMessageOperation,
        ApplyRemoteMessageOperationWorkflow.Step,
        ApplyRemoteMessageOperationWorkflow.Outcome
    > {
    override val commandType: KClass<IncomingCommand.ApplyRemoteMessageOperation> =
        IncomingCommand.ApplyRemoteMessageOperation::class

    override fun firstStep(command: IncomingCommand.ApplyRemoteMessageOperation): Step = Step.FindTarget(command)

    override suspend fun perform(step: Step): Outcome {
        val command = step.command
        return when (step) {
            is Step.FindTarget -> findTarget(command)
            is Step.CheckEditable -> {
                requireSenderOwns(step.target, command)
                val text = messageTextOf(step.target, command)
                check(!text.isNullOrBlank()) { "Only text messages can be edited" }
                check(attachmentRepository.messageParts(step.target.id).getOrThrow().isEmpty()) {
                    "Messages with attachments cannot be edited"
                }
                Outcome.Editable(step.edit)
            }
            is Step.ReplaceText -> {
                val edit = step.edit
                when (command.conversation) {
                    OperationConversation.DIRECT ->
                        directMessageRepository.replaceText(edit.messageId, edit.text.trim())
                    OperationConversation.GROUP -> groupMessageRepository.replaceText(edit.messageId, edit.text.trim())
                }.getOrThrow()
                Outcome.Applied
            }
            is Step.CheckSender -> {
                requireMessageWithoutPayment(step.target.id).getOrThrow()
                requireSenderOwns(step.target, command)
                Outcome.SenderOwnsTarget(step.target)
            }
            is Step.DeleteMessage -> {
                when (command.conversation) {
                    OperationConversation.DIRECT -> discardDirectMessages(listOf(step.target))
                    OperationConversation.GROUP -> discardGroupMessages(listOf(step.target))
                }.getOrThrow()
                Outcome.Applied
            }
            is Step.SaveReaction -> {
                val reaction = step.reaction
                reactionRepository.save(
                    reaction.messageId,
                    command.conversationId,
                    command.senderContactId,
                    reaction.emoji
                ).getOrThrow()
                Outcome.Applied
            }
            is Step.RemoveReaction -> {
                val reaction = step.reaction
                reactionRepository.delete(reaction.messageId, command.senderContactId, reaction.emoji).getOrThrow()
                Outcome.Applied
            }
            is Step.CheckPollCloser -> {
                val administration = membershipRepository.observeAdministration(command.conversationId).first()
                check(
                    step.target.senderContactId == command.senderContactId || command.senderContactId in administration.adminContactIds
                ) {
                    "Only the poll creator or a group admin can close the poll"
                }
                Outcome.PollCloserAllowed
            }
            is Step.FindPoll -> Outcome.PollFound(findGroupPoll(command.operation.messageId, step.pollId).getOrThrow())
            is Step.SavePoll -> {
                attachmentRepository.updateMessagePart(
                    command.operation.messageId,
                    updatedPoll(step.poll, command)
                ).getOrThrow()
                Outcome.Applied
            }
        }
    }

    override fun transition(step: Step, outcome: Outcome): Transition<Step> =
        when (outcome) {
            Outcome.TargetMissing -> Transition.Complete()
            is Outcome.TargetFound -> nextForOperation(outcome.target, step.command)
            is Outcome.Editable -> Transition.Next(Step.ReplaceText(outcome.edit, step.command))
            is Outcome.SenderOwnsTarget -> Transition.Next(Step.DeleteMessage(outcome.target, step.command))
            Outcome.PollCloserAllowed -> Transition.Next(Step.FindPoll(step.command.pollId(), step.command))
            is Outcome.PollFound -> Transition.Next(Step.SavePoll(outcome.poll, step.command))
            Outcome.Applied -> Transition.Complete()
        }

    private suspend fun findTarget(command: IncomingCommand.ApplyRemoteMessageOperation): Outcome {
        val operation = command.operation
        if (operation is MessageOperation.PollVote || operation is MessageOperation.PollClose) {
            check(command.conversation == OperationConversation.GROUP) {
                "Poll operations are only supported in groups"
            }
        }
        val target = findOperationTarget(command.conversation, operation.messageId).getOrThrow()
        if (target == null) {
            val ignoreMissing =
                command.conversation == OperationConversation.DIRECT || operation is MessageOperation.Reaction
            check(ignoreMissing) { "Message was not found" }
            return Outcome.TargetMissing
        }
        check(target.conversationId == command.conversationId) { "Operation target belongs to another conversation" }
        if (command.conversation == OperationConversation.GROUP && operation !is MessageOperation.Reaction) {
            check(target.isGroupUserMessage) { "Operation target is not a user message" }
        }
        return Outcome.TargetFound(target)
    }

    private fun nextForOperation(
        target: StoredMessage,
        command: IncomingCommand.ApplyRemoteMessageOperation
    ): Transition<Step> =
        when (val operation = command.operation) {
            is MessageOperation.Edit -> Transition.Next(Step.CheckEditable(target, operation, command))
            is MessageOperation.Delete -> Transition.Next(Step.CheckSender(target, command))
            is MessageOperation.Reaction ->
                Transition.Next(
                    if (operation.removed) {
                        Step.RemoveReaction(operation, command)
                    } else {
                        Step.SaveReaction(
                            operation,
                            command
                        )
                    }
                )
            is MessageOperation.PollVote -> Transition.Next(Step.FindPoll(operation.pollId, command))
            is MessageOperation.PollClose -> Transition.Next(Step.CheckPollCloser(target, command))
        }

    private fun requireSenderOwns(target: StoredMessage, command: IncomingCommand.ApplyRemoteMessageOperation) {
        check(!target.isMine && target.senderContactId == command.senderContactId) {
            "Only the original sender can change a message"
        }
    }

    private suspend fun messageTextOf(
        target: StoredMessage,
        command: IncomingCommand.ApplyRemoteMessageOperation
    ): String? =
        when (command.conversation) {
            OperationConversation.DIRECT -> directMessageRepository.findText(target.id)
            OperationConversation.GROUP -> groupMessageRepository.findText(target.id)
        }.getOrThrow()

    private fun updatedPoll(poll: Poll, command: IncomingCommand.ApplyRemoteMessageOperation): Poll =
        when (val operation = command.operation) {
            is MessageOperation.PollVote ->
                PollPolicy.vote(
                    poll = poll,
                    voterId = command.senderContactId,
                    selectedOptionIds = operation.selectedOptionIds,
                    nowEpochMilliseconds = command.sentAtEpochMilliseconds
                )
            is MessageOperation.PollClose -> PollPolicy.close(poll, operation.closedAtEpochMilliseconds)
            else -> error("Operation does not change a poll")
        }

    private fun IncomingCommand.ApplyRemoteMessageOperation.pollId(): String =
        when (val operation = operation) {
            is MessageOperation.PollVote -> operation.pollId
            is MessageOperation.PollClose -> operation.pollId
            else -> error("Operation does not address a poll")
        }

    sealed interface Step : WorkflowStep {
        val command: IncomingCommand.ApplyRemoteMessageOperation

        data class FindTarget(
            override val command: IncomingCommand.ApplyRemoteMessageOperation
        ) : Step

        data class CheckEditable(
            val target: StoredMessage,
            val edit: MessageOperation.Edit,
            override val command: IncomingCommand.ApplyRemoteMessageOperation
        ) : Step

        data class ReplaceText(
            val edit: MessageOperation.Edit,
            override val command: IncomingCommand.ApplyRemoteMessageOperation
        ) : Step

        data class CheckSender(
            val target: StoredMessage,
            override val command: IncomingCommand.ApplyRemoteMessageOperation
        ) : Step

        data class DeleteMessage(
            val target: StoredMessage,
            override val command: IncomingCommand.ApplyRemoteMessageOperation
        ) : Step

        data class SaveReaction(
            val reaction: MessageOperation.Reaction,
            override val command: IncomingCommand.ApplyRemoteMessageOperation
        ) : Step

        data class RemoveReaction(
            val reaction: MessageOperation.Reaction,
            override val command: IncomingCommand.ApplyRemoteMessageOperation
        ) : Step

        data class CheckPollCloser(
            val target: StoredMessage,
            override val command: IncomingCommand.ApplyRemoteMessageOperation
        ) : Step

        data class FindPoll(
            val pollId: String,
            override val command: IncomingCommand.ApplyRemoteMessageOperation
        ) : Step

        data class SavePoll(
            val poll: Poll,
            override val command: IncomingCommand.ApplyRemoteMessageOperation
        ) : Step
    }

    sealed interface Outcome {
        data object TargetMissing : Outcome

        data class TargetFound(
            val target: StoredMessage
        ) : Outcome

        data class Editable(
            val edit: MessageOperation.Edit
        ) : Outcome

        data class SenderOwnsTarget(
            val target: StoredMessage
        ) : Outcome

        data object PollCloserAllowed : Outcome

        data class PollFound(
            val poll: Poll
        ) : Outcome

        data object Applied : Outcome
    }
}
