package com.cbgm.sparrow.feature.conversationorchestration.incoming.domain.workflow.membership

import com.cbgm.sparrow.feature.conversationorchestration.engine.machine.Transition
import com.cbgm.sparrow.feature.conversationorchestration.engine.machine.WorkflowStateMachine
import com.cbgm.sparrow.feature.conversationorchestration.engine.machine.WorkflowStep
import com.cbgm.sparrow.feature.conversationorchestration.incoming.domain.command.IncomingCommand
import com.cbgm.sparrow.feature.identity.domain.usecase.EnsureRemoteSigningIdentityUseCase
import com.cbgm.sparrow.feature.invite.domain.model.InvitationPayloadType
import com.cbgm.sparrow.feature.invite.domain.model.InvitationResponse
import com.cbgm.sparrow.feature.invite.domain.usecase.HandleInvitationResponseUseCase
import com.cbgm.sparrow.feature.membership.domain.model.MembershipDeclineDisposition
import com.cbgm.sparrow.feature.membership.domain.model.MembershipDeclineResult
import com.cbgm.sparrow.feature.membership.domain.usecase.ClearMembershipHandshakeUseCase
import com.cbgm.sparrow.feature.membership.domain.usecase.MarkMembershipRemovedUseCase
import com.cbgm.sparrow.feature.membership.domain.usecase.ReceiveGroupMembershipDeclineUseCase
import kotlin.reflect.KClass

/**
 * pending invitation: ReceiveDecline -> ClearHandshake -> MarkInvitationDeclined
 * active member:      ReceiveDecline -> RequireSigningIdentity -> MarkMembershipRemoved
 *
 * A pending decline is authenticated by its signed original challenge and never changes a pinned key;
 * removing an active member requires the accepted identity.
 */
class ReceiveGroupInviteDeclineWorkflow internal constructor(
    private val receiveGroupMembershipDecline: ReceiveGroupMembershipDeclineUseCase,
    private val clearMembershipHandshake: ClearMembershipHandshakeUseCase,
    private val handleInvitationResponse: HandleInvitationResponseUseCase,
    private val ensureRemoteSigningIdentity: EnsureRemoteSigningIdentityUseCase,
    private val markMembershipRemoved: MarkMembershipRemovedUseCase
) : WorkflowStateMachine<
        IncomingCommand.ReceiveGroupInviteDecline,
        ReceiveGroupInviteDeclineWorkflow.Step,
        ReceiveGroupInviteDeclineWorkflow.Outcome
    > {
    override val commandType: KClass<IncomingCommand.ReceiveGroupInviteDecline> =
        IncomingCommand.ReceiveGroupInviteDecline::class

    override fun firstStep(command: IncomingCommand.ReceiveGroupInviteDecline): Step = Step.ReceiveDecline(command)

    override suspend fun perform(step: Step): Outcome =
        when (step) {
            is Step.ReceiveDecline ->
                receiveGroupMembershipDecline(step.command.context.contactId, step.command.packet).getOrThrow()
                    ?.let(Outcome::DeclineReceived)
                    ?: Outcome.Done
            is Step.ClearHandshake -> {
                clearMembershipHandshake(step.decline.sourceId).getOrThrow()
                Outcome.HandshakeCleared(step.decline)
            }
            is Step.MarkInvitationDeclined -> {
                handleInvitationResponse(
                    InvitationPayloadType.GROUP,
                    step.decline.sourceId,
                    InvitationResponse.DECLINED
                ).getOrThrow()
                Outcome.Done
            }
            is Step.RequireSigningIdentity -> {
                ensureRemoteSigningIdentity(step.decline.peerId, step.decline.signingPublicKey).getOrThrow()
                Outcome.SigningIdentityConfirmed(step.decline)
            }
            is Step.MarkMembershipRemoved -> {
                markMembershipRemoved(
                    step.decline.sourceId,
                    step.command.context.receivedAtEpochMilliseconds
                ).getOrThrow()
                Outcome.Done
            }
        }

    override fun transition(step: Step, outcome: Outcome): Transition<Step> =
        when (outcome) {
            is Outcome.DeclineReceived ->
                when (outcome.decline.disposition) {
                    MembershipDeclineDisposition.PENDING_HANDSHAKE ->
                        Transition.Next(Step.ClearHandshake(outcome.decline, step.command))
                    MembershipDeclineDisposition.ACTIVE_MEMBER ->
                        Transition.Next(Step.RequireSigningIdentity(outcome.decline, step.command))
                }
            is Outcome.HandshakeCleared -> Transition.Next(Step.MarkInvitationDeclined(outcome.decline, step.command))
            is Outcome.SigningIdentityConfirmed ->
                Transition.Next(Step.MarkMembershipRemoved(outcome.decline, step.command))
            Outcome.Done -> Transition.Complete()
        }

    sealed interface Step : WorkflowStep {
        val command: IncomingCommand.ReceiveGroupInviteDecline

        data class ReceiveDecline(
            override val command: IncomingCommand.ReceiveGroupInviteDecline
        ) : Step

        data class ClearHandshake(
            val decline: MembershipDeclineResult,
            override val command: IncomingCommand.ReceiveGroupInviteDecline
        ) : Step

        data class MarkInvitationDeclined(
            val decline: MembershipDeclineResult,
            override val command: IncomingCommand.ReceiveGroupInviteDecline
        ) : Step

        data class RequireSigningIdentity(
            val decline: MembershipDeclineResult,
            override val command: IncomingCommand.ReceiveGroupInviteDecline
        ) : Step

        data class MarkMembershipRemoved(
            val decline: MembershipDeclineResult,
            override val command: IncomingCommand.ReceiveGroupInviteDecline
        ) : Step
    }

    sealed interface Outcome {
        data class DeclineReceived(
            val decline: MembershipDeclineResult
        ) : Outcome

        data class HandshakeCleared(
            val decline: MembershipDeclineResult
        ) : Outcome

        data class SigningIdentityConfirmed(
            val decline: MembershipDeclineResult
        ) : Outcome

        data object Done : Outcome
    }
}
