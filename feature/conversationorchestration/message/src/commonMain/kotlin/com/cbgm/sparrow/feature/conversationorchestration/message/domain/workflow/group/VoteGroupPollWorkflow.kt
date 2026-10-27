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
import com.cbgm.sparrow.feature.conversationorchestration.message.domain.usecase.FindGroupPollMessageUseCase
import com.cbgm.sparrow.feature.conversationorchestration.message.domain.usecase.FindGroupPollUseCase
import com.cbgm.sparrow.feature.conversationorchestration.message.domain.usecase.FindGroupRecipientsUseCase
import com.cbgm.sparrow.protocol.message.MessageOperation
import com.cbgm.sparrow.protocol.packet.GroupChatMessagePacket
import kotlin.reflect.KClass

/** CheckMembership -> FindPollMessage -> FindRecipients -> FindPoll -> SaveVote -> EncryptVote -> EnqueueVote. */
class VoteGroupPollWorkflow internal constructor(
    private val attachmentRepository: MessageAttachmentOperationsRepository,
    private val checkGroupMessageAccess: CheckGroupMessageAccessUseCase,
    private val findGroupPollMessage: FindGroupPollMessageUseCase,
    private val findGroupRecipients: FindGroupRecipientsUseCase,
    private val findGroupPoll: FindGroupPollUseCase,
    private val encryptGroupOperation: EncryptGroupOperationUseCase,
    private val enqueueGroupPackets: EnqueueGroupPacketsUseCase
) : ConversationWorkflow<GroupMessageCommand.VotePoll, VoteGroupPollWorkflow.Step, VoteGroupPollWorkflow.Outcome> {
    override val commandType: KClass<GroupMessageCommand.VotePoll> = GroupMessageCommand.VotePoll::class

    override fun firstStep(command: GroupMessageCommand.VotePoll): Step = Step.CheckMembership(command)

    override suspend fun perform(step: Step): Outcome =
        when (step) {
            is Step.CheckMembership ->
                when (val access = checkGroupMessageAccess(step.command.groupId).getOrThrow()) {
                    GroupMessageAccess.Granted -> Outcome.MembershipActive
                    is GroupMessageAccess.Denied -> Outcome.MembershipInactive(access.reason)
                }
            is Step.FindPollMessage -> {
                findGroupPollMessage(step.command.groupId, step.command.messageId).getOrThrow()
                Outcome.PollMessageFound
            }
            is Step.FindRecipients -> Outcome.RecipientsFound(findGroupRecipients(step.command.groupId).getOrThrow())
            is Step.FindPoll ->
                Outcome.PollFound(
                    findGroupPoll(step.command.messageId, step.command.pollId).getOrThrow(),
                    step.recipients
                )
            is Step.SaveVote -> {
                val voted =
                    PollPolicy.vote(
                        poll = step.poll,
                        voterId = PollPolicy.LOCAL_VOTER_ID,
                        selectedOptionIds = step.command.selectedOptionIds,
                        nowEpochMilliseconds = SystemClock.nowEpochMilliseconds()
                    )
                attachmentRepository.updateMessagePart(step.command.messageId, voted).getOrThrow()
                Outcome.VoteSaved(step.recipients)
            }
            is Step.EncryptVote ->
                Outcome.VoteEncrypted(
                    encryptGroupOperation(
                        groupId = step.command.groupId,
                        operation = MessageOperation.PollVote(
                            messageId = step.command.messageId,
                            pollId = step.command.pollId,
                            selectedOptionIds = step.command.selectedOptionIds
                        ),
                        recipients = step.recipients
                    ).getOrThrow()
                )
            is Step.EnqueueVote -> {
                enqueueGroupPackets(step.packets, recordFailedRecipients = false).getOrThrow()
                Outcome.VoteEnqueued
            }
        }

    override fun transition(step: Step, outcome: Outcome): Transition<Step> =
        when (outcome) {
            Outcome.MembershipActive -> Transition.Next(Step.FindPollMessage(step.command))
            is Outcome.MembershipInactive ->
                Transition.Reject(ConversationFailure(ConversationFailureReason.NOT_ALLOWED, outcome.reason))
            Outcome.PollMessageFound -> Transition.Next(Step.FindRecipients(step.command))
            is Outcome.RecipientsFound -> Transition.Next(Step.FindPoll(outcome.recipients, step.command))
            is Outcome.PollFound -> Transition.Next(Step.SaveVote(outcome.poll, outcome.recipients, step.command))
            is Outcome.VoteSaved -> Transition.Next(Step.EncryptVote(outcome.recipients, step.command))
            is Outcome.VoteEncrypted -> Transition.Next(Step.EnqueueVote(outcome.packets, step.command))
            Outcome.VoteEnqueued -> Transition.Complete()
        }

    sealed interface Step : WorkflowStep {
        val command: GroupMessageCommand.VotePoll

        data class CheckMembership(
            override val command: GroupMessageCommand.VotePoll
        ) : Step

        data class FindPollMessage(
            override val command: GroupMessageCommand.VotePoll
        ) : Step

        data class FindRecipients(
            override val command: GroupMessageCommand.VotePoll
        ) : Step

        data class FindPoll(
            val recipients: List<String>,
            override val command: GroupMessageCommand.VotePoll
        ) : Step

        data class SaveVote(
            val poll: Poll,
            val recipients: List<String>,
            override val command: GroupMessageCommand.VotePoll
        ) : Step

        data class EncryptVote(
            val recipients: List<String>,
            override val command: GroupMessageCommand.VotePoll
        ) : Step

        data class EnqueueVote(
            val packets: Map<String, GroupChatMessagePacket>,
            override val command: GroupMessageCommand.VotePoll
        ) : Step
    }

    sealed interface Outcome {
        data object MembershipActive : Outcome

        data class MembershipInactive(
            val reason: String
        ) : Outcome

        data object PollMessageFound : Outcome

        data class RecipientsFound(
            val recipients: List<String>
        ) : Outcome

        data class PollFound(
            val poll: Poll,
            val recipients: List<String>
        ) : Outcome

        data class VoteSaved(
            val recipients: List<String>
        ) : Outcome

        data class VoteEncrypted(
            val packets: Map<String, GroupChatMessagePacket>
        ) : Outcome

        data object VoteEnqueued : Outcome
    }
}
