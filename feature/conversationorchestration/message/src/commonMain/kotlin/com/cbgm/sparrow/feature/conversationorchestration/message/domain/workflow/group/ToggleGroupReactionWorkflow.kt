package com.cbgm.sparrow.feature.conversationorchestration.message.domain.workflow.group

import com.cbgm.sparrow.feature.conversationorchestration.api.command.GroupMessageCommand
import com.cbgm.sparrow.feature.conversationorchestration.api.model.ConversationFailure
import com.cbgm.sparrow.feature.conversationorchestration.api.model.ConversationFailureReason
import com.cbgm.sparrow.feature.conversationorchestration.engine.machine.ConversationWorkflow
import com.cbgm.sparrow.feature.conversationorchestration.engine.machine.Transition
import com.cbgm.sparrow.feature.conversationorchestration.engine.machine.WorkflowStep
import com.cbgm.sparrow.feature.conversationorchestration.message.domain.model.GroupMessageAccess
import com.cbgm.sparrow.feature.conversationorchestration.message.domain.repository.MessageReactionRepository
import com.cbgm.sparrow.feature.conversationorchestration.message.domain.usecase.CheckGroupMessageAccessUseCase
import com.cbgm.sparrow.feature.conversationorchestration.message.domain.usecase.EncryptGroupOperationUseCase
import com.cbgm.sparrow.feature.conversationorchestration.message.domain.usecase.EnqueueGroupPacketsUseCase
import com.cbgm.sparrow.feature.conversationorchestration.message.domain.usecase.FindGroupMessageUseCase
import com.cbgm.sparrow.feature.conversationorchestration.message.domain.usecase.FindGroupRecipientsUseCase
import com.cbgm.sparrow.protocol.message.MessageOperation
import com.cbgm.sparrow.protocol.packet.GroupChatMessagePacket
import kotlin.reflect.KClass

/**
 * CheckMembership -> FindMessage -> FindRecipients -> FindLocalReaction -> (AddReaction | RemoveReaction)
 * -> EncryptReaction -> EnqueueReaction.
 */
