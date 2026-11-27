package com.cbgm.sparrow.feature.conversationorchestration.incoming.domain.workflow.membership

import com.cbgm.sparrow.core.util.logging.SparrowLog
import com.cbgm.sparrow.feature.conversationorchestration.engine.machine.Transition
import com.cbgm.sparrow.feature.conversationorchestration.engine.machine.WorkflowStateMachine
import com.cbgm.sparrow.feature.conversationorchestration.engine.machine.WorkflowStep
import com.cbgm.sparrow.feature.conversationorchestration.engine.runner.WorkflowEngine
import com.cbgm.sparrow.feature.conversationorchestration.groupstate.domain.command.GroupStateCommand
import com.cbgm.sparrow.feature.conversationorchestration.incoming.domain.command.IncomingCommand
import com.cbgm.sparrow.feature.conversationorchestration.membership.domain.command.MembershipCommand
import com.cbgm.sparrow.feature.conversationorchestration.membership.domain.model.GroupParticipantAddition
import com.cbgm.sparrow.feature.conversationorchestration.membership.domain.repository.GroupConversationRepository
import com.cbgm.sparrow.feature.conversationorchestration.membership.domain.usecase.FindMemberDisplayNameUseCase
import com.cbgm.sparrow.feature.conversationorchestration.message.domain.command.MessageCommand
import com.cbgm.sparrow.feature.membership.domain.model.MembershipHandshake
import com.cbgm.sparrow.feature.membership.domain.usecase.CheckReadyAcknowledgementEpochUseCase
import com.cbgm.sparrow.feature.membership.domain.usecase.FindReadyAcknowledgedMembershipUseCase
import com.cbgm.sparrow.feature.membership.domain.usecase.ReceiveGroupReadyAcknowledgementUseCase
import kotlin.reflect.KClass

/**
 * CheckEpochCurrent -> ReceiveAcknowledgement -> CheckEpochStillCurrent -> FindActivatedMembership
 * -> [ResolveMemberName -> AddParticipant -> FlushQueuedMessages] -> RefreshVerification -> SendCurrentMetadata
 *
 * The owner discards old epoch keys on rotation: a delayed acknowledgement of an older welcome is
 * obsolete and changes nothing. The participant is projected synchronously so group sends never wait
 * for an observer.
 */
