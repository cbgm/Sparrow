package com.cbgm.sparrow.feature.conversationorchestration.incoming.domain.workflow.membership

import com.cbgm.sparrow.core.util.logging.SparrowLog
import com.cbgm.sparrow.feature.conversationorchestration.api.command.GroupAdministrationCommand
import com.cbgm.sparrow.feature.conversationorchestration.engine.machine.Transition
import com.cbgm.sparrow.feature.conversationorchestration.engine.machine.WorkflowStateMachine
import com.cbgm.sparrow.feature.conversationorchestration.engine.machine.WorkflowStep
import com.cbgm.sparrow.feature.conversationorchestration.engine.runner.WorkflowEngine
import com.cbgm.sparrow.feature.conversationorchestration.incoming.domain.command.IncomingCommand
import com.cbgm.sparrow.feature.conversationorchestration.membership.domain.model.GroupParticipantActivation
import com.cbgm.sparrow.feature.conversationorchestration.membership.domain.repository.GroupConversationRepository
import com.cbgm.sparrow.feature.conversationorchestration.membership.domain.usecase.FindMemberDisplayNameUseCase
import com.cbgm.sparrow.feature.conversationorchestration.membership.domain.usecase.GetTrustedGroupAuthorityUseCase
import com.cbgm.sparrow.feature.conversationorchestration.membership.domain.usecase.ResolveSigningIdentityContactUseCase
import com.cbgm.sparrow.feature.conversationorchestration.message.domain.command.MessageCommand
import com.cbgm.sparrow.feature.identity.domain.model.RemotePeerIdentity
import com.cbgm.sparrow.feature.membership.domain.usecase.ApplyIncomingGroupActivationUseCase
import com.cbgm.sparrow.feature.membership.domain.usecase.AuthorizeIncomingGroupActivationUseCase
import com.cbgm.sparrow.protocol.packet.GroupMemberActivatedPacket
import kotlin.reflect.KClass

/**
 * LoadOwner -> AuthorizeActivation -> [ResolveMemberContact] -> ApplyActivation, then
 * local member:     SynchronizeVerification
 * DISCOVERY round:  ResolveMemberName -> RecordMemberAdded           (history only, no routing yet)
 * FINAL round:      ResolveMemberName -> ActivateParticipant -> FlushQueuedMessages
 */
