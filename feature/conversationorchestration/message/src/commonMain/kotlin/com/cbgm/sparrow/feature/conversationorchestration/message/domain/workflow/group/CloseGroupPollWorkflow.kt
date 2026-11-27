package com.cbgm.sparrow.feature.conversationorchestration.message.domain.workflow.group

import com.cbgm.sparrow.core.base.messagepart.domain.model.Poll
import com.cbgm.sparrow.core.util.time.SystemClock
import com.cbgm.sparrow.feature.attachments.domain.repository.MessageAttachmentOperationsRepository
import com.cbgm.sparrow.feature.conversationorchestration.api.command.GroupMessageCommand
import com.cbgm.sparrow.feature.conversationorchestration.api.model.ConversationFailure
import com.cbgm.sparrow.feature.conversationorchestration.api.model.ConversationFailureReason
import com.cbgm.sparrow.feature.conversationorchestration.engine.machine.ConversationWorkflow
import com.cbgm.sparrow.feature.conversationorchestration.engine.machine.Transition
import com.cbgm.sparrow.feature.conversationorchestration.engine.machine.WorkflowStep
import com.cbgm.sparrow.feature.conversationorchestration.message.domain.model.GroupMessageAccess
import com.cbgm.sparrow.feature.conversationorchestration.message.domain.model.StoredMessage
import com.cbgm.sparrow.feature.conversationorchestration.message.domain.usecase.CheckGroupMessageAccessUseCase
import com.cbgm.sparrow.feature.conversationorchestration.message.domain.usecase.CheckPollClosePermissionUseCase
import com.cbgm.sparrow.feature.conversationorchestration.message.domain.usecase.EncryptGroupOperationUseCase
import com.cbgm.sparrow.feature.conversationorchestration.message.domain.usecase.EnqueueGroupPacketsUseCase
import com.cbgm.sparrow.feature.conversationorchestration.message.domain.usecase.FindGroupPollMessageUseCase
import com.cbgm.sparrow.feature.conversationorchestration.message.domain.usecase.FindGroupPollUseCase
import com.cbgm.sparrow.feature.conversationorchestration.message.domain.usecase.FindGroupRecipientsUseCase
import com.cbgm.sparrow.protocol.message.MessageOperation
import com.cbgm.sparrow.protocol.packet.GroupChatMessagePacket
import kotlin.reflect.KClass

/**
 * CheckMembership -> FindPollMessage -> CheckClosePermission -> FindRecipients -> FindPoll -> EncryptClose
 * -> EnqueueClose -> SaveClosedPoll.
 *
 * Only the poll creator or a group admin may close a poll. The poll is closed locally only after the close is
 * queued: a failed send leaves it open, so a retry closes and sends again.
 */
