package com.cbgm.sparrow.feature.conversationorchestration.message.domain.workflow.group

import com.cbgm.sparrow.feature.conversationorchestration.api.command.GroupMessageCommand
import com.cbgm.sparrow.feature.conversationorchestration.api.model.ConversationFailure
import com.cbgm.sparrow.feature.conversationorchestration.api.model.ConversationFailureReason
import com.cbgm.sparrow.feature.conversationorchestration.engine.machine.ConversationWorkflow
import com.cbgm.sparrow.feature.conversationorchestration.engine.machine.Transition
import com.cbgm.sparrow.feature.conversationorchestration.engine.machine.WorkflowStep
import com.cbgm.sparrow.feature.conversationorchestration.message.domain.model.GroupMessageAccess
import com.cbgm.sparrow.feature.conversationorchestration.message.domain.usecase.CheckGroupMessageAccessUseCase
import com.cbgm.sparrow.feature.conversationorchestration.message.domain.usecase.EncryptGroupOperationUseCase
import com.cbgm.sparrow.feature.conversationorchestration.message.domain.usecase.EnqueueGroupPacketsUseCase
import com.cbgm.sparrow.feature.conversationorchestration.message.domain.usecase.FindGroupMessageUseCase
import com.cbgm.sparrow.feature.conversationorchestration.message.domain.usecase.FindGroupRecipientsUseCase
import com.cbgm.sparrow.feature.conversationorchestration.message.domain.usecase.ToggleLocalReactionUseCase
import com.cbgm.sparrow.protocol.message.MessageOperation
import com.cbgm.sparrow.protocol.packet.GroupChatMessagePacket
import kotlin.reflect.KClass

/**
 * CheckMembership -> FindMessage -> FindRecipients -> ToggleReaction -> EncryptReaction -> EnqueueReaction.
 */
class ToggleGroupReactionWorkflow internal constructor(
    private val toggleLocalReaction: ToggleLocalReactionUseCase,
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
            is Step.ToggleReaction ->
                Outcome.ReactionChanged(
                    recipients = step.recipients,
                    removed = toggleLocalReaction(
                        messageId = step.command.messageId,
                        conversationId = step.command.groupId,
                        emoji = step.command.emoji
                    ).getOrThrow()
                )
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
            is Outcome.RecipientsFound -> Transition.Next(Step.ToggleReaction(outcome.recipients, step.command))
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

        data class ToggleReaction(
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
