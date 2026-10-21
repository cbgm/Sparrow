package com.cbgm.sparrow.feature.conversationorchestration.incoming.domain.workflow.operation

import com.cbgm.sparrow.core.util.logging.SparrowLog
import com.cbgm.sparrow.feature.conversationorchestration.engine.machine.Transition
import com.cbgm.sparrow.feature.conversationorchestration.engine.machine.WorkflowStateMachine
import com.cbgm.sparrow.feature.conversationorchestration.engine.machine.WorkflowStep
import com.cbgm.sparrow.feature.conversationorchestration.engine.runner.WorkflowEngine
import com.cbgm.sparrow.feature.conversationorchestration.incoming.domain.command.IncomingCommand
import com.cbgm.sparrow.feature.conversationorchestration.incoming.domain.model.ParkedMessageOperation
import com.cbgm.sparrow.feature.conversationorchestration.incoming.domain.repository.ParkedMessageOperationRepository
import kotlin.reflect.KClass

/**
 * FindParked -> [Apply -> Apply -> ...]
 *
 * Runs after a message was stored: the operations that arrived before it (an edit that overtook its message
 * after a reconnect, for example) are applied in the order they were sent. Each one is removed once it ran;
 * one that is not allowed, such as an edit by another sender, is dropped as it would have been on arrival.
 */
class ApplyParkedMessageOperationsWorkflow internal constructor(
    private val engine: WorkflowEngine,
    private val parkedOperations: ParkedMessageOperationRepository
) : WorkflowStateMachine<
        IncomingCommand.ApplyParkedMessageOperations,
        ApplyParkedMessageOperationsWorkflow.Step,
        ApplyParkedMessageOperationsWorkflow.Outcome
    > {
    private val logger = SparrowLog.withTag("ApplyParkedMessageOperationsWorkflow")

    override val commandType: KClass<IncomingCommand.ApplyParkedMessageOperations> =
        IncomingCommand.ApplyParkedMessageOperations::class

    override fun firstStep(command: IncomingCommand.ApplyParkedMessageOperations): Step = Step.FindParked(command)

    override suspend fun perform(step: Step): Outcome =
        when (step) {
            is Step.FindParked -> findParked(step.command)
            is Step.Apply -> apply(step.waiting)
        }

    override fun transition(step: Step, outcome: Outcome): Transition<Step> =
        when (outcome) {
            is Outcome.Waiting -> applyNext(outcome.operations, step.command)
        }

    private suspend fun findParked(command: IncomingCommand.ApplyParkedMessageOperations): Outcome =
        Outcome.Waiting(parkedOperations.findFor(command.messageId).getOrThrow())

    private suspend fun apply(waiting: List<ParkedMessageOperation>): Outcome {
        val parked = waiting.first()
        engine.run(parked.operation).onFailure { error ->
            logger.warn(error) { "Parked operation on message ${parked.operation.operation.messageId} was dropped" }
        }
        parkedOperations.remove(parked.id).getOrThrow()
        return Outcome.Waiting(waiting.drop(1))
    }

    private fun applyNext(
        waiting: List<ParkedMessageOperation>,
        command: IncomingCommand.ApplyParkedMessageOperations
    ): Transition<Step> = if (waiting.isEmpty()) Transition.Complete() else Transition.Next(Step.Apply(waiting, command))

    sealed interface Step : WorkflowStep {
        val command: IncomingCommand.ApplyParkedMessageOperations

        data class FindParked(
            override val command: IncomingCommand.ApplyParkedMessageOperations
        ) : Step

        /** Applies the first of [waiting]. */
        data class Apply(
            val waiting: List<ParkedMessageOperation>,
            override val command: IncomingCommand.ApplyParkedMessageOperations
        ) : Step
    }

    sealed interface Outcome {
        data class Waiting(
            val operations: List<ParkedMessageOperation>
        ) : Outcome
    }
}
