package com.cbgm.sparrow.feature.conversationorchestration.incoming.domain.workflow.verification

import com.cbgm.sparrow.feature.conversationorchestration.engine.machine.Transition
import com.cbgm.sparrow.feature.conversationorchestration.engine.machine.WorkflowStateMachine
import com.cbgm.sparrow.feature.conversationorchestration.engine.machine.WorkflowStep
import com.cbgm.sparrow.feature.conversationorchestration.incoming.domain.command.IncomingCommand
import com.cbgm.sparrow.feature.conversationorchestration.membership.domain.usecase.AcceptParticipantVerificationReceiptUseCase
import com.cbgm.sparrow.feature.conversationorchestration.membership.domain.usecase.BroadcastGroupVerificationSnapshotUseCase
import com.cbgm.sparrow.feature.conversationorchestration.membership.domain.workflow.verification.groupVerificationLockKey
import kotlin.reflect.KClass

/** AcceptReceipt -> BroadcastSnapshot: the admin records that a member verified it and tells everybody. */
class ReceiveVerificationReceiptWorkflow internal constructor(
    private val acceptParticipantVerificationReceipt: AcceptParticipantVerificationReceiptUseCase,
    private val broadcastGroupVerificationSnapshot: BroadcastGroupVerificationSnapshotUseCase
) : WorkflowStateMachine<
        IncomingCommand.ReceiveVerificationReceipt,
        ReceiveVerificationReceiptWorkflow.Step,
        ReceiveVerificationReceiptWorkflow.Outcome
    > {
    override val commandType: KClass<IncomingCommand.ReceiveVerificationReceipt> =
        IncomingCommand.ReceiveVerificationReceipt::class

    override fun lockKey(command: IncomingCommand.ReceiveVerificationReceipt): String =
        groupVerificationLockKey(command.packet.groupId)

    override fun firstStep(command: IncomingCommand.ReceiveVerificationReceipt): Step = Step.AcceptReceipt(command)

    override suspend fun perform(step: Step): Outcome =
        when (step) {
            is Step.AcceptReceipt -> {
                acceptParticipantVerificationReceipt(step.command.context, step.command.packet).getOrThrow()
                Outcome.ReceiptAccepted
            }
            is Step.BroadcastSnapshot -> {
                broadcastGroupVerificationSnapshot(step.command.packet.groupId).getOrThrow()
                Outcome.SnapshotBroadcast
            }
        }

    override fun transition(step: Step, outcome: Outcome): Transition<Step> =
        when (outcome) {
            Outcome.ReceiptAccepted -> Transition.Next(Step.BroadcastSnapshot(step.command))
            Outcome.SnapshotBroadcast -> Transition.Complete()
        }

    sealed interface Step : WorkflowStep {
        val command: IncomingCommand.ReceiveVerificationReceipt

        data class AcceptReceipt(
            override val command: IncomingCommand.ReceiveVerificationReceipt
        ) : Step

        data class BroadcastSnapshot(
            override val command: IncomingCommand.ReceiveVerificationReceipt
        ) : Step
    }

    sealed interface Outcome {
        data object ReceiptAccepted : Outcome

        data object SnapshotBroadcast : Outcome
    }
}