class ToggleGroupReactionWorkflow internal constructor(
    private val reactionRepository: MessageReactionRepository,
    private val checkGroupMessageAccess: CheckGroupMessageAccessUseCase,
    private val findGroupMessage: FindGroupMessageUseCase,
    private val findGroupRecipients: FindGroupRecipientsUseCase,
    private val encryptGroupOperation: EncryptGroupOperationUseCase,
    private val enqueueGroupPackets: EnqueueGroupPacketsUseCase
) : ConversationWorkflow<
        GroupMessageCommand.ToggleReaction,
        ToggleGroupReactionWorkflow.Step,
        ToggleGroupReactionWorkflow.Outcome
    > {
    override val commandType: KClass<GroupMessageCommand.ToggleReaction> = GroupMessageCommand.ToggleReaction::class

    override fun firstStep(command: GroupMessageCommand.ToggleReaction): Step = Step.CheckMembership(command)

    override suspend fun perform(step: Step): Outcome =
        when (step) {
            is Step.CheckMembership -> {
                require(step.command.messageId.isNotBlank()) { "Message ID must not be blank" }
                require(step.command.emoji.isNotBlank()) { "Reaction emoji must not be blank" }
                when (val access = checkGroupMessageAccess(step.command.groupId).getOrThrow()) {
                    GroupMessageAccess.Granted -> Outcome.MembershipActive
                    is GroupMessageAccess.Denied -> Outcome.MembershipInactive(access.reason)
                }
            }
            is Step.FindMessage -> {
                findGroupMessage(step.command.groupId, step.command.messageId).getOrThrow()
                Outcome.MessageFound
            }
            is Step.FindRecipients -> Outcome.RecipientsFound(findGroupRecipients(step.command.groupId).getOrThrow())
            is Step.FindLocalReaction ->
                if (reactionRepository.exists(step.command.messageId, null, step.command.emoji).getOrThrow()) {
                    Outcome.ReactionPresent(step.recipients)
                } else {
                    Outcome.ReactionAbsent(step.recipients)
                }
            is Step.RemoveReaction -> {
                reactionRepository.delete(step.command.messageId, null, step.command.emoji).getOrThrow()
                Outcome.ReactionChanged(step.recipients, removed = true)
            }
            is Step.AddReaction -> {
                reactionRepository.save(
                    step.command.messageId,
                    step.command.groupId,
                    null,
                    step.command.emoji
                ).getOrThrow()
                Outcome.ReactionChanged(step.recipients, removed = false)
            }
            is Step.EncryptReaction ->
                Outcome.ReactionEncrypted(
                    encryptGroupOperation(
                        groupId = step.command.groupId,
                        operation = MessageOperation.Reaction(
                            messageId = step.command.messageId,
                            emoji = step.command.emoji,
                            removed = step.removed
                        ),
                        recipients = step.recipients
                    ).getOrThrow()
                )
            is Step.EnqueueReaction -> {
                enqueueGroupPackets(step.packets, recordFailedRecipients = false).getOrThrow()
                Outcome.ReactionEnqueued
            }
        }

    override fun transition(step: Step, outcome: Outcome): Transition<Step> =
        when (outcome) {
            Outcome.MembershipActive -> Transition.Next(Step.FindMessage(step.command))
            is Outcome.MembershipInactive ->
                Transition.Reject(ConversationFailure(ConversationFailureReason.NOT_ALLOWED, outcome.reason))
            Outcome.MessageFound -> Transition.Next(Step.FindRecipients(step.command))
            is Outcome.RecipientsFound -> Transition.Next(Step.FindLocalReaction(outcome.recipients, step.command))
            is Outcome.ReactionPresent -> Transition.Next(Step.RemoveReaction(outcome.recipients, step.command))
            is Outcome.ReactionAbsent -> Transition.Next(Step.AddReaction(outcome.recipients, step.command))
            is Outcome.ReactionChanged ->
                Transition.Next(Step.EncryptReaction(outcome.recipients, outcome.removed, step.command))
            is Outcome.ReactionEncrypted -> Transition.Next(Step.EnqueueReaction(outcome.packets, step.command))
            Outcome.ReactionEnqueued -> Transition.Complete()
        }

    sealed interface Step : WorkflowStep {
        val command: GroupMessageCommand.ToggleReaction

        data class CheckMembership(
            override val command: GroupMessageCommand.ToggleReaction
        ) : Step

        data class FindMessage(
            override val command: GroupMessageCommand.ToggleReaction
        ) : Step

        data class FindRecipients(
            override val command: GroupMessageCommand.ToggleReaction
        ) : Step

        data class FindLocalReaction(
            val recipients: List<String>,
            override val command: GroupMessageCommand.ToggleReaction
        ) : Step

        data class RemoveReaction(
            val recipients: List<String>,
            override val command: GroupMessageCommand.ToggleReaction
        ) : Step

        data class AddReaction(
            val recipients: List<String>,
            override val command: GroupMessageCommand.ToggleReaction
        ) : Step

        data class EncryptReaction(
            val recipients: List<String>,
            val removed: Boolean,
            override val command: GroupMessageCommand.ToggleReaction
        ) : Step

        data class EnqueueReaction(
            val packets: Map<String, GroupChatMessagePacket>,
            override val command: GroupMessageCommand.ToggleReaction
        ) : Step
    }

    sealed interface Outcome {
        data object MembershipActive : Outcome

        data class MembershipInactive(
            val reason: String
        ) : Outcome

        data object MessageFound : Outcome

        data class RecipientsFound(
            val recipients: List<String>
        ) : Outcome

        data class ReactionPresent(
            val recipients: List<String>
        ) : Outcome

        data class ReactionAbsent(
            val recipients: List<String>
        ) : Outcome

        data class ReactionChanged(
            val recipients: List<String>,
            val removed: Boolean
        ) : Outcome

        data class ReactionEncrypted(
            val packets: Map<String, GroupChatMessagePacket>
        ) : Outcome

        data object ReactionEnqueued : Outcome
    }
}
