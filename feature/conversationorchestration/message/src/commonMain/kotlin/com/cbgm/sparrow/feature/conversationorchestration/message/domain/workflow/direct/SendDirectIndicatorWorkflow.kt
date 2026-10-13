package com.cbgm.sparrow.feature.conversationorchestration.message.domain.workflow.direct

import com.cbgm.sparrow.feature.conversationorchestration.api.command.DirectConversationCommand
import com.cbgm.sparrow.feature.conversationorchestration.delivery.domain.usecase.SendConversationIndicatorUseCase
import com.cbgm.sparrow.feature.conversationorchestration.engine.machine.ConversationWorkflow
import com.cbgm.sparrow.feature.conversationorchestration.engine.machine.Transition
import com.cbgm.sparrow.feature.conversationorchestration.engine.machine.WorkflowStep
import kotlin.reflect.KClass

/** SendIndicator. Indicators are transient transport events and never persisted. */
class SendDirectIndicatorWorkflow internal constructor(
    private val sendConversationIndicator: SendConversationIndicatorUseCase
) : ConversationWorkflow<
        DirectConversationCommand.SendIndicator,
        SendDirectIndicatorWorkflow.Step,
        SendDirectIndicatorWorkflow.Outcome
    > {
    override val commandType: KClass<DirectConversationCommand.SendIndicator> =
        DirectConversationCommand.SendIndicator::class

    override fun firstStep(command: DirectConversationCommand.SendIndicator): Step = Step.SendIndicator(command)

    override suspend fun perform(step: Step): Outcome {
        sendConversationIndicator.toContact(step.command.contactId, step.command.indicator.name).getOrThrow()
        return Outcome.IndicatorSent
    }

    override fun transition(step: Step, outcome: Outcome): Transition<Step> = Transition.Complete()

    sealed interface Step : WorkflowStep {
        val command: DirectConversationCommand.SendIndicator

        data class SendIndicator(
            override val command: DirectConversationCommand.SendIndicator
        ) : Step
    }

    sealed interface Outcome {
        data object IndicatorSent : Outcome
    }
}
