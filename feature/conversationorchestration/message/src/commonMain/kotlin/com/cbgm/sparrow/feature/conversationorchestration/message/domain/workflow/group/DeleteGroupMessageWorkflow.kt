package com.cbgm.sparrow.feature.conversationorchestration.message.domain.workflow.group

import com.cbgm.sparrow.core.util.time.SystemClock
import com.cbgm.sparrow.feature.conversationorchestration.api.command.GroupMessageCommand
import com.cbgm.sparrow.feature.conversationorchestration.api.model.ConversationFailure
import com.cbgm.sparrow.feature.conversationorchestration.api.model.ConversationFailureReason
import com.cbgm.sparrow.feature.conversationorchestration.engine.machine.ConversationWorkflow
import com.cbgm.sparrow.feature.conversationorchestration.engine.machine.Transition
import com.cbgm.sparrow.feature.conversationorchestration.engine.machine.WorkflowStep
import com.cbgm.sparrow.feature.conversationorchestration.engine.runner.WorkflowEngine
import com.cbgm.sparrow.feature.conversationorchestration.message.domain.command.MessageCommand
import com.cbgm.sparrow.feature.conversationorchestration.message.domain.model.GroupMessageAccess
import com.cbgm.sparrow.feature.conversationorchestration.message.domain.model.StoredMessage
import com.cbgm.sparrow.feature.conversationorchestration.message.domain.usecase.CheckGroupMessageAccessUseCase
import com.cbgm.sparrow.feature.conversationorchestration.message.domain.usecase.DiscardGroupMessagesUseCase
import com.cbgm.sparrow.feature.conversationorchestration.message.domain.usecase.EncryptGroupOperationUseCase
import com.cbgm.sparrow.feature.conversationorchestration.message.domain.usecase.EnqueueGroupPacketsUseCase
import com.cbgm.sparrow.feature.conversationorchestration.message.domain.usecase.FindDeletableGroupMessageUseCase
import com.cbgm.sparrow.feature.conversationorchestration.message.domain.usecase.FindGroupRecipientsUseCase
import com.cbgm.sparrow.protocol.message.MessageOperation
import com.cbgm.sparrow.protocol.packet.GroupChatMessagePacket
import kotlin.reflect.KClass

/**
 * CheckMembership -> FindOwnMessage -> FindRecipients -> (EncryptDelete -> EnqueueDelete -> DiscardMessage)
 * | DeleteUnsentMessage.
 *
 * A message nobody can receive or that is still queued unsent is deleted locally only, so it is never flushed.
 * Only that local delete ([MessageCommand.DeleteUnsentGroupMessage]) takes the group send lock, so a running flush
 * cannot send it while it is deleted. The delete of a message members already have takes no lock.
 */
class DeleteGroupMessageWorkflow internal constructor(
    private val engine: WorkflowEngine,
    private val checkGroupMessageAccess: CheckGroupMessageAccessUseCase,
    private val findDeletableGroupMessage: FindDeletableGroupMessageUseCase,
    private val findGroupRecipients: FindGroupRecipientsUseCase,
    private val encryptGroupOperation: EncryptGroupOperationUseCase,
    private val enqueueGroupPackets: EnqueueGroupPacketsUseCase,
    private val discardGroupMessages: DiscardGroupMessagesUseCase
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
            is Step.FindOwnMessage ->
                Outcome.OwnMessageFound(
                    findDeletableGroupMessage(step.command.groupId, step.command.messageId).getOrThrow()
                )
            is Step.FindRecipients ->
                Outcome.RecipientsFound(
                    step.message,
                    findGroupRecipients.forOwnMessage(step.command.groupId, step.message.id).getOrThrow()
                )
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
            is Step.DeleteUnsentMessage -> {
                engine.run(MessageCommand.DeleteUnsentGroupMessage(step.command.groupId, step.message.id))
                    .getOrThrow()
                Outcome.UnsentMessageDeleted
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
            is Outcome.RecipientsFound -> encryptOrDeleteLocally(outcome.message, outcome.recipients, step.command)
            is Outcome.DeleteEncrypted ->
                Transition.Next(Step.EnqueueDelete(outcome.message, outcome.packets, step.command))
            is Outcome.DeleteEnqueued -> Transition.Next(Step.DiscardMessage(outcome.message, step.command))
            Outcome.MessageDiscarded -> Transition.Complete()
            Outcome.UnsentMessageDeleted -> Transition.Complete()
        }

    /** Without a recipient the message is only removed locally, under the group send lock. */
    private fun encryptOrDeleteLocally(
        message: StoredMessage,
        recipients: List<String>,
        command: GroupMessageCommand.DeleteMessage
    ): Transition<Step> =
        if (recipients.isEmpty()) {
            Transition.Next(Step.DeleteUnsentMessage(message, command))
        } else {
            Transition.Next(Step.EncryptDelete(message, recipients, command))
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

        data class DeleteUnsentMessage(
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

        data object UnsentMessageDeleted : Outcome
    }
}
