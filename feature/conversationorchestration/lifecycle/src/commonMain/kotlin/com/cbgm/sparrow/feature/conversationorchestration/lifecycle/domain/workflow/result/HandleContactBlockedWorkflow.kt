package com.cbgm.sparrow.feature.conversationorchestration.lifecycle.domain.workflow.result

import com.cbgm.sparrow.feature.contacts.domain.repository.ContactBlocklistRepository
import com.cbgm.sparrow.feature.conversationorchestration.engine.machine.Transition
import com.cbgm.sparrow.feature.conversationorchestration.engine.machine.WorkflowStateMachine
import com.cbgm.sparrow.feature.conversationorchestration.engine.machine.WorkflowStep
import com.cbgm.sparrow.feature.conversationorchestration.engine.runner.WorkflowEngine
import com.cbgm.sparrow.feature.conversationorchestration.lifecycle.domain.command.LifecycleCommand
import kotlin.reflect.KClass

/** CheckStillBlocked -> RevokeExchange. An unblocked contact keeps its authorization. */
class HandleContactBlockedWorkflow internal constructor(
    private val engine: WorkflowEngine,
    private val blocklistRepository: ContactBlocklistRepository
) : WorkflowStateMachine<
        LifecycleCommand.HandleContactBlocked,
        HandleContactBlockedWorkflow.Step,
        HandleContactBlockedWorkflow.Outcome
    > {
    override val commandType: KClass<LifecycleCommand.HandleContactBlocked> =
        LifecycleCommand.HandleContactBlocked::class

    override fun firstStep(command: LifecycleCommand.HandleContactBlocked): Step = Step.CheckStillBlocked(command)

    override suspend fun perform(step: Step): Outcome =
        when (step) {
            is Step.CheckStillBlocked ->
                if (blocklistRepository.isBlocked(step.command.contactId).getOrThrow()) {
                    Outcome.StillBlocked
                } else {
                    Outcome.Unblocked
                }
            is Step.RevokeExchange -> {
                engine.run(LifecycleCommand.RevokePeerExchange(step.command.contactId)).getOrThrow()
                Outcome.ExchangeRevoked
            }
        }

    override fun transition(step: Step, outcome: Outcome): Transition<Step> =
        when (outcome) {
            Outcome.StillBlocked -> Transition.Next(Step.RevokeExchange(step.command))
            Outcome.Unblocked -> Transition.Complete()
            Outcome.ExchangeRevoked -> Transition.Complete()
        }

    sealed interface Step : WorkflowStep {
        val command: LifecycleCommand.HandleContactBlocked

        data class CheckStillBlocked(
            override val command: LifecycleCommand.HandleContactBlocked
        ) : Step

        data class RevokeExchange(
            override val command: LifecycleCommand.HandleContactBlocked
        ) : Step
    }

    sealed interface Outcome {
        data object StillBlocked : Outcome

        data object Unblocked : Outcome

        data object ExchangeRevoked : Outcome
    }
}
