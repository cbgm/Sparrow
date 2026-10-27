package com.cbgm.sparrow.feature.conversationorchestration.groupstate.domain.workflow.metadata

import com.cbgm.sparrow.core.base.group.domain.model.GroupMetadataPolicy
import com.cbgm.sparrow.feature.conversationorchestration.api.command.GroupAdministrationCommand
import com.cbgm.sparrow.feature.conversationorchestration.engine.machine.ConversationWorkflow
import com.cbgm.sparrow.feature.conversationorchestration.engine.machine.Transition
import com.cbgm.sparrow.feature.conversationorchestration.engine.machine.WorkflowStep
import com.cbgm.sparrow.feature.conversationorchestration.engine.runner.WorkflowEngine
import com.cbgm.sparrow.feature.conversationorchestration.groupstate.domain.command.GroupStateCommand
import com.cbgm.sparrow.feature.conversationorchestration.groupstate.domain.model.GroupTitleState
import com.cbgm.sparrow.feature.conversationorchestration.groupstate.domain.usecase.PrepareGroupTitleUseCase
import com.cbgm.sparrow.feature.conversationorchestration.groupstate.domain.usecase.RequireGroupAdminContextUseCase
import com.cbgm.sparrow.feature.conversationorchestration.groupstate.domain.usecase.SaveGroupTitleUseCase
import kotlin.reflect.KClass

/**
 * AuthorizeAdmin -> PrepareTitle -> SaveTitle -> PublishTitle.
 * An unchanged title that was published already stops after PrepareTitle.
 *
 * The new title is saved as unpublished right away, so the user sees it at once without waiting for a running
 * membership change. Only the publishing ([GroupStateCommand.PublishGroupTitle]) waits for the group administration
 * lock. A failed or interrupted publish leaves the title unpublished: the same change again, or the recovery after
 * the next start, publishes it.
 */
class ChangeGroupTitleWorkflow internal constructor(
    private val engine: WorkflowEngine,
    private val requireGroupAdminContext: RequireGroupAdminContextUseCase,
    private val prepareGroupTitle: PrepareGroupTitleUseCase,
    private val saveGroupTitle: SaveGroupTitleUseCase
) : ConversationWorkflow<
        GroupAdministrationCommand.ChangeTitle,
        ChangeGroupTitleWorkflow.Step,
        ChangeGroupTitleWorkflow.Outcome
    > {
    override val commandType: KClass<GroupAdministrationCommand.ChangeTitle> =
        GroupAdministrationCommand.ChangeTitle::class

    override fun firstStep(command: GroupAdministrationCommand.ChangeTitle): Step = Step.AuthorizeAdmin(command)

    override suspend fun perform(step: Step): Outcome =
        when (step) {
            is Step.AuthorizeAdmin -> {
                require(step.command.groupId.isNotBlank()) { "Group ID must not be blank" }
                GroupMetadataPolicy.requireValidTitle(step.command.title)
                requireGroupAdminContext(step.command.groupId, ADMIN_ACTION).getOrThrow()
                Outcome.AdminAuthorized
            }
            is Step.PrepareTitle ->
                titlePrepared(prepareGroupTitle(step.command.groupId, step.command.title).getOrThrow())
            is Step.SaveTitle -> {
                saveGroupTitle(step.title).getOrThrow()
                Outcome.TitleSaved
            }
            is Step.PublishTitle -> {
                engine.run(GroupStateCommand.PublishGroupTitle(step.command.groupId)).getOrThrow()
                Outcome.TitlePublished
            }
        }

    override fun transition(step: Step, outcome: Outcome): Transition<Step> =
        when (outcome) {
            Outcome.AdminAuthorized -> Transition.Next(Step.PrepareTitle(step.command))
            Outcome.TitleUnchanged -> Transition.Complete()
            is Outcome.TitlePrepared -> Transition.Next(Step.SaveTitle(outcome.title, step.command))
            Outcome.TitleSaved -> Transition.Next(Step.PublishTitle(step.command))
            Outcome.TitlePublished -> Transition.Complete()
        }

    private fun titlePrepared(title: GroupTitleState?): Outcome =
        title?.let(Outcome::TitlePrepared) ?: Outcome.TitleUnchanged

    sealed interface Step : WorkflowStep {
        val command: GroupAdministrationCommand.ChangeTitle

        data class AuthorizeAdmin(
            override val command: GroupAdministrationCommand.ChangeTitle
        ) : Step

        data class PrepareTitle(
            override val command: GroupAdministrationCommand.ChangeTitle
        ) : Step

        data class SaveTitle(
            val title: GroupTitleState,
            override val command: GroupAdministrationCommand.ChangeTitle
        ) : Step

        data class PublishTitle(
            override val command: GroupAdministrationCommand.ChangeTitle
        ) : Step
    }

    sealed interface Outcome {
        data object AdminAuthorized : Outcome

        data object TitleUnchanged : Outcome

        data class TitlePrepared(
            val title: GroupTitleState
        ) : Outcome

        data object TitleSaved : Outcome

        data object TitlePublished : Outcome
    }

    private companion object {
        const val ADMIN_ACTION = "group title"
    }
}
