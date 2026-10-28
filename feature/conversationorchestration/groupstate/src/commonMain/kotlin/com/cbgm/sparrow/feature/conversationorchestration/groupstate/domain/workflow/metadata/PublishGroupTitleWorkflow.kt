package com.cbgm.sparrow.feature.conversationorchestration.groupstate.domain.workflow.metadata

import com.cbgm.sparrow.feature.conversationorchestration.engine.machine.Transition
import com.cbgm.sparrow.feature.conversationorchestration.engine.machine.WorkflowStateMachine
import com.cbgm.sparrow.feature.conversationorchestration.engine.machine.WorkflowStep
import com.cbgm.sparrow.feature.conversationorchestration.groupstate.domain.command.GroupStateCommand
import com.cbgm.sparrow.feature.conversationorchestration.groupstate.domain.model.GroupAdminContext
import com.cbgm.sparrow.feature.conversationorchestration.groupstate.domain.model.GroupTitleState
import com.cbgm.sparrow.feature.conversationorchestration.groupstate.domain.repository.GroupTitleRepository
import com.cbgm.sparrow.feature.conversationorchestration.groupstate.domain.usecase.CreateGroupTitlePacketsUseCase
import com.cbgm.sparrow.feature.conversationorchestration.groupstate.domain.usecase.FindUnpublishedGroupStateUseCase
import com.cbgm.sparrow.feature.conversationorchestration.groupstate.domain.usecase.RequireGroupAdminContextUseCase
import com.cbgm.sparrow.feature.conversationorchestration.groupstate.domain.workflow.groupAdministrationLockKey
import com.cbgm.sparrow.feature.conversationorchestration.message.domain.usecase.EnqueueGroupPacketsUseCase
import com.cbgm.sparrow.protocol.packet.GroupTitleUpdatedPacket
import kotlin.reflect.KClass

/**
 * FindUnpublishedTitle -> AuthorizeAdmin -> CreateTitlePackets -> EnqueueTitlePackets -> MarkTitlePublished.
 * Without an unpublished title (published already, or replaced by a newer one from another admin) it stops
 * after FindUnpublishedTitle.
 *
 * The title was saved locally before; this workflow only sends it. It holds the group administration lock, so the
 * packets are signed for the epoch and recipients that are current when they are queued. The title is marked
 * published only while it is still the stored version; a failed send leaves it unpublished for the next attempt.
 */
class PublishGroupTitleWorkflow internal constructor(
    private val titleRepository: GroupTitleRepository,
    private val findUnpublishedGroupState: FindUnpublishedGroupStateUseCase,
    private val requireGroupAdminContext: RequireGroupAdminContextUseCase,
    private val createGroupTitlePackets: CreateGroupTitlePacketsUseCase,
    private val enqueueGroupPackets: EnqueueGroupPacketsUseCase
) : WorkflowStateMachine<
        GroupStateCommand.PublishGroupTitle,
        PublishGroupTitleWorkflow.Step,
        PublishGroupTitleWorkflow.Outcome
    > {
    override val commandType: KClass<GroupStateCommand.PublishGroupTitle> = GroupStateCommand.PublishGroupTitle::class

    override fun lockKey(command: GroupStateCommand.PublishGroupTitle): String =
        groupAdministrationLockKey(command.groupId)

    override fun firstStep(command: GroupStateCommand.PublishGroupTitle): Step = Step.FindUnpublishedTitle(command)

    override suspend fun perform(step: Step): Outcome =
        when (step) {
            is Step.FindUnpublishedTitle -> {
                require(step.command.groupId.isNotBlank()) { "Group ID must not be blank" }
                titleFound(findUnpublishedGroupState.title(step.command.groupId).getOrThrow())
            }
            is Step.AuthorizeAdmin ->
                Outcome.AdminAuthorized(
                    step.title,
                    requireGroupAdminContext(step.command.groupId, ADMIN_ACTION).getOrThrow()
                )
            is Step.CreateTitlePackets ->
                Outcome.TitlePacketsCreated(step.title, createGroupTitlePackets(step.title, step.context).getOrThrow())
            is Step.EnqueueTitlePackets -> {
                enqueueGroupPackets(step.packets, recordFailedRecipients = false).getOrThrow()
                Outcome.TitlePacketsEnqueued(step.title)
            }
            is Step.MarkTitlePublished -> {
                titleRepository.markPublished(step.command.groupId, step.title.changedAtEpochMilliseconds).getOrThrow()
                Outcome.TitlePublished
            }
        }

    override fun transition(step: Step, outcome: Outcome): Transition<Step> =
        when (outcome) {
            Outcome.NothingToPublish -> Transition.Complete()
            is Outcome.UnpublishedTitleFound -> Transition.Next(Step.AuthorizeAdmin(outcome.title, step.command))
            is Outcome.AdminAuthorized ->
                Transition.Next(Step.CreateTitlePackets(outcome.title, outcome.context, step.command))
            is Outcome.TitlePacketsCreated ->
                Transition.Next(Step.EnqueueTitlePackets(outcome.title, outcome.packets, step.command))
            is Outcome.TitlePacketsEnqueued -> Transition.Next(Step.MarkTitlePublished(outcome.title, step.command))
            Outcome.TitlePublished -> Transition.Complete()
        }

    private fun titleFound(title: GroupTitleState?): Outcome =
        title?.let(Outcome::UnpublishedTitleFound) ?: Outcome.NothingToPublish

    sealed interface Step : WorkflowStep {
        val command: GroupStateCommand.PublishGroupTitle

        data class FindUnpublishedTitle(
            override val command: GroupStateCommand.PublishGroupTitle
        ) : Step

        data class AuthorizeAdmin(
            val title: GroupTitleState,
            override val command: GroupStateCommand.PublishGroupTitle
        ) : Step

        data class CreateTitlePackets(
            val title: GroupTitleState,
            val context: GroupAdminContext,
            override val command: GroupStateCommand.PublishGroupTitle
        ) : Step

        data class EnqueueTitlePackets(
            val title: GroupTitleState,
            val packets: Map<String, GroupTitleUpdatedPacket>,
            override val command: GroupStateCommand.PublishGroupTitle
        ) : Step

        data class MarkTitlePublished(
            val title: GroupTitleState,
            override val command: GroupStateCommand.PublishGroupTitle
        ) : Step
    }

    sealed interface Outcome {
        data object NothingToPublish : Outcome

        data class UnpublishedTitleFound(
            val title: GroupTitleState
        ) : Outcome

        data class AdminAuthorized(
            val title: GroupTitleState,
            val context: GroupAdminContext
        ) : Outcome

        data class TitlePacketsCreated(
            val title: GroupTitleState,
            val packets: Map<String, GroupTitleUpdatedPacket>
        ) : Outcome

        data class TitlePacketsEnqueued(
            val title: GroupTitleState
        ) : Outcome

        data object TitlePublished : Outcome
    }

    private companion object {
        const val ADMIN_ACTION = "group title"
    }
}
