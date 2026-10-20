package com.cbgm.sparrow.feature.conversationorchestration.membership.domain.workflow.join

import com.cbgm.sparrow.core.util.time.SystemClock
import com.cbgm.sparrow.feature.conversationorchestration.engine.machine.Transition
import com.cbgm.sparrow.feature.conversationorchestration.engine.machine.WorkflowStateMachine
import com.cbgm.sparrow.feature.conversationorchestration.engine.machine.WorkflowStep
import com.cbgm.sparrow.feature.conversationorchestration.membership.domain.command.MembershipCommand
import com.cbgm.sparrow.feature.conversationorchestration.membership.domain.usecase.ConfirmGroupMemberIdentityUseCase
import com.cbgm.sparrow.feature.identity.domain.model.RemotePeerIdentity
import com.cbgm.sparrow.feature.identity.domain.usecase.EstablishMutualIdentityUseCase
import com.cbgm.sparrow.feature.identity.domain.usecase.GetRemoteIdentityUseCase
import com.cbgm.sparrow.feature.identity.domain.usecase.StageRemoteIdentityUseCase
import com.cbgm.sparrow.feature.membership.domain.model.MembershipHandshake
import com.cbgm.sparrow.feature.membership.domain.model.MembershipStatus
import com.cbgm.sparrow.feature.membership.domain.usecase.GetMembershipHandshakeUseCase
import kotlin.reflect.KClass

/**
 * FindHandshake -> LoadApprovedIdentity -> StageIdentity -> EstablishMutualIdentity -> ConfirmMemberIdentity
 *
 * Approval changed the pinned keys in place but reset mutual authorization: the original signed JOIN's key
 * receipt is marked again and no earlier verification is inherited. A join that already got its welcome
 * needs nothing.
 */
class ResumeApprovedGroupJoinWorkflow internal constructor(
    private val getMembershipHandshake: GetMembershipHandshakeUseCase,
    private val getRemoteIdentity: GetRemoteIdentityUseCase,
    private val stageRemoteIdentity: StageRemoteIdentityUseCase,
    private val establishMutualIdentity: EstablishMutualIdentityUseCase,
    private val confirmGroupMemberIdentity: ConfirmGroupMemberIdentityUseCase
) : WorkflowStateMachine<
        MembershipCommand.ResumeApprovedGroupJoin,
        ResumeApprovedGroupJoinWorkflow.Step,
        ResumeApprovedGroupJoinWorkflow.Outcome
    > {
    override val commandType: KClass<MembershipCommand.ResumeApprovedGroupJoin> =
        MembershipCommand.ResumeApprovedGroupJoin::class

    override fun firstStep(command: MembershipCommand.ResumeApprovedGroupJoin): Step = Step.FindHandshake(command)

    override suspend fun perform(step: Step): Outcome {
        val contactId = step.command.contactId
        return when (step) {
            is Step.FindHandshake -> {
                val handshake =
                    requireNotNull(getMembershipHandshake(step.command.sourceId).getOrThrow()) {
                        "Approved group join no longer has a membership"
                    }
                check(handshake.peerId == contactId && handshake.ownerSigningPublicKey == null) {
                    "Approved identity does not belong to the owner-side group join"
                }
                Outcome.HandshakeFound(handshake)
            }
            is Step.LoadApprovedIdentity ->
                Outcome.IdentityLoaded(
                    requireNotNull(getRemoteIdentity(contactId).getOrThrow()) { "Approved member identity is missing" },
                    step.groupId
                )
            is Step.StageIdentity -> {
                stageRemoteIdentity(
                    contactId,
                    step.identity.encryptionPublicKey,
                    step.identity.signingPublicKey
                ).getOrThrow()
                Outcome.IdentityStaged(step.identity, step.groupId)
            }
            is Step.EstablishMutualIdentity -> {
                establishMutualIdentity(
                    contactId,
                    step.identity.encryptionPublicKey,
                    step.identity.signingPublicKey
                ).getOrThrow()
                Outcome.MutualIdentityEstablished(step.identity, step.groupId)
            }
            is Step.ConfirmMemberIdentity -> {
                confirmGroupMemberIdentity(
                    sourceId = step.command.sourceId,
                    groupId = step.groupId,
                    memberContactId = contactId,
                    memberEncryptionPublicKey = step.identity.encryptionPublicKey,
                    memberSigningPublicKey = step.identity.signingPublicKey,
                    updatedAtEpochMilliseconds = SystemClock.nowEpochMilliseconds()
                ).getOrThrow()
                Outcome.MemberIdentityConfirmed
            }
        }
    }

    override fun transition(step: Step, outcome: Outcome): Transition<Step> =
        when (outcome) {
            is Outcome.HandshakeFound ->
                when (outcome.handshake.status) {
                    MembershipStatus.WELCOME_SENT,
                    MembershipStatus.WAITING_FOR_ACTIVATION,
                    MembershipStatus.ACTIVE -> Transition.Complete()
                    MembershipStatus.STAGED,
                    MembershipStatus.IDENTITY_READY ->
                        Transition.Next(Step.LoadApprovedIdentity(outcome.handshake.groupId, step.command))
                    else -> Transition.Reject.because(IllegalStateException("Approved group join is no longer pending"))
                }
            is Outcome.IdentityLoaded ->
                Transition.Next(Step.StageIdentity(outcome.identity, outcome.groupId, step.command))
            is Outcome.IdentityStaged ->
                Transition.Next(Step.EstablishMutualIdentity(outcome.identity, outcome.groupId, step.command))
            is Outcome.MutualIdentityEstablished ->
                Transition.Next(Step.ConfirmMemberIdentity(outcome.identity, outcome.groupId, step.command))
            Outcome.MemberIdentityConfirmed -> Transition.Complete()
        }

    sealed interface Step : WorkflowStep {
        val command: MembershipCommand.ResumeApprovedGroupJoin

        data class FindHandshake(
            override val command: MembershipCommand.ResumeApprovedGroupJoin
        ) : Step

        data class LoadApprovedIdentity(
            val groupId: String,
            override val command: MembershipCommand.ResumeApprovedGroupJoin
        ) : Step

        data class StageIdentity(
            val identity: RemotePeerIdentity,
            val groupId: String,
            override val command: MembershipCommand.ResumeApprovedGroupJoin
        ) : Step

        data class EstablishMutualIdentity(
            val identity: RemotePeerIdentity,
            val groupId: String,
            override val command: MembershipCommand.ResumeApprovedGroupJoin
        ) : Step

        data class ConfirmMemberIdentity(
            val identity: RemotePeerIdentity,
            val groupId: String,
            override val command: MembershipCommand.ResumeApprovedGroupJoin
        ) : Step
    }

    sealed interface Outcome {
        data class HandshakeFound(
            val handshake: MembershipHandshake
        ) : Outcome

        data class IdentityLoaded(
            val identity: RemotePeerIdentity,
            val groupId: String
        ) : Outcome

        data class IdentityStaged(
            val identity: RemotePeerIdentity,
            val groupId: String
        ) : Outcome

        data class MutualIdentityEstablished(
            val identity: RemotePeerIdentity,
            val groupId: String
        ) : Outcome

        data object MemberIdentityConfirmed : Outcome
    }
}
