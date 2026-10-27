package com.cbgm.sparrow.feature.conversationorchestration.groupstate.domain.workflow.metadata

import com.cbgm.sparrow.feature.conversationorchestration.engine.machine.Transition
import com.cbgm.sparrow.feature.conversationorchestration.engine.machine.WorkflowStateMachine
import com.cbgm.sparrow.feature.conversationorchestration.engine.machine.WorkflowStep
import com.cbgm.sparrow.feature.conversationorchestration.groupstate.domain.command.GroupStateCommand
import com.cbgm.sparrow.feature.conversationorchestration.groupstate.domain.model.GroupAdminContext
import com.cbgm.sparrow.feature.conversationorchestration.groupstate.domain.model.GroupDescriptionState
import com.cbgm.sparrow.feature.conversationorchestration.groupstate.domain.repository.GroupDescriptionRepository
import com.cbgm.sparrow.feature.conversationorchestration.groupstate.domain.usecase.CreateGroupDescriptionPacketsUseCase
import com.cbgm.sparrow.feature.conversationorchestration.groupstate.domain.usecase.FindUnpublishedGroupStateUseCase
import com.cbgm.sparrow.feature.conversationorchestration.groupstate.domain.usecase.RequireGroupAdminContextUseCase
import com.cbgm.sparrow.feature.conversationorchestration.groupstate.domain.workflow.groupAdministrationLockKey
import com.cbgm.sparrow.feature.conversationorchestration.message.domain.usecase.EnqueueGroupPacketsUseCase
import com.cbgm.sparrow.protocol.packet.GroupDescriptionUpdatedPacket
import kotlin.reflect.KClass

/**
 * FindUnpublishedDescription -> AuthorizeAdmin -> CreateDescriptionPackets -> EnqueueDescriptionPackets -> MarkDescriptionPublished.
 * Without an unpublished description (published already, or replaced by a newer one from another admin) it stops
 * after FindUnpublishedDescription.
 *
 * The description was saved locally before; this workflow only sends it. It holds the group administration lock, so the
 * packets are signed for the epoch and recipients that are current when they are queued. The description is marked
 * published only while it is still the stored version; a failed send leaves it unpublished for the next attempt.
 */
class PublishGroupDescriptionWorkflow internal constructor(
    private val descriptionRepository: GroupDescriptionRepository,
    private val findUnpublishedGroupState: FindUnpublishedGroupStateUseCase,
    private val requireGroupAdminContext: RequireGroupAdminContextUseCase,
    private val createGroupDescriptionPackets: CreateGroupDescriptionPacketsUseCase,
    private val enqueueGroupPackets: EnqueueGroupPacketsUseCase
) : WorkflowStateMachine<
        GroupStateCommand.PublishGroupDescription,
        PublishGroupDescriptionWorkflow.Step,
        PublishGroupDescriptionWorkflow.Outcome
    > {
    override val commandType: KClass<GroupStateCommand.PublishGroupDescription> = GroupStateCommand.PublishGroupDescription::class

    override fun lockKey(command: GroupStateCommand.PublishGroupDescription): String =
        groupAdministrationLockKey(command.groupId)

    override fun firstStep(command: GroupStateCommand.PublishGroupDescription): Step = Step.FindUnpublishedDescription(command)

    override suspend fun perform(step: Step): Outcome =
        when (step) {
            is Step.FindUnpublishedDescription -> {
                require(step.command.groupId.isNotBlank()) { "Group ID must not be blank" }
                descriptionFound(findUnpublishedGroupState.description(step.command.groupId).getOrThrow())
            }
            is Step.AuthorizeAdmin ->
                Outcome.AdminAuthorized(
                    step.description,
                    requireGroupAdminContext(step.command.groupId, ADMIN_ACTION).getOrThrow()
                )
            is Step.CreateDescriptionPackets ->
                Outcome.DescriptionPacketsCreated(step.description, createGroupDescriptionPackets(step.description, step.context).getOrThrow())
            is Step.EnqueueDescriptionPackets -> {
                enqueueGroupPackets(step.packets, recordFailedRecipients = false).getOrThrow()
                Outcome.DescriptionPacketsEnqueued(step.description)
            }
            is Step.MarkDescriptionPublished -> {
                descriptionRepository.markPublished(step.command.groupId, step.description.changedAtEpochMilliseconds).getOrThrow()
                Outcome.DescriptionPublished
            }
        }

    override fun transition(step: Step, outcome: Outcome): Transition<Step> =
        when (outcome) {
            Outcome.NothingToPublish -> Transition.Complete()
            is Outcome.UnpublishedDescriptionFound -> Transition.Next(Step.AuthorizeAdmin(outcome.description, step.command))
            is Outcome.AdminAuthorized ->
                Transition.Next(Step.CreateDescriptionPackets(outcome.description, outcome.context, step.command))
            is Outcome.DescriptionPacketsCreated ->
                Transition.Next(Step.EnqueueDescriptionPackets(outcome.description, outcome.packets, step.command))
            is Outcome.DescriptionPacketsEnqueued -> Transition.Next(Step.MarkDescriptionPublished(outcome.description, step.command))
            Outcome.DescriptionPublished -> Transition.Complete()
        }

    private fun descriptionFound(description: GroupDescriptionState?): Outcome =
        description?.let(Outcome::UnpublishedDescriptionFound) ?: Outcome.NothingToPublish

    sealed interface Step : WorkflowStep {
        val command: GroupStateCommand.PublishGroupDescription

        data class FindUnpublishedDescription(
            override val command: GroupStateCommand.PublishGroupDescription
        ) : Step

        data class AuthorizeAdmin(
            val description: GroupDescriptionState,
            override val command: GroupStateCommand.PublishGroupDescription
        ) : Step

        data class CreateDescriptionPackets(
            val description: GroupDescriptionState,
            val context: GroupAdminContext,
            override val command: GroupStateCommand.PublishGroupDescription
        ) : Step

        data class EnqueueDescriptionPackets(
            val description: GroupDescriptionState,
            val packets: Map<String, GroupDescriptionUpdatedPacket>,
            override val command: GroupStateCommand.PublishGroupDescription
        ) : Step

        data class MarkDescriptionPublished(
            val description: GroupDescriptionState,
            override val command: GroupStateCommand.PublishGroupDescription
        ) : Step
    }

    sealed interface Outcome {
        data object NothingToPublish : Outcome

        data class UnpublishedDescriptionFound(
            val description: GroupDescriptionState
        ) : Outcome

        data class AdminAuthorized(
            val description: GroupDescriptionState,
            val context: GroupAdminContext
        ) : Outcome

        data class DescriptionPacketsCreated(
            val description: GroupDescriptionState,
            val packets: Map<String, GroupDescriptionUpdatedPacket>
        ) : Outcome

        data class DescriptionPacketsEnqueued(
            val description: GroupDescriptionState
        ) : Outcome

        data object DescriptionPublished : Outcome
    }

    private companion object {
        const val ADMIN_ACTION = "group description"
    }
}