@Suppress("LongParameterList")
class ReceiveGroupReadyAcknowledgementWorkflow internal constructor(
    private val engine: WorkflowEngine,
    private val checkReadyAcknowledgementEpoch: CheckReadyAcknowledgementEpochUseCase,
    private val receiveGroupReadyAcknowledgement: ReceiveGroupReadyAcknowledgementUseCase,
    private val findReadyAcknowledgedMembership: FindReadyAcknowledgedMembershipUseCase,
    private val findMemberDisplayName: FindMemberDisplayNameUseCase,
    private val conversationRepository: GroupConversationRepository
) : WorkflowStateMachine<
        IncomingCommand.ReceiveGroupReadyAcknowledgement,
        ReceiveGroupReadyAcknowledgementWorkflow.Step,
        ReceiveGroupReadyAcknowledgementWorkflow.Outcome
    > {
    private val logger = SparrowLog.withTag("ReceiveGroupReadyAcknowledgementWorkflow")

    override val commandType: KClass<IncomingCommand.ReceiveGroupReadyAcknowledgement> =
        IncomingCommand.ReceiveGroupReadyAcknowledgement::class

    override fun firstStep(command: IncomingCommand.ReceiveGroupReadyAcknowledgement): Step =
        Step.CheckEpochCurrent(command)

    override suspend fun perform(step: Step): Outcome {
        val context = step.command.context
        val packet = step.command.packet
        return when (step) {
            is Step.CheckEpochCurrent -> checkEpochCurrent(step.command)
            is Step.ReceiveAcknowledgement -> {
                receiveGroupReadyAcknowledgement(
                    context.contactId,
                    packet,
                    context.receivedAtEpochMilliseconds
                ).getOrThrow()
                Outcome.AcknowledgementReceived
            }
            is Step.CheckEpochStillCurrent -> checkEpochStillCurrent(step.command)
            is Step.FindActivatedMembership ->
                findReadyAcknowledgedMembership(context.contactId, packet).getOrThrow()
                    ?.let(Outcome::MembershipActivated)
                    ?: Outcome.NoActivatedMembership
            is Step.ResolveMemberName ->
                Outcome.MemberNameResolved(step.membership, findMemberDisplayName(context.contactId).getOrThrow())
            is Step.AddParticipant -> {
                conversationRepository.addParticipant(
                    GroupParticipantAddition(
                        groupId = packet.groupId,
                        contactId = context.contactId,
                        memberDisplayName = step.memberDisplayName,
                        epoch = packet.epoch,
                        // Membership activated the member then: the later of invitation and acknowledgement.
                        joinedAtEpochMilliseconds = step.membership.updatedAtEpochMilliseconds,
                        eventId = step.membership.sourceId
                    )
                ).getOrThrow()
                Outcome.ParticipantAdded
            }
            is Step.FlushQueuedMessages -> {
                engine.run(MessageCommand.FlushQueuedGroupMessages(packet.groupId))
                    .onFailure { error ->
                        logger.warn(error) {
                            "Pending group messages remain queued for retry: groupId=${packet.groupId}"
                        }
                    }
                Outcome.QueueFlushed
            }
            is Step.RefreshVerification -> {
                engine.run(MembershipCommand.RefreshOwnedGroupVerification(packet.groupId)).getOrThrow()
                Outcome.VerificationRefreshed(step.includeExpenseHistory)
            }
            is Step.SendCurrentMetadata -> {
                engine.run(
                    GroupStateCommand.SendCurrentGroupMetadata(
                        packet.groupId,
                        context.contactId,
                        includeExpenseHistory = step.includeExpenseHistory
                    )
                ).getOrThrow()
                Outcome.MetadataSent
            }
        }
    }

    override fun transition(step: Step, outcome: Outcome): Transition<Step> =
        when (outcome) {
            Outcome.Obsolete -> Transition.Complete()
            Outcome.EpochCurrent -> Transition.Next(Step.ReceiveAcknowledgement(step.command))
            Outcome.EpochStillCurrent -> Transition.Next(Step.FindActivatedMembership(step.command))
            Outcome.AcknowledgementReceived -> Transition.Next(Step.CheckEpochStillCurrent(step.command))
            is Outcome.MembershipActivated -> Transition.Next(Step.ResolveMemberName(outcome.membership, step.command))
            Outcome.NoActivatedMembership -> Transition.Next(Step.RefreshVerification(step.command, includeExpenseHistory = false))
            is Outcome.MemberNameResolved ->
                Transition.Next(Step.AddParticipant(outcome.membership, outcome.memberDisplayName, step.command))
            Outcome.ParticipantAdded -> Transition.Next(Step.FlushQueuedMessages(step.command))
            Outcome.QueueFlushed -> Transition.Next(Step.RefreshVerification(step.command, includeExpenseHistory = true))
            is Outcome.VerificationRefreshed ->
                Transition.Next(Step.SendCurrentMetadata(step.command, outcome.includeExpenseHistory))
            Outcome.MetadataSent -> Transition.Complete()
        }

    private suspend fun checkEpochCurrent(command: IncomingCommand.ReceiveGroupReadyAcknowledgement): Outcome =
        if (checkReadyAcknowledgementEpoch.isObsolete(command.packet).getOrThrow()) {
            Outcome.Obsolete
        } else {
            Outcome.EpochCurrent
        }

    private suspend fun checkEpochStillCurrent(command: IncomingCommand.ReceiveGroupReadyAcknowledgement): Outcome =
        if (checkReadyAcknowledgementEpoch.isStillCurrent(command.packet).getOrThrow()) {
            Outcome.EpochStillCurrent
        } else {
            Outcome.Obsolete
        }

    sealed interface Step : WorkflowStep {
        val command: IncomingCommand.ReceiveGroupReadyAcknowledgement

        data class CheckEpochCurrent(
            override val command: IncomingCommand.ReceiveGroupReadyAcknowledgement
        ) : Step

        data class ReceiveAcknowledgement(
            override val command: IncomingCommand.ReceiveGroupReadyAcknowledgement
        ) : Step

        data class CheckEpochStillCurrent(
            override val command: IncomingCommand.ReceiveGroupReadyAcknowledgement
        ) : Step

        data class FindActivatedMembership(
            override val command: IncomingCommand.ReceiveGroupReadyAcknowledgement
        ) : Step

        data class ResolveMemberName(
            val membership: MembershipHandshake,
            override val command: IncomingCommand.ReceiveGroupReadyAcknowledgement
        ) : Step

        data class AddParticipant(
            val membership: MembershipHandshake,
            val memberDisplayName: String,
            override val command: IncomingCommand.ReceiveGroupReadyAcknowledgement
        ) : Step

        data class FlushQueuedMessages(
            override val command: IncomingCommand.ReceiveGroupReadyAcknowledgement
        ) : Step

        data class RefreshVerification(
            override val command: IncomingCommand.ReceiveGroupReadyAcknowledgement,
            val includeExpenseHistory: Boolean
        ) : Step

        data class SendCurrentMetadata(
            override val command: IncomingCommand.ReceiveGroupReadyAcknowledgement,
            val includeExpenseHistory: Boolean
        ) : Step
    }

    sealed interface Outcome {
        data object EpochCurrent : Outcome

        /** The acknowledged epoch was not rotated while Membership processed it. */
        data object EpochStillCurrent : Outcome

        data object Obsolete : Outcome

        data object AcknowledgementReceived : Outcome

        data class MembershipActivated(
            val membership: MembershipHandshake
        ) : Outcome

        data object NoActivatedMembership : Outcome

        data class MemberNameResolved(
            val membership: MembershipHandshake,
            val memberDisplayName: String
        ) : Outcome

        data object ParticipantAdded : Outcome

        data object QueueFlushed : Outcome

        data class VerificationRefreshed(
            val includeExpenseHistory: Boolean
        ) : Outcome

        data object MetadataSent : Outcome
    }
}
