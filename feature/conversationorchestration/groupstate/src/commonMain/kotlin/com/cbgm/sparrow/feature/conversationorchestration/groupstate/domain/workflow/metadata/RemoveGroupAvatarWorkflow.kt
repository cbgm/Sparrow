package com.cbgm.sparrow.feature.conversationorchestration.groupstate.domain.workflow.metadata

import com.cbgm.sparrow.feature.conversationorchestration.api.command.GroupAdministrationCommand
import com.cbgm.sparrow.feature.conversationorchestration.engine.machine.ConversationWorkflow
import com.cbgm.sparrow.feature.conversationorchestration.engine.machine.Transition
import com.cbgm.sparrow.feature.conversationorchestration.engine.machine.WorkflowStep
import com.cbgm.sparrow.feature.conversationorchestration.engine.runner.WorkflowEngine
import com.cbgm.sparrow.feature.conversationorchestration.groupstate.domain.command.GroupStateCommand
import kotlin.reflect.KClass

/** PublishRemoval: the removal is published like any other avatar change. */
class RemoveGroupAvatarWorkflow internal constructor(
    private val engine: WorkflowEngine
) : ConversationWorkflow<
        GroupAdministrationCommand.RemoveAvatar,
        RemoveGroupAvatarWorkflow.Step,
        RemoveGroupAvatarWorkflow.Outcome
    > {
    override val commandType: KClass<GroupAdministrationCommand.RemoveAvatar> =
        GroupAdministrationCommand.RemoveAvatar::class

    override fun firstStep(command: GroupAdministrationCommand.RemoveAvatar): Step = Step.PublishRemoval(command)

    override suspend fun perform(step: Step): Outcome {
        engine.run(GroupStateCommand.PublishGroupAvatar(step.command.groupId, bytes = null)).getOrThrow()
        return Outcome.RemovalPublished
    }

    override fun transition(step: Step, outcome: Outcome): Transition<Step> = Transition.Complete()

    sealed interface Step : WorkflowStep {
        val command: GroupAdministrationCommand.RemoveAvatar

        data class PublishRemoval(
            override val command: GroupAdministrationCommand.RemoveAvatar
        ) : Step
    }

    sealed interface Outcome {
        data object RemovalPublished : Outcome
    }
}
