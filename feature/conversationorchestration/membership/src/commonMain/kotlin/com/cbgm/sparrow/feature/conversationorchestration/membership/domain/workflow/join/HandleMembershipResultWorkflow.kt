package com.cbgm.sparrow.feature.conversationorchestration.membership.domain.workflow.join

import com.cbgm.sparrow.core.util.logging.SparrowLog
import com.cbgm.sparrow.feature.conversationorchestration.engine.machine.Transition
import com.cbgm.sparrow.feature.conversationorchestration.engine.machine.WorkflowStateMachine
import com.cbgm.sparrow.feature.conversationorchestration.engine.machine.WorkflowStep
import com.cbgm.sparrow.feature.conversationorchestration.engine.runner.WorkflowEngine
import com.cbgm.sparrow.feature.conversationorchestration.membership.domain.command.MembershipCommand
import com.cbgm.sparrow.feature.conversationorchestration.membership.domain.model.GroupParticipantAddition
import com.cbgm.sparrow.feature.conversationorchestration.membership.domain.repository.GroupConversationRepository
import com.cbgm.sparrow.feature.conversationorchestration.membership.domain.usecase.ConfirmGroupMemberIdentityUseCase
import com.cbgm.sparrow.feature.conversationorchestration.membership.domain.usecase.FindMemberDisplayNameUseCase
import com.cbgm.sparrow.feature.conversationorchestration.membership.domain.usecase.LoadMutualMemberIdentityUseCase
import com.cbgm.sparrow.feature.conversationorchestration.message.domain.command.MessageCommand
import com.cbgm.sparrow.feature.identity.domain.model.RemotePeerIdentity
import com.cbgm.sparrow.feature.invite.domain.model.InvitationPayloadType
import com.cbgm.sparrow.feature.invite.domain.model.InvitationResponse
import com.cbgm.sparrow.feature.invite.domain.usecase.HandleInvitationResponseUseCase
import com.cbgm.sparrow.feature.membership.domain.model.MembershipPerspective
import com.cbgm.sparrow.feature.membership.domain.model.MembershipStatus
import com.cbgm.sparrow.feature.membership.domain.usecase.GetGroupCurrentEpochUseCase
import kotlin.reflect.KClass

/**
 * IDENTITY_READY: MarkInvitationAccepted -> LoadMemberIdentity -> ConfirmMemberIdentity
 * WELCOME_SENT:   MarkInvitationAccepted
 * ACTIVE:         ResolveMemberName -> LoadEpoch -> AddParticipant -> FlushQueuedMessages
 *
 * IDENTITY_READY recovers memberships left behind when the process stopped after the signed join request
 * but before the welcome.
 */
