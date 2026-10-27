package com.cbgm.sparrow.feature.conversationorchestration.incoming.domain.workflow.operation

import com.cbgm.sparrow.core.base.messagepart.domain.model.Poll
import com.cbgm.sparrow.core.base.messagepart.domain.model.PollPolicy
import com.cbgm.sparrow.feature.attachments.domain.repository.MessageAttachmentOperationsRepository
import com.cbgm.sparrow.feature.conversationorchestration.engine.machine.Transition
import com.cbgm.sparrow.feature.conversationorchestration.engine.machine.WorkflowStateMachine
import com.cbgm.sparrow.feature.conversationorchestration.engine.machine.WorkflowStep
import com.cbgm.sparrow.feature.conversationorchestration.incoming.domain.command.IncomingCommand
import com.cbgm.sparrow.feature.conversationorchestration.incoming.domain.error.rejectInvalid
import com.cbgm.sparrow.feature.conversationorchestration.incoming.domain.model.OperationConversation
import com.cbgm.sparrow.feature.conversationorchestration.incoming.domain.repository.ParkedMessageOperationRepository
import com.cbgm.sparrow.feature.conversationorchestration.incoming.domain.usecase.AuthorizeRemoteMessageOperationUseCase
import com.cbgm.sparrow.feature.conversationorchestration.message.domain.model.StoredMessage
import com.cbgm.sparrow.feature.conversationorchestration.message.domain.repository.DirectMessageRepository
import com.cbgm.sparrow.feature.conversationorchestration.message.domain.repository.GroupMessageRepository
import com.cbgm.sparrow.feature.conversationorchestration.message.domain.repository.MessageReactionRepository
import com.cbgm.sparrow.feature.conversationorchestration.message.domain.usecase.DiscardDirectMessagesUseCase
import com.cbgm.sparrow.feature.conversationorchestration.message.domain.usecase.DiscardGroupMessagesUseCase
import com.cbgm.sparrow.feature.conversationorchestration.message.domain.usecase.FindGroupPollUseCase
import com.cbgm.sparrow.protocol.message.MessageOperation
import kotlin.reflect.KClass

/**
 * FindTarget, then by operation:
 * Edit:      CheckEditable -> ReplaceText
 * Delete:    CheckSender -> DeleteMessage
 * Reaction:  SaveReaction | RemoveReaction
 * PollVote:  FindPoll -> SavePoll
 * PollClose: CheckPollCloser -> FindPoll -> SavePoll
 * No target: Park
 *
 * [AuthorizeRemoteMessageOperationUseCase] decides who may change which message. An operation whose
 * message has not arrived yet, for example an edit that overtook it after a reconnect, is parked and applied
 * once the message is stored ([ApplyParkedMessageOperationsWorkflow]).
 */
class ApplyRemoteMessageOperationWorkflow internal constructor(
    private val authorizeOperation: AuthorizeRemoteMessageOperationUseCase,
    private val directMessageRepository: DirectMessageRepository,
    private val groupMessageRepository: GroupMessageRepository,
    private val attachmentRepository: MessageAttachmentOperationsRepository,
    private val reactionRepository: MessageReactionRepository,
    private val discardDirectMessages: DiscardDirectMessagesUseCase,
    private val discardGroupMessages: DiscardGroupMessagesUseCase,
    private val findGroupPoll: FindGroupPollUseCase,
    private val parkedOperations: ParkedMessageOperationRepository
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
                authorizeOperation.requireEditable(step.target, command).getOrThrow()
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
                authorizeOperation.requireDeletable(step.target, command).getOrThrow()
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
                authorizeOperation.requirePollCloser(step.target, command).getOrThrow()
                Outcome.PollCloserAllowed
            }
            is Step.FindPoll -> Outcome.PollFound(findGroupPoll(command.operation.messageId, step.pollId).getOrThrow())
            is Step.SavePoll -> {
                attachmentRepository.updateMessagePart(
                    command.operation.messageId,
                    rejectInvalid { updatedPoll(step.poll, command) }
                ).getOrThrow()
                Outcome.Applied
            }
            is Step.Park -> {
                parkedOperations.park(command).getOrThrow()
                Outcome.Parked
            }
        }
    }

    override fun transition(step: Step, outcome: Outcome): Transition<Step> =
        when (outcome) {
            Outcome.TargetMissing -> Transition.Next(Step.Park(step.command))
            is Outcome.TargetFound -> nextForOperation(outcome.target, step.command)
            is Outcome.Editable -> Transition.Next(Step.ReplaceText(outcome.edit, step.command))
            is Outcome.SenderOwnsTarget -> Transition.Next(Step.DeleteMessage(outcome.target, step.command))
            Outcome.PollCloserAllowed -> Transition.Next(Step.FindPoll(step.command.pollId(), step.command))
            is Outcome.PollFound -> Transition.Next(Step.SavePoll(outcome.poll, step.command))
            Outcome.Applied -> Transition.Complete()
            Outcome.Parked -> Transition.Complete()
        }

    private suspend fun findTarget(command: IncomingCommand.ApplyRemoteMessageOperation): Outcome =
        authorizeOperation.findTarget(command).getOrThrow()?.let(Outcome::TargetFound) ?: Outcome.TargetMissing

    private fun nextForOperation(
        target: StoredMessage,
        command: IncomingCommand.ApplyRemoteMessageOperation
    ): Transition<Step> =
        when (val operation = command.operation) {
            is MessageOperation.Edit -> Transition.Next(Step.CheckEditable(target, operation, command))
            is MessageOperation.Delete -> Transition.Next(Step.CheckSender(target, command))
            is MessageOperation.Reaction -> Transition.Next(reactionStep(operation, command))
            is MessageOperation.PollVote -> Transition.Next(Step.FindPoll(operation.pollId, command))
            is MessageOperation.PollClose -> Transition.Next(Step.CheckPollCloser(target, command))
        }

    private fun reactionStep(
        reaction: MessageOperation.Reaction,
        command: IncomingCommand.ApplyRemoteMessageOperation
    ): Step =
        if (reaction.removed) {
            Step.RemoveReaction(reaction, command)
        } else {
            Step.SaveReaction(reaction, command)
        }

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

        /** The message has not arrived yet; the operation waits for it. */
        data class Park(
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

        data object Parked : Outcome
    }
}
