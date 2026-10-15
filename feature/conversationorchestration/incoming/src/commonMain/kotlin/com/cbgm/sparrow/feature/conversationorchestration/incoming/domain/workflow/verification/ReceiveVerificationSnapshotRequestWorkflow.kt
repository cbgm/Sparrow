package com.cbgm.sparrow.feature.conversationorchestration.incoming.domain.workflow.verification

import com.cbgm.sparrow.feature.conversationorchestration.engine.machine.Transition
import com.cbgm.sparrow.feature.conversationorchestration.engine.machine.WorkflowStateMachine
import com.cbgm.sparrow.feature.conversationorchestration.engine.machine.WorkflowStep
import com.cbgm.sparrow.feature.conversationorchestration.incoming.domain.command.IncomingCommand
import com.cbgm.sparrow.feature.conversationorchestration.membership.domain.usecase.AcceptVerificationSnapshotRequestUseCase
import com.cbgm.sparrow.feature.conversationorchestration.membership.domain.usecase.RefreshOwnedGroupVerificationUseCase
import com.cbgm.sparrow.feature.conversationorchestration.membership.domain.usecase.SendGroupVerificationSnapshotUseCase
import com.cbgm.sparrow.feature.conversationorchestration.membership.domain.workflow.verification.groupVerificationLockKey
import kotlin.reflect.KClass

/** AuthenticateRequest -> RefreshRows -> SendSnapshot: the admin answers a member with its current rows. */
class ReceiveVerificationSnapshotRequestWorkflow internal constructor(
    private val acceptVerificationSnapshotRequest: AcceptVerificationSnapshotRequestUseCase,
    private val refreshOwnedGroupVerification: RefreshOwnedGroupVerificationUseCase,
    private val sendGroupVerificationSnapshot: SendGroupVerificationSnapshotUseCase
) : WorkflowStateMachine<
        IncomingCommand.ReceiveVerificationSnapshotRequest,
        ReceiveVerificationSnapshotRequestWorkflow.Step,
        ReceiveVerificationSnapshotRequestWorkflow.Outcome
    > {
    override val commandType: KClass<IncomingCommand.ReceiveVerificationSnapshotRequest> =
        IncomingCommand.ReceiveVerificationSnapshotRequest::class

    override fun lockKey(command: IncomingCommand.ReceiveVerificationSnapshotRequest): String =
        groupVerificationLockKey(command.packet.groupId)

    override fun firstStep(command: IncomingCommand.ReceiveVerificationSnapshotRequest): Step =
        Step.AuthenticateRequest(command)

    override suspend fun perform(step: Step): Outcome {
        val groupId = step.command.packet.groupId
        return when (step) {
            is Step.AuthenticateRequest -> {
                acceptVerificationSnapshotRequest(step.command.context, step.command.packet).getOrThrow()
                Outcome.RequestAuthenticated
            }
            is Step.RefreshRows -> {
                refreshOwnedGroupVerification(groupId).getOrThrow()
                Outcome.RowsRefreshed
            }
            is Step.SendSnapshot -> {
                sendGroupVerificationSnapshot(groupId, step.command.context.contactId).getOrThrow()
                Outcome.SnapshotSent
            }
        }
    }

    override fun transition(step: Step, outcome: Outcome): Transition<Step> =
        when (outcome) {
            Outcome.RequestAuthenticated -> Transition.Next(Step.RefreshRows(step.command))
            Outcome.RowsRefreshed -> Transition.Next(Step.SendSnapshot(step.command))
            Outcome.SnapshotSent -> Transition.Complete()
        }

    sealed interface Step : WorkflowStep {
        val command: IncomingCommand.ReceiveVerificationSnapshotRequest

        data class AuthenticateRequest(
            override val command: IncomingCommand.ReceiveVerificationSnapshotRequest
        ) : Step

        data class RefreshRows(
            override val command: IncomingCommand.ReceiveVerificationSnapshotRequest
        ) : Step

        data class SendSnapshot(
            override val command: IncomingCommand.ReceiveVerificationSnapshotRequest
        ) : Step
    }

    sealed interface Outcome {
        data object RequestAuthenticated : Outcome

        data object RowsRefreshed : Outcome

        data object SnapshotSent : Outcome
    }
}
