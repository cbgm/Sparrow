package com.cbgm.sparrow.feature.conversationorchestration.groupstate.domain.workflow.metadata

import com.cbgm.sparrow.core.base.group.domain.model.GroupMetadataPolicy
import com.cbgm.sparrow.feature.conversationorchestration.api.command.GroupAdministrationCommand
import com.cbgm.sparrow.feature.conversationorchestration.engine.machine.ConversationWorkflow
import com.cbgm.sparrow.feature.conversationorchestration.engine.machine.Transition
import com.cbgm.sparrow.feature.conversationorchestration.engine.machine.WorkflowStep
import com.cbgm.sparrow.feature.conversationorchestration.engine.runner.WorkflowEngine
import com.cbgm.sparrow.feature.conversationorchestration.groupstate.domain.command.GroupStateCommand
import kotlin.reflect.KClass

/** ValidateAvatar -> PublishAvatar. */
class ChangeGroupAvatarWorkflow internal constructor(
    private val engine: WorkflowEngine
) : ConversationWorkflow<
        GroupAdministrationCommand.ChangeAvatar,
        ChangeGroupAvatarWorkflow.Step,
        ChangeGroupAvatarWorkflow.Outcome
    > {
    override val commandType: KClass<GroupAdministrationCommand.ChangeAvatar> =
        GroupAdministrationCommand.ChangeAvatar::class

    override fun firstStep(command: GroupAdministrationCommand.ChangeAvatar): Step = Step.ValidateAvatar(command)

    override suspend fun perform(step: Step): Outcome =
        when (step) {
            is Step.ValidateAvatar -> {
                GroupMetadataPolicy.requireValidAvatar(step.command.bytes)
                Outcome.AvatarValid
            }
            is Step.PublishAvatar -> {
                engine.run(
                    GroupStateCommand.PublishGroupAvatar(step.command.groupId, step.command.bytes.copyOf())
                ).getOrThrow()
                Outcome.AvatarPublished
            }
        }

    override fun transition(step: Step, outcome: Outcome): Transition<Step> =
        when (outcome) {
            Outcome.AvatarValid -> Transition.Next(Step.PublishAvatar(step.command))
            Outcome.AvatarPublished -> Transition.Complete()
        }

    sealed interface Step : WorkflowStep {
        val command: GroupAdministrationCommand.ChangeAvatar

        data class ValidateAvatar(
            override val command: GroupAdministrationCommand.ChangeAvatar
        ) : Step

        data class PublishAvatar(
            override val command: GroupAdministrationCommand.ChangeAvatar
        ) : Step
    }

    sealed interface Outcome {
        data object AvatarValid : Outcome

        data object AvatarPublished : Outcome
    }
}
