package com.cbgm.sparrow.feature.conversationorchestration.message.domain.workflow.group

import com.cbgm.sparrow.core.base.messagepart.domain.model.Poll
import com.cbgm.sparrow.core.base.messagepart.domain.model.PollPolicy
import com.cbgm.sparrow.core.util.time.SystemClock
import com.cbgm.sparrow.feature.attachments.domain.repository.MessageAttachmentOperationsRepository
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
import com.cbgm.sparrow.feature.conversationorchestration.message.domain.usecase.FindGroupPollUseCase
import com.cbgm.sparrow.feature.conversationorchestration.message.domain.usecase.FindGroupRecipientsUseCase
import com.cbgm.sparrow.feature.membership.domain.usecase.ObserveGroupAdministrationUseCase
import com.cbgm.sparrow.protocol.message.MessageOperation
import com.cbgm.sparrow.protocol.packet.GroupChatMessagePacket
import kotlinx.coroutines.flow.first
import kotlin.reflect.KClass

/**
 * CheckMembership -> CheckClosePermission -> FindRecipients -> FindPoll -> SaveClosedPoll
 * -> EncryptClose -> EnqueueClose.
 *
 * Only the poll creator or a group admin may close a poll.
 */
class CloseGroupPollWorkflow internal constructor(
    private val attachmentRepository: MessageAttachmentOperationsRepository,
    private val observeGroupAdministration: ObserveGroupAdministrationUseCase,
    private val checkGroupMessageAccess: CheckGroupMessageAccessUseCase,
    private val findGroupMessage: FindGroupMessageUseCase,
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
            is Step.CheckClosePermission -> {
                val message = findGroupMessage(step.command.groupId, step.command.messageId).getOrThrow()
                check(message.isGroupUserMessage) { "Poll target is not a user message" }
                val isLocalAdmin = observeGroupAdministration(step.command.groupId).first().isLocalAdmin
                if (message.isMine || isLocalAdmin) {
                    Outcome.CloseAllowed(step.closedAtEpochMilliseconds)
                } else {
                    Outcome.CloseNotAllowed
                }
            }
            is Step.FindRecipients ->
                Outcome.RecipientsFound(
                    findGroupRecipients(step.command.groupId).getOrThrow(),
                    step.closedAtEpochMilliseconds
                )
            is Step.FindPoll ->
                Outcome.PollFound(
                    poll = findGroupPoll(step.command.messageId, step.command.pollId).getOrThrow(),
                    recipients = step.recipients,
                    closedAtEpochMilliseconds = step.closedAtEpochMilliseconds
                )
            is Step.SaveClosedPoll -> {
                attachmentRepository
                    .updateMessagePart(
                        step.command.messageId,
                        PollPolicy.close(step.poll, step.closedAtEpochMilliseconds)
                    )
                    .getOrThrow()
                Outcome.ClosedPollSaved(step.recipients, step.closedAtEpochMilliseconds)
            }
            is Step.EncryptClose ->
                Outcome.CloseEncrypted(
                    encryptGroupOperation(
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
                Outcome.CloseEnqueued
            }
        }

    override fun transition(step: Step, outcome: Outcome): Transition<Step> =
        when (outcome) {
            is Outcome.MembershipActive ->
                Transition.Next(Step.CheckClosePermission(outcome.closedAtEpochMilliseconds, step.command))
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
                    Step.SaveClosedPoll(
                        outcome.poll,
                        outcome.recipients,
                        outcome.closedAtEpochMilliseconds,
                        step.command
                    )
                )
            is Outcome.ClosedPollSaved ->
                Transition.Next(Step.EncryptClose(outcome.recipients, outcome.closedAtEpochMilliseconds, step.command))
            is Outcome.CloseEncrypted -> Transition.Next(Step.EnqueueClose(outcome.packets, step.command))
            Outcome.CloseEnqueued -> Transition.Complete()
        }

    sealed interface Step : WorkflowStep {
        val command: GroupMessageCommand.ClosePoll

        data class CheckMembership(
            val closedAtEpochMilliseconds: Long,
            override val command: GroupMessageCommand.ClosePoll
        ) : Step

        data class CheckClosePermission(
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

        data class SaveClosedPoll(
            val poll: Poll,
            val recipients: List<String>,
            val closedAtEpochMilliseconds: Long,
            override val command: GroupMessageCommand.ClosePoll
        ) : Step

        data class EncryptClose(
            val recipients: List<String>,
            val closedAtEpochMilliseconds: Long,
            override val command: GroupMessageCommand.ClosePoll
        ) : Step

        data class EnqueueClose(
            val packets: Map<String, GroupChatMessagePacket>,
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

        data class CloseAllowed(
            val closedAtEpochMilliseconds: Long
        ) : Outcome

        data object CloseNotAllowed : Outcome

        data class RecipientsFound(
            val recipients: List<String>,
            val closedAtEpochMilliseconds: Long
        ) : Outcome

        data class PollFound(
            val poll: Poll,
            val recipients: List<String>,
            val closedAtEpochMilliseconds: Long
        ) : Outcome

        data class ClosedPollSaved(
            val recipients: List<String>,
            val closedAtEpochMilliseconds: Long
        ) : Outcome

        data class CloseEncrypted(
            val packets: Map<String, GroupChatMessagePacket>
        ) : Outcome

        data object CloseEnqueued : Outcome
    }
}