class ReceiveGroupMemberActivationWorkflow internal constructor(
    private val engine: WorkflowEngine,
    private val getTrustedGroupAuthority: GetTrustedGroupAuthorityUseCase,
    private val authorizeIncomingGroupActivation: AuthorizeIncomingGroupActivationUseCase,
    private val resolveSigningIdentityContact: ResolveSigningIdentityContactUseCase,
    private val applyIncomingGroupActivation: ApplyIncomingGroupActivationUseCase,
    private val findMemberDisplayName: FindMemberDisplayNameUseCase,
    private val conversationRepository: GroupConversationRepository
) : WorkflowStateMachine<
        IncomingCommand.ReceiveGroupMemberActivation,
        ReceiveGroupMemberActivationWorkflow.Step,
        ReceiveGroupMemberActivationWorkflow.Outcome
    > {
    private val logger = SparrowLog.withTag("ReceiveGroupMemberActivationWorkflow")

    override val commandType: KClass<IncomingCommand.ReceiveGroupMemberActivation> =
        IncomingCommand.ReceiveGroupMemberActivation::class

    override fun firstStep(command: IncomingCommand.ReceiveGroupMemberActivation): Step = Step.LoadOwner(command)

    override suspend fun perform(step: Step): Outcome {
        val context = step.command.context
        val packet = step.command.packet
        return when (step) {
            is Step.LoadOwner ->
                Outcome.OwnerLoaded(getTrustedGroupAuthority.forActivation(context.contactId).getOrThrow())
            is Step.AuthorizeActivation -> authorizeActivation(step)
            is Step.ResolveMemberContact ->
                Outcome.MemberContactResolved(
                    step.owner,
                    resolveSigningIdentityContact(
                        signingPublicKey = packet.member.signingPublicKey,
                        encryptionPublicKey = packet.member.encryptionPublicKey,
                        phoneNumber = packet.member.phoneNumber
                    ).getOrThrow()
                )
            is Step.ApplyActivation -> applyActivation(step)
            is Step.SynchronizeVerification -> {
                engine.run(
                    GroupAdministrationCommand.SynchronizeVerification(packet.groupId)
                ).getOrThrow()
                Outcome.Done
            }
            is Step.ResolveMemberName ->
                Outcome.MemberNameResolved(step.activation(findMemberDisplayName(step.memberContactId).getOrThrow()))
            is Step.RecordMemberAdded -> {
                conversationRepository.recordRemoteMemberAdded(step.activation).getOrThrow()
                Outcome.Done
            }
            is Step.ActivateParticipant -> {
                conversationRepository.activateRemoteParticipant(step.activation).getOrThrow()
                Outcome.ParticipantActivated
            }
            is Step.FlushQueuedMessages -> {
                engine.run(MessageCommand.FlushQueuedGroupMessages(packet.groupId))
                    .onFailure { error ->
                        logger.warn(error) {
                            "Pending group messages remain queued for retry: groupId=${packet.groupId}"
                        }
                    }
                Outcome.Done
            }
        }
    }

    override fun transition(step: Step, outcome: Outcome): Transition<Step> =
        when (outcome) {
            is Outcome.OwnerLoaded -> Transition.Next(Step.AuthorizeActivation(outcome.owner, step.command))
            is Outcome.LocalMemberActivated ->
                Transition.Next(Step.ApplyActivation(outcome.owner, memberContactId = null, step.command))
            is Outcome.RemoteMemberActivated -> Transition.Next(Step.ResolveMemberContact(outcome.owner, step.command))
            is Outcome.MemberContactResolved ->
                Transition.Next(Step.ApplyActivation(outcome.owner, outcome.memberContactId, step.command))
            Outcome.AppliedForLocalMember -> Transition.Next(Step.SynchronizeVerification(step.command))
            is Outcome.AppliedForMember -> afterAppliedForMember(outcome.memberContactId, step.command)
            is Outcome.MemberNameResolved -> afterMemberNameResolved(outcome.activation, step.command)
            Outcome.ParticipantActivated -> Transition.Next(Step.FlushQueuedMessages(step.command))
            Outcome.Done -> Transition.Complete()
        }

    private suspend fun authorizeActivation(step: Step.AuthorizeActivation): Outcome {
        val context = step.command.context
        return if (
            authorizeIncomingGroupActivation(
                step.command.packet,
                context.contactId,
                step.owner.signingPublicKey,
                context.transportMode
            ).getOrThrow()
        ) {
            Outcome.LocalMemberActivated(step.owner)
        } else {
            Outcome.RemoteMemberActivated(step.owner)
        }
    }

    private suspend fun applyActivation(step: Step.ApplyActivation): Outcome {
        val context = step.command.context
        val appliedLocal =
            applyIncomingGroupActivation(
                packet = step.command.packet,
                ownerContactId = context.contactId,
                ownerSigningPublicKey = step.owner.signingPublicKey,
                transportMode = context.transportMode,
                memberContactId = step.memberContactId,
                receivedAtEpochMilliseconds = context.receivedAtEpochMilliseconds
            ).getOrThrow()
        return if (appliedLocal) {
            Outcome.AppliedForLocalMember
        } else {
            Outcome.AppliedForMember(requireNotNull(step.memberContactId))
        }
    }

    private fun afterAppliedForMember(
        memberContactId: String,
        command: IncomingCommand.ReceiveGroupMemberActivation
    ): Transition<Step> =
        when (command.packet.activationRound) {
            GroupMemberActivatedPacket.DISCOVERY_ROUND,
            GroupMemberActivatedPacket.FINAL_ROUND -> Transition.Next(Step.ResolveMemberName(memberContactId, command))
            else -> Transition.Complete()
        }

    private fun afterMemberNameResolved(
        activation: GroupParticipantActivation,
        command: IncomingCommand.ReceiveGroupMemberActivation
    ): Transition<Step> =
        if (command.packet.activationRound == GroupMemberActivatedPacket.DISCOVERY_ROUND) {
            Transition.Next(Step.RecordMemberAdded(activation, command))
        } else {
            Transition.Next(Step.ActivateParticipant(activation, command))
        }

    sealed interface Step : WorkflowStep {
        val command: IncomingCommand.ReceiveGroupMemberActivation

        data class LoadOwner(
            override val command: IncomingCommand.ReceiveGroupMemberActivation
        ) : Step

        data class AuthorizeActivation(
            val owner: RemotePeerIdentity,
            override val command: IncomingCommand.ReceiveGroupMemberActivation
        ) : Step

        data class ResolveMemberContact(
            val owner: RemotePeerIdentity,
            override val command: IncomingCommand.ReceiveGroupMemberActivation
        ) : Step

        /** [memberContactId] is null when the local member was activated. */
        data class ApplyActivation(
            val owner: RemotePeerIdentity,
            val memberContactId: String?,
            override val command: IncomingCommand.ReceiveGroupMemberActivation
        ) : Step

        data class SynchronizeVerification(
            override val command: IncomingCommand.ReceiveGroupMemberActivation
        ) : Step

        data class ResolveMemberName(
            val memberContactId: String,
            override val command: IncomingCommand.ReceiveGroupMemberActivation
        ) : Step {
            fun activation(memberDisplayName: String): GroupParticipantActivation =
                GroupParticipantActivation(
                    groupId = command.packet.groupId,
                    contactId = memberContactId,
                    role = command.packet.member.role,
                    epoch = command.packet.epoch,
                    activationId = command.packet.activationId,
                    memberDisplayName = memberDisplayName,
                    joinedAtEpochMilliseconds = command.packet.activatedAtEpochMilliseconds
                )
        }

        data class RecordMemberAdded(
            val activation: GroupParticipantActivation,
            override val command: IncomingCommand.ReceiveGroupMemberActivation
        ) : Step

        data class ActivateParticipant(
            val activation: GroupParticipantActivation,
            override val command: IncomingCommand.ReceiveGroupMemberActivation
        ) : Step

        data class FlushQueuedMessages(
            override val command: IncomingCommand.ReceiveGroupMemberActivation
        ) : Step
    }

    sealed interface Outcome {
        data class OwnerLoaded(
            val owner: RemotePeerIdentity
        ) : Outcome

        data class LocalMemberActivated(
            val owner: RemotePeerIdentity
        ) : Outcome

        data class RemoteMemberActivated(
            val owner: RemotePeerIdentity
        ) : Outcome

        data class MemberContactResolved(
            val owner: RemotePeerIdentity,
            val memberContactId: String
        ) : Outcome

        data object AppliedForLocalMember : Outcome

        data class AppliedForMember(
            val memberContactId: String
        ) : Outcome

        data class MemberNameResolved(
            val activation: GroupParticipantActivation
        ) : Outcome

        data object ParticipantActivated : Outcome

        data object Done : Outcome
    }
}