class HandleMembershipResultWorkflow internal constructor(
    private val engine: WorkflowEngine,
    private val handleInvitationResponse: HandleInvitationResponseUseCase,
    private val loadMutualMemberIdentity: LoadMutualMemberIdentityUseCase,
    private val confirmGroupMemberIdentity: ConfirmGroupMemberIdentityUseCase,
    private val findMemberDisplayName: FindMemberDisplayNameUseCase,
    private val getGroupCurrentEpoch: GetGroupCurrentEpochUseCase,
    private val conversationRepository: GroupConversationRepository
) : WorkflowStateMachine<
        MembershipCommand.HandleMembershipResult,
        HandleMembershipResultWorkflow.Step,
        HandleMembershipResultWorkflow.Outcome
    > {
    private val logger = SparrowLog.withTag("HandleMembershipResultWorkflow")

    override val commandType: KClass<MembershipCommand.HandleMembershipResult> =
        MembershipCommand.HandleMembershipResult::class

    override fun firstStep(command: MembershipCommand.HandleMembershipResult): Step {
        if (command.result.perspective != MembershipPerspective.OWNER) return Step.IgnoreResult(command)
        return when (command.result.status) {
            MembershipStatus.IDENTITY_READY,
            MembershipStatus.WELCOME_SENT -> Step.MarkInvitationAccepted(command)
            MembershipStatus.ACTIVE -> Step.ResolveMemberName(command)
            else -> Step.IgnoreResult(command)
        }
    }

    override suspend fun perform(step: Step): Outcome {
        val result = step.command.result
        return when (step) {
            is Step.MarkInvitationAccepted -> {
                handleInvitationResponse(
                    InvitationPayloadType.GROUP,
                    result.sourceInvitationId,
                    InvitationResponse.ACCEPTED
                )
                    .getOrThrow()
                Outcome.InvitationAccepted
            }
            is Step.LoadMemberIdentity ->
                Outcome.MemberIdentityLoaded(loadMutualMemberIdentity(result.peerId).getOrThrow())
            is Step.ConfirmMemberIdentity -> {
                confirmGroupMemberIdentity(
                    sourceId = result.sourceInvitationId,
                    groupId = result.groupId,
                    memberContactId = result.peerId,
                    memberEncryptionPublicKey = step.identity.encryptionPublicKey,
                    memberSigningPublicKey = step.identity.signingPublicKey,
                    updatedAtEpochMilliseconds = result.updatedAtEpochMilliseconds
                ).getOrThrow()
                Outcome.Done
            }
            is Step.ResolveMemberName -> Outcome.MemberNameResolved(findMemberDisplayName(result.peerId).getOrThrow())
            is Step.LoadEpoch ->
                Outcome.EpochLoaded(step.memberDisplayName, getGroupCurrentEpoch(result.groupId).getOrThrow())
            is Step.AddParticipant -> {
                conversationRepository.addParticipant(
                    GroupParticipantAddition(
                        groupId = result.groupId,
                        contactId = result.peerId,
                        memberDisplayName = step.memberDisplayName,
                        epoch = step.epoch,
                        joinedAtEpochMilliseconds = result.updatedAtEpochMilliseconds,
                        eventId = result.sourceInvitationId
                    )
                ).getOrThrow()
                Outcome.ParticipantAdded
            }
            is Step.FlushQueuedMessages -> {
                engine.run(MessageCommand.FlushQueuedGroupMessages(result.groupId))
                    .onFailure { error ->
                        logger.warn(error) {
                            "Pending group messages remain queued for retry: groupId=${result.groupId}"
                        }
                    }
                Outcome.Done
            }
            is Step.IgnoreResult -> Outcome.Done
        }
    }

    override fun transition(step: Step, outcome: Outcome): Transition<Step> =
        when (outcome) {
            Outcome.InvitationAccepted -> afterInvitationAccepted(step.command)
            is Outcome.MemberIdentityLoaded ->
                Transition.Next(Step.ConfirmMemberIdentity(outcome.identity, step.command))
            is Outcome.MemberNameResolved -> Transition.Next(Step.LoadEpoch(outcome.memberDisplayName, step.command))
            is Outcome.EpochLoaded ->
                Transition.Next(Step.AddParticipant(outcome.memberDisplayName, outcome.epoch, step.command))
            Outcome.ParticipantAdded -> Transition.Next(Step.FlushQueuedMessages(step.command))
            Outcome.Done -> Transition.Complete()
        }

    /** Only IDENTITY_READY still lacks the bound member identity; WELCOME_SENT is done. */
    private fun afterInvitationAccepted(command: MembershipCommand.HandleMembershipResult): Transition<Step> =
        if (command.result.status == MembershipStatus.IDENTITY_READY) {
            Transition.Next(Step.LoadMemberIdentity(command))
        } else {
            Transition.Complete()
        }

    sealed interface Step : WorkflowStep {
        val command: MembershipCommand.HandleMembershipResult

        data class MarkInvitationAccepted(
            override val command: MembershipCommand.HandleMembershipResult
        ) : Step

        data class LoadMemberIdentity(
            override val command: MembershipCommand.HandleMembershipResult
        ) : Step

        data class ConfirmMemberIdentity(
            val identity: RemotePeerIdentity,
            override val command: MembershipCommand.HandleMembershipResult
        ) : Step

        data class ResolveMemberName(
            override val command: MembershipCommand.HandleMembershipResult
        ) : Step

        data class LoadEpoch(
            val memberDisplayName: String,
            override val command: MembershipCommand.HandleMembershipResult
        ) : Step

        data class AddParticipant(
            val memberDisplayName: String,
            val epoch: Int,
            override val command: MembershipCommand.HandleMembershipResult
        ) : Step

        data class FlushQueuedMessages(
            override val command: MembershipCommand.HandleMembershipResult
        ) : Step

        /** Member-side results and statuses without an owner reaction. */
        data class IgnoreResult(
            override val command: MembershipCommand.HandleMembershipResult
        ) : Step
    }

    sealed interface Outcome {
        data object InvitationAccepted : Outcome

        data class MemberIdentityLoaded(
            val identity: RemotePeerIdentity
        ) : Outcome

        data class MemberNameResolved(
            val memberDisplayName: String
        ) : Outcome

        data class EpochLoaded(
            val memberDisplayName: String,
            val epoch: Int
        ) : Outcome

        data object ParticipantAdded : Outcome

        data object Done : Outcome
    }
}
