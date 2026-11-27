package com.cbgm.sparrow.feature.conversationorchestration.groupstate.domain.workflow.metadata

import com.cbgm.sparrow.core.base.group.domain.model.GroupMetadataPolicy
import com.cbgm.sparrow.feature.conversationorchestration.api.command.GroupAdministrationCommand
import com.cbgm.sparrow.feature.conversationorchestration.engine.machine.ConversationWorkflow
import com.cbgm.sparrow.feature.conversationorchestration.engine.machine.Transition
import com.cbgm.sparrow.feature.conversationorchestration.engine.machine.WorkflowStep
import com.cbgm.sparrow.feature.conversationorchestration.engine.runner.WorkflowEngine
import com.cbgm.sparrow.feature.conversationorchestration.groupstate.domain.command.GroupStateCommand
import com.cbgm.sparrow.feature.conversationorchestration.groupstate.domain.model.GroupDescriptionState
import com.cbgm.sparrow.feature.conversationorchestration.groupstate.domain.usecase.PrepareGroupDescriptionUseCase
import com.cbgm.sparrow.feature.conversationorchestration.groupstate.domain.usecase.RequireGroupAdminContextUseCase
import com.cbgm.sparrow.feature.conversationorchestration.groupstate.domain.usecase.SaveGroupDescriptionUseCase
import kotlin.reflect.KClass

/**
 * AuthorizeAdmin -> PrepareDescription -> SaveDescription -> PublishDescription.
 * An unchanged description that was published already stops after PrepareDescription.
 *
 * The new description is saved as unpublished right away, so the user sees it at once without waiting for a running
 * membership change. Only the publishing ([GroupStateCommand.PublishGroupDescription]) waits for the group administration
 * lock. A failed or interrupted publish leaves the description unpublished: the same change again, or the recovery after
 * the next start, publishes it.
 */
class ChangeGroupDescriptionWorkflow internal constructor(
    private val engine: WorkflowEngine,
    private val requireGroupAdminContext: RequireGroupAdminContextUseCase,
    private val prepareGroupDescription: PrepareGroupDescriptionUseCase,
    private val saveGroupDescription: SaveGroupDescriptionUseCase
) : ConversationWorkflow<
        GroupAdministrationCommand.ChangeDescription,
        ChangeGroupDescriptionWorkflow.Step,
        ChangeGroupDescriptionWorkflow.Outcome
    > {
    override val commandType: KClass<GroupAdministrationCommand.ChangeDescription> =
        GroupAdministrationCommand.ChangeDescription::class

    override fun firstStep(command: GroupAdministrationCommand.ChangeDescription): Step = Step.AuthorizeAdmin(command)

    override suspend fun perform(step: Step): Outcome =
        when (step) {
            is Step.AuthorizeAdmin -> {
                require(step.command.groupId.isNotBlank()) { "Group ID must not be blank" }
                GroupMetadataPolicy.requireValidDescription(step.command.description)
                requireGroupAdminContext(step.command.groupId, ADMIN_ACTION).getOrThrow()
                Outcome.AdminAuthorized
            }
            is Step.PrepareDescription ->
                descriptionPrepared(prepareGroupDescription(step.command.groupId, step.command.description).getOrThrow())
            is Step.SaveDescription -> {
                saveGroupDescription(step.description).getOrThrow()
                Outcome.DescriptionSaved
            }
            is Step.PublishDescription -> {
                engine.run(GroupStateCommand.PublishGroupDescription(step.command.groupId)).getOrThrow()
                Outcome.DescriptionPublished
            }
        }

    override fun transition(step: Step, outcome: Outcome): Transition<Step> =
        when (outcome) {
            Outcome.AdminAuthorized -> Transition.Next(Step.PrepareDescription(step.command))
            Outcome.DescriptionUnchanged -> Transition.Complete()
            is Outcome.DescriptionPrepared -> Transition.Next(Step.SaveDescription(outcome.description, step.command))
            Outcome.DescriptionSaved -> Transition.Next(Step.PublishDescription(step.command))
            Outcome.DescriptionPublished -> Transition.Complete()
        }

    private fun descriptionPrepared(description: GroupDescriptionState?): Outcome =
        description?.let(Outcome::DescriptionPrepared) ?: Outcome.DescriptionUnchanged

    sealed interface Step : WorkflowStep {
        val command: GroupAdministrationCommand.ChangeDescription

        data class AuthorizeAdmin(
            override val command: GroupAdministrationCommand.ChangeDescription
        ) : Step

        data class PrepareDescription(
            override val command: GroupAdministrationCommand.ChangeDescription
        ) : Step

        data class SaveDescription(
            val description: GroupDescriptionState,
            override val command: GroupAdministrationCommand.ChangeDescription
        ) : Step

        data class PublishDescription(
            override val command: GroupAdministrationCommand.ChangeDescription
        ) : Step
    }

    sealed interface Outcome {
        data object AdminAuthorized : Outcome

        data object DescriptionUnchanged : Outcome

        data class DescriptionPrepared(
            val description: GroupDescriptionState
        ) : Outcome

        data object DescriptionSaved : Outcome

        data object DescriptionPublished : Outcome
    }

    private companion object {
        const val ADMIN_ACTION = "group description"
    }
}
