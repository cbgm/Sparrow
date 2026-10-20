package com.cbgm.sparrow.feature.conversationorchestration.message.domain.workflow.group

import com.cbgm.sparrow.core.util.time.SystemClock
import com.cbgm.sparrow.feature.conversationorchestration.api.command.GroupMessageCommand
import com.cbgm.sparrow.feature.conversationorchestration.api.model.ConversationFailure
import com.cbgm.sparrow.feature.conversationorchestration.api.model.ConversationFailureReason
import com.cbgm.sparrow.feature.conversationorchestration.engine.machine.ConversationWorkflow
import com.cbgm.sparrow.feature.conversationorchestration.engine.machine.Transition
import com.cbgm.sparrow.feature.conversationorchestration.engine.machine.WorkflowStep
import com.cbgm.sparrow.feature.conversationorchestration.message.domain.model.GroupMessageAccess
import com.cbgm.sparrow.feature.conversationorchestration.message.domain.model.StoredMessage
import com.cbgm.sparrow.feature.conversationorchestration.message.domain.usecase.CheckGroupMessageAccessUseCase
import com.cbgm.sparrow.feature.conversationorchestration.message.domain.usecase.DiscardGroupMessagesUseCase
import com.cbgm.sparrow.feature.conversationorchestration.message.domain.usecase.EncryptGroupOperationUseCase
import com.cbgm.sparrow.feature.conversationorchestration.message.domain.usecase.EnqueueGroupPacketsUseCase
import com.cbgm.sparrow.feature.conversationorchestration.message.domain.usecase.FindGroupMessageUseCase
import com.cbgm.sparrow.feature.conversationorchestration.message.domain.usecase.FindGroupRecipientsUseCase
import com.cbgm.sparrow.feature.conversationorchestration.message.domain.usecase.RequireMessageWithoutPaymentUseCase
import com.cbgm.sparrow.protocol.message.MessageOperation
import com.cbgm.sparrow.protocol.packet.GroupChatMessagePacket
import kotlin.reflect.KClass

/** CheckMembership -> FindOwnMessage -> FindRecipients -> EncryptDelete -> EnqueueDelete -> DiscardMessage. */
class DeleteGroupMessageWorkflow internal constructor(
    private val checkGroupMessageAccess: CheckGroupMessageAccessUseCase,
    private val findGroupMessage: FindGroupMessageUseCase,
    private val findGroupRecipients: FindGroupRecipientsUseCase,
    private val encryptGroupOperation: EncryptGroupOperationUseCase,
    private val enqueueGroupPackets: EnqueueGroupPacketsUseCase,
    private val discardGroupMessages: DiscardGroupMessagesUseCase,
    private val requireMessageWithoutPayment: RequireMessageWithoutPaymentUseCase
) : ConversationWorkflow<
        GroupMessageCommand.DeleteMessage,
        DeleteGroupMessageWorkflow.Step,
        DeleteGroupMessageWorkflow.Outcome
    > {
    override val commandType: KClass<GroupMessageCommand.DeleteMessage> = GroupMessageCommand.DeleteMessage::class

    override fun firstStep(command: GroupMessageCommand.DeleteMessage): Step = Step.CheckMembership(command)

    override suspend fun perform(step: Step): Outcome =
        when (step) {
            is Step.CheckMembership -> {
                require(step.command.messageId.isNotBlank()) { "Message ID must not be blank" }
                when (val access = checkGroupMessageAccess(step.command.groupId).getOrThrow()) {
                    GroupMessageAccess.Granted -> Outcome.MembershipActive
                    is GroupMessageAccess.Denied -> Outcome.MembershipInactive(access.reason)
                }
            }
            is Step.FindOwnMessage -> {
                val message = findGroupMessage(step.command.groupId, step.command.messageId).getOrThrow()
                check(message.isGroupUserMessage) { "Only user messages can be deleted" }
                requireMessageWithoutPayment(message.id).getOrThrow()
                check(message.isMine) { "Only your own messages can be deleted for everyone" }
                Outcome.OwnMessageFound(message)
            }
            is Step.FindRecipients ->
                Outcome.RecipientsFound(step.message, findGroupRecipients(step.command.groupId).getOrThrow())
            is Step.EncryptDelete ->
                Outcome.DeleteEncrypted(
                    message = step.message,
                    packets = encryptGroupOperation(
                        groupId = step.command.groupId,
                        operation = MessageOperation.Delete(
                            messageId = step.message.id,
                            deletedAtEpochMilliseconds = SystemClock.nowEpochMilliseconds()
                        ),
                        recipients = step.recipients
                    ).getOrThrow()
                )
            is Step.EnqueueDelete -> {
                enqueueGroupPackets(step.packets, recordFailedRecipients = false).getOrThrow()
                Outcome.DeleteEnqueued(step.message)
            }
            is Step.DiscardMessage -> {
                discardGroupMessages(listOf(step.message)).getOrThrow()
                Outcome.MessageDiscarded
            }
        }

    override fun transition(step: Step, outcome: Outcome): Transition<Step> =
        when (outcome) {
            Outcome.MembershipActive -> Transition.Next(Step.FindOwnMessage(step.command))
            is Outcome.MembershipInactive ->
                Transition.Reject(ConversationFailure(ConversationFailureReason.NOT_ALLOWED, outcome.reason))
            is Outcome.OwnMessageFound -> Transition.Next(Step.FindRecipients(outcome.message, step.command))
            is Outcome.RecipientsFound ->
                Transition.Next(Step.EncryptDelete(outcome.message, outcome.recipients, step.command))
            is Outcome.DeleteEncrypted ->
                Transition.Next(Step.EnqueueDelete(outcome.message, outcome.packets, step.command))
            is Outcome.DeleteEnqueued -> Transition.Next(Step.DiscardMessage(outcome.message, step.command))
            Outcome.MessageDiscarded -> Transition.Complete()
        }

    sealed interface Step : WorkflowStep {
        val command: GroupMessageCommand.DeleteMessage

        data class CheckMembership(
            override val command: GroupMessageCommand.DeleteMessage
        ) : Step

        data class FindOwnMessage(
            override val command: GroupMessageCommand.DeleteMessage
        ) : Step

        data class FindRecipients(
            val message: StoredMessage,
            override val command: GroupMessageCommand.DeleteMessage
        ) : Step

        data class EncryptDelete(
            val message: StoredMessage,
            val recipients: List<String>,
            override val command: GroupMessageCommand.DeleteMessage
        ) : Step

        data class EnqueueDelete(
            val message: StoredMessage,
            val packets: Map<String, GroupChatMessagePacket>,
            override val command: GroupMessageCommand.DeleteMessage
        ) : Step

        data class DiscardMessage(
            val message: StoredMessage,
            override val command: GroupMessageCommand.DeleteMessage
        ) : Step
    }

    sealed interface Outcome {
        data object MembershipActive : Outcome

        data class MembershipInactive(
            val reason: String
        ) : Outcome

        data class OwnMessageFound(
            val message: StoredMessage
        ) : Outcome

        data class RecipientsFound(
            val message: StoredMessage,
            val recipients: List<String>
        ) : Outcome

        data class DeleteEncrypted(
            val message: StoredMessage,
            val packets: Map<String, GroupChatMessagePacket>
        ) : Outcome

        data class DeleteEnqueued(
            val message: StoredMessage
        ) : Outcome

        data object MessageDiscarded : Outcome
    }
}
