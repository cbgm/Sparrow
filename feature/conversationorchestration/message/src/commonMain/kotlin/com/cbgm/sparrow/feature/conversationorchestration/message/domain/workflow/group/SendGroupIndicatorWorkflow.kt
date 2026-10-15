package com.cbgm.sparrow.feature.conversationorchestration.message.domain.workflow.group

import com.cbgm.sparrow.feature.conversationorchestration.api.command.GroupMessageCommand
import com.cbgm.sparrow.feature.conversationorchestration.delivery.domain.usecase.SendConversationIndicatorUseCase
import com.cbgm.sparrow.feature.conversationorchestration.engine.machine.ConversationWorkflow
import com.cbgm.sparrow.feature.conversationorchestration.engine.machine.Transition
import com.cbgm.sparrow.feature.conversationorchestration.engine.machine.WorkflowStep
import kotlin.reflect.KClass

/** SendIndicator to every current group member. Indicators are transient and never persisted. */
class SendGroupIndicatorWorkflow internal constructor(
    private val sendConversationIndicator: SendConversationIndicatorUseCase
) : ConversationWorkflow<
        GroupMessageCommand.SendIndicator,
        SendGroupIndicatorWorkflow.Step,
        SendGroupIndicatorWorkflow.Outcome
    > {
    override val commandType: KClass<GroupMessageCommand.SendIndicator> = GroupMessageCommand.SendIndicator::class

    override fun firstStep(command: GroupMessageCommand.SendIndicator): Step = Step.SendIndicator(command)

    override suspend fun perform(step: Step): Outcome {
        sendConversationIndicator.toGroup(step.command.groupId, step.command.indicator.name).getOrThrow()
        return Outcome.IndicatorSent
    }

    override fun transition(step: Step, outcome: Outcome): Transition<Step> = Transition.Complete()

    sealed interface Step : WorkflowStep {
        val command: GroupMessageCommand.SendIndicator

        data class SendIndicator(
            override val command: GroupMessageCommand.SendIndicator
        ) : Step
    }

    sealed interface Outcome {
        data object IndicatorSent : Outcome
    }
}