class CloseGroupPollWorkflow internal constructor(
    private val attachmentRepository: MessageAttachmentOperationsRepository,
    private val checkGroupMessageAccess: CheckGroupMessageAccessUseCase,
    private val findGroupPollMessage: FindGroupPollMessageUseCase,
    private val checkPollClosePermission: CheckPollClosePermissionUseCase,
    private val findGroupRecipients: FindGroupRecipientsUseCase,
    private val findGroupPoll: FindGroupPollUseCase,
    private val encryptGroupOperation: EncryptGroupOperationUseCase,
    private val enqueueGroupPackets: EnqueueGroupPacketsUseCase
) : ConversationWorkflow<GroupMessageCommand.ClosePoll, CloseGroupPollWorkflow.Step, CloseGroupPollWorkflow.Outcome> {
    override val commandType: KClass<GroupMessageCommand.ClosePoll> = GroupMessageCommand.ClosePoll::class

    override fun firstStep(command: GroupMessageCommand.ClosePoll): Step =
        Step.CheckMembership(closedAtEpochMilliseconds = SystemClock.nowEpochMilliseconds(), command = command)

    override suspend fun perform(step: Step): Outcome =
        when (step) {
            is Step.CheckMembership ->
                when (val access = checkGroupMessageAccess(step.command.groupId).getOrThrow()) {
                    GroupMessageAccess.Granted -> Outcome.MembershipActive(step.closedAtEpochMilliseconds)
                    is GroupMessageAccess.Denied -> Outcome.MembershipInactive(access.reason)
                }
            is Step.FindPollMessage ->
                Outcome.PollMessageFound(
                    message = findGroupPollMessage(step.command.groupId, step.command.messageId).getOrThrow(),
                    closedAtEpochMilliseconds = step.closedAtEpochMilliseconds
                )
            is Step.CheckClosePermission ->
                closePermissionChecked(
                    allowed = checkPollClosePermission(
                        groupId = step.command.groupId,
                        pollMessage = step.message,
                        closerContactId = null
                    ).getOrThrow(),
                    closedAtEpochMilliseconds = step.closedAtEpochMilliseconds
                )
            is Step.FindRecipients ->
                Outcome.RecipientsFound(
                    findGroupRecipients(step.command.groupId).getOrThrow(),
                    step.closedAtEpochMilliseconds
                )
            is Step.FindPoll ->
                Outcome.PollFound(
                    closedPoll = findGroupPoll.closing(
                        messageId = step.command.messageId,
                        pollId = step.command.pollId,
                        closedAtEpochMilliseconds = step.closedAtEpochMilliseconds
                    ).getOrThrow(),
                    recipients = step.recipients,
                    closedAtEpochMilliseconds = step.closedAtEpochMilliseconds
                )
            is Step.EncryptClose ->
                Outcome.CloseEncrypted(
                    closedPoll = step.closedPoll,
                    packets = encryptGroupOperation(
                        groupId = step.command.groupId,
                        operation = MessageOperation.PollClose(
                            messageId = step.command.messageId,
                            pollId = step.command.pollId,
                            closedAtEpochMilliseconds = step.closedAtEpochMilliseconds
                        ),
                        recipients = step.recipients
                    ).getOrThrow()
                )
            is Step.EnqueueClose -> {
                enqueueGroupPackets(step.packets, recordFailedRecipients = false).getOrThrow()
                Outcome.CloseEnqueued(step.closedPoll)
            }
            is Step.SaveClosedPoll -> {
                attachmentRepository.updateMessagePart(step.command.messageId, step.closedPoll).getOrThrow()
                Outcome.ClosedPollSaved
            }
        }

    override fun transition(step: Step, outcome: Outcome): Transition<Step> =
        when (outcome) {
            is Outcome.MembershipActive ->
                Transition.Next(Step.FindPollMessage(outcome.closedAtEpochMilliseconds, step.command))
            is Outcome.PollMessageFound ->
                Transition.Next(
                    Step.CheckClosePermission(outcome.message, outcome.closedAtEpochMilliseconds, step.command)
                )
            is Outcome.MembershipInactive ->
                Transition.Reject(ConversationFailure(ConversationFailureReason.NOT_ALLOWED, outcome.reason))
            is Outcome.CloseAllowed ->
                Transition.Next(Step.FindRecipients(outcome.closedAtEpochMilliseconds, step.command))
            Outcome.CloseNotAllowed ->
                Transition.Reject(
                    ConversationFailure(
                        reason = ConversationFailureReason.NOT_ALLOWED,
                        message = "Only the poll creator or a group admin can close the poll"
                    )
                )
            is Outcome.RecipientsFound ->
                Transition.Next(Step.FindPoll(outcome.recipients, outcome.closedAtEpochMilliseconds, step.command))
            is Outcome.PollFound ->
                Transition.Next(
                    Step.EncryptClose(
                        outcome.closedPoll,
                        outcome.recipients,
                        outcome.closedAtEpochMilliseconds,
                        step.command
                    )
                )
            is Outcome.CloseEncrypted ->
                Transition.Next(Step.EnqueueClose(outcome.closedPoll, outcome.packets, step.command))
            is Outcome.CloseEnqueued -> Transition.Next(Step.SaveClosedPoll(outcome.closedPoll, step.command))
            Outcome.ClosedPollSaved -> Transition.Complete()
        }

    private fun closePermissionChecked(allowed: Boolean, closedAtEpochMilliseconds: Long): Outcome =
        if (allowed) Outcome.CloseAllowed(closedAtEpochMilliseconds) else Outcome.CloseNotAllowed

    sealed interface Step : WorkflowStep {
        val command: GroupMessageCommand.ClosePoll

        data class CheckMembership(
            val closedAtEpochMilliseconds: Long,
            override val command: GroupMessageCommand.ClosePoll
        ) : Step

        data class FindPollMessage(
            val closedAtEpochMilliseconds: Long,
            override val command: GroupMessageCommand.ClosePoll
        ) : Step

        data class CheckClosePermission(
            val message: StoredMessage,
            val closedAtEpochMilliseconds: Long,
            override val command: GroupMessageCommand.ClosePoll
        ) : Step

        data class FindRecipients(
            val closedAtEpochMilliseconds: Long,
            override val command: GroupMessageCommand.ClosePoll
        ) : Step

        data class FindPoll(
            val recipients: List<String>,
            val closedAtEpochMilliseconds: Long,
            override val command: GroupMessageCommand.ClosePoll
        ) : Step

        data class EncryptClose(
            val closedPoll: Poll,
            val recipients: List<String>,
            val closedAtEpochMilliseconds: Long,
            override val command: GroupMessageCommand.ClosePoll
        ) : Step

        data class EnqueueClose(
            val closedPoll: Poll,
            val packets: Map<String, GroupChatMessagePacket>,
            override val command: GroupMessageCommand.ClosePoll
        ) : Step

        data class SaveClosedPoll(
            val closedPoll: Poll,
            override val command: GroupMessageCommand.ClosePoll
        ) : Step
    }

    sealed interface Outcome {
        data class MembershipActive(
            val closedAtEpochMilliseconds: Long
        ) : Outcome

        data class MembershipInactive(
            val reason: String
        ) : Outcome

        data class PollMessageFound(
            val message: StoredMessage,
            val closedAtEpochMilliseconds: Long
        ) : Outcome

        data class CloseAllowed(
            val closedAtEpochMilliseconds: Long
        ) : Outcome

        data object CloseNotAllowed : Outcome

        data class RecipientsFound(
            val recipients: List<String>,
            val closedAtEpochMilliseconds: Long
        ) : Outcome

        data class PollFound(
            val closedPoll: Poll,
            val recipients: List<String>,
            val closedAtEpochMilliseconds: Long
        ) : Outcome

        data class CloseEncrypted(
            val closedPoll: Poll,
            val packets: Map<String, GroupChatMessagePacket>
        ) : Outcome

        data class CloseEnqueued(
            val closedPoll: Poll
        ) : Outcome

        data object ClosedPollSaved : Outcome
    }
}
