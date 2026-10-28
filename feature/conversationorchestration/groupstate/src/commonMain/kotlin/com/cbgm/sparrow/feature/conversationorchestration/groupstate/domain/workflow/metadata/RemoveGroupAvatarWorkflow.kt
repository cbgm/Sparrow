package com.cbgm.sparrow.feature.conversationorchestration.groupstate.domain.workflow.metadata

import com.cbgm.sparrow.feature.conversationorchestration.api.command.GroupAdministrationCommand
import com.cbgm.sparrow.feature.conversationorchestration.engine.machine.ConversationWorkflow
import com.cbgm.sparrow.feature.conversationorchestration.engine.machine.Transition
import com.cbgm.sparrow.feature.conversationorchestration.engine.machine.WorkflowStep
import com.cbgm.sparrow.feature.conversationorchestration.engine.runner.WorkflowEngine
import com.cbgm.sparrow.feature.conversationorchestration.groupstate.domain.command.GroupStateCommand
import com.cbgm.sparrow.feature.conversationorchestration.groupstate.domain.model.GroupAvatarState
import com.cbgm.sparrow.feature.conversationorchestration.groupstate.domain.usecase.PrepareGroupAvatarUseCase
import com.cbgm.sparrow.feature.conversationorchestration.groupstate.domain.usecase.RequireGroupAdminContextUseCase
import com.cbgm.sparrow.feature.conversationorchestration.groupstate.domain.usecase.SaveGroupAvatarUseCase
import kotlin.reflect.KClass

/**
 * AuthorizeAdmin -> PrepareAvatar -> SaveAvatar -> PublishAvatar. The removal is a new avatar version without an image.
 *
 * The new avatar is saved as unpublished right away, so the user sees it at once without waiting for a running
 * membership change. Only the publishing ([GroupStateCommand.PublishGroupAvatar]) waits for the group
 * administration lock. A failed or interrupted publish leaves the avatar unpublished: the same change again, or the
 * recovery after the next start, publishes it.
 */
class RemoveGroupAvatarWorkflow internal constructor(
    private val engine: WorkflowEngine,
    private val requireGroupAdminContext: RequireGroupAdminContextUseCase,
    private val prepareGroupAvatar: PrepareGroupAvatarUseCase,
    private val saveGroupAvatar: SaveGroupAvatarUseCase
) : ConversationWorkflow<
        GroupAdministrationCommand.RemoveAvatar,
        RemoveGroupAvatarWorkflow.Step,
        RemoveGroupAvatarWorkflow.Outcome
    > {
    override val commandType: KClass<GroupAdministrationCommand.RemoveAvatar> =
        GroupAdministrationCommand.RemoveAvatar::class

    override fun firstStep(command: GroupAdministrationCommand.RemoveAvatar): Step = Step.AuthorizeAdmin(command)

    override suspend fun perform(step: Step): Outcome =
        when (step) {
            is Step.AuthorizeAdmin -> {
                require(step.command.groupId.isNotBlank()) { "Group ID must not be blank" }
                requireGroupAdminContext(step.command.groupId, ADMIN_ACTION).getOrThrow()
                Outcome.AdminAuthorized
            }
            is Step.PrepareAvatar ->
                Outcome.AvatarPrepared(prepareGroupAvatar(step.command.groupId, null).getOrThrow())
            is Step.SaveAvatar -> {
                saveGroupAvatar(step.avatar).getOrThrow()
                Outcome.AvatarSaved
            }
            is Step.PublishAvatar -> {
                engine.run(GroupStateCommand.PublishGroupAvatar(step.command.groupId)).getOrThrow()
                Outcome.AvatarPublished
            }
        }

    override fun transition(step: Step, outcome: Outcome): Transition<Step> =
        when (outcome) {
            Outcome.AdminAuthorized -> Transition.Next(Step.PrepareAvatar(step.command))
            is Outcome.AvatarPrepared -> Transition.Next(Step.SaveAvatar(outcome.avatar, step.command))
            Outcome.AvatarSaved -> Transition.Next(Step.PublishAvatar(step.command))
            Outcome.AvatarPublished -> Transition.Complete()
        }

    sealed interface Step : WorkflowStep {
        val command: GroupAdministrationCommand.RemoveAvatar

        data class AuthorizeAdmin(
            override val command: GroupAdministrationCommand.RemoveAvatar
        ) : Step

        data class PrepareAvatar(
            override val command: GroupAdministrationCommand.RemoveAvatar
        ) : Step

        data class SaveAvatar(
            val avatar: GroupAvatarState,
            override val command: GroupAdministrationCommand.RemoveAvatar
        ) : Step

        data class PublishAvatar(
            override val command: GroupAdministrationCommand.RemoveAvatar
        ) : Step
    }

    sealed interface Outcome {
        data object AdminAuthorized : Outcome

        data class AvatarPrepared(
            val avatar: GroupAvatarState
        ) : Outcome

        data object AvatarSaved : Outcome

        data object AvatarPublished : Outcome
    }

    private companion object {
        const val ADMIN_ACTION = "group avatar"
    }
}
