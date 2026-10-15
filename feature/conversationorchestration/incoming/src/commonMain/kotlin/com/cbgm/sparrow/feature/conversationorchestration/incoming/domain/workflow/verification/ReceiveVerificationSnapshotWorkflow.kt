package com.cbgm.sparrow.feature.conversationorchestration.incoming.domain.workflow.verification

import com.cbgm.sparrow.feature.conversationorchestration.engine.machine.Transition
import com.cbgm.sparrow.feature.conversationorchestration.engine.machine.WorkflowStateMachine
import com.cbgm.sparrow.feature.conversationorchestration.engine.machine.WorkflowStep
import com.cbgm.sparrow.feature.conversationorchestration.incoming.domain.command.IncomingCommand
import com.cbgm.sparrow.feature.conversationorchestration.membership.domain.usecase.ApplyVerificationSnapshotUseCase
import com.cbgm.sparrow.feature.conversationorchestration.membership.domain.workflow.verification.groupVerificationLockKey
import kotlin.reflect.KClass

/** ApplySnapshot: a member stores the admin's newest verification rows. */
class ReceiveVerificationSnapshotWorkflow internal constructor(
    private val applyVerificationSnapshot: ApplyVerificationSnapshotUseCase
) : WorkflowStateMachine<
        IncomingCommand.ReceiveVerificationSnapshot,
        ReceiveVerificationSnapshotWorkflow.Step,
        ReceiveVerificationSnapshotWorkflow.Outcome
    > {
    override val commandType: KClass<IncomingCommand.ReceiveVerificationSnapshot> =
        IncomingCommand.ReceiveVerificationSnapshot::class

    override fun lockKey(command: IncomingCommand.ReceiveVerificationSnapshot): String =
        groupVerificationLockKey(command.packet.groupId)

    override fun firstStep(command: IncomingCommand.ReceiveVerificationSnapshot): Step = Step.ApplySnapshot(command)

    override suspend fun perform(step: Step): Outcome {
        applyVerificationSnapshot(step.command.context, step.command.packet).getOrThrow()
        return Outcome.SnapshotApplied
    }

    override fun transition(step: Step, outcome: Outcome): Transition<Step> = Transition.Complete()

    sealed interface Step : WorkflowStep {
        val command: IncomingCommand.ReceiveVerificationSnapshot

        data class ApplySnapshot(
            override val command: IncomingCommand.ReceiveVerificationSnapshot
        ) : Step
    }

    sealed interface Outcome {
        data object SnapshotApplied : Outcome
    }
}
