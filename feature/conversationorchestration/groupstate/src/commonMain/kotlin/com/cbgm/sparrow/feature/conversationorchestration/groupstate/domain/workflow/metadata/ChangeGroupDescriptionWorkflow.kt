package com.cbgm.sparrow.feature.conversationorchestration.groupstate.domain.workflow.metadata

import com.cbgm.sparrow.core.base.group.domain.model.GroupMetadataPolicy
import com.cbgm.sparrow.core.util.time.SystemClock
import com.cbgm.sparrow.feature.conversationorchestration.api.command.GroupAdministrationCommand
import com.cbgm.sparrow.feature.conversationorchestration.engine.machine.ConversationWorkflow
import com.cbgm.sparrow.feature.conversationorchestration.engine.machine.Transition
import com.cbgm.sparrow.feature.conversationorchestration.engine.machine.WorkflowStep
import com.cbgm.sparrow.feature.conversationorchestration.groupstate.domain.model.GroupAdminContext
import com.cbgm.sparrow.feature.conversationorchestration.groupstate.domain.model.GroupDescriptionState
import com.cbgm.sparrow.feature.conversationorchestration.groupstate.domain.repository.GroupDescriptionRepository
import com.cbgm.sparrow.feature.conversationorchestration.groupstate.domain.usecase.CreateGroupDescriptionPacketsUseCase
import com.cbgm.sparrow.feature.conversationorchestration.groupstate.domain.usecase.RequireGroupAdminContextUseCase
import com.cbgm.sparrow.feature.conversationorchestration.message.domain.usecase.EnqueueGroupPacketsUseCase
import com.cbgm.sparrow.protocol.packet.GroupDescriptionUpdatedPacket
import kotlin.reflect.KClass

/** AuthorizeAdmin -> LoadDescription -> SaveDescription -> CreateDescriptionPackets -> EnqueueDescriptionPackets. An unchanged description stops after LoadDescription. */
class ChangeGroupDescriptionWorkflow internal constructor(
    private val descriptionRepository: GroupDescriptionRepository,
    private val requireGroupAdminContext: RequireGroupAdminContextUseCase,
    private val createGroupDescriptionPackets: CreateGroupDescriptionPacketsUseCase,
    private val enqueueGroupPackets: EnqueueGroupPacketsUseCase
) : ConversationWorkflow<
        GroupAdministrationCommand.ChangeDescription,
        ChangeGroupDescriptionWorkflow.Step,
        ChangeGroupDescriptionWorkflow.Outcome
    > {
    override val commandType: KClass<GroupAdministrationCommand.ChangeDescription> =
        GroupAdministrationCommand.ChangeDescription::class

    override fun lockKey(command: GroupAdministrationCommand.ChangeDescription): String =
        groupMetadataLockKey(command.groupId)

    override fun firstStep(command: GroupAdministrationCommand.ChangeDescription): Step = Step.AuthorizeAdmin(command)

    override suspend fun perform(step: Step): Outcome =
        when (step) {
            is Step.AuthorizeAdmin -> {
                require(step.command.groupId.isNotBlank()) { "Group ID must not be blank" }
                val normalized = step.command.normalizedDescription
                require(normalized == null || normalized.length <= GroupMetadataPolicy.MAX_DESCRIPTION_LENGTH) {
                    "Group description must not exceed ${GroupMetadataPolicy.MAX_DESCRIPTION_LENGTH} characters"
                }
                Outcome.AdminAuthorized(requireGroupAdminContext(step.command.groupId, ADMIN_ACTION).getOrThrow())
            }
            is Step.LoadDescription -> {
                val current = descriptionRepository.get(step.command.groupId).getOrThrow()
                if (current.description == step.command.normalizedDescription) {
                    Outcome.DescriptionUnchanged
                } else {
                    Outcome.DescriptionLoaded(current, step.context)
                }
            }
            is Step.SaveDescription -> {
                val description =
                    GroupDescriptionState(
                        groupId = step.command.groupId,
                        description = step.command.normalizedDescription,
                        changedAtEpochMilliseconds =
                            maxOf(SystemClock.nowEpochMilliseconds(), step.previous.changedAtEpochMilliseconds + 1L)
                    )
                descriptionRepository.save(description).getOrThrow()
                Outcome.DescriptionSaved(description, step.context)
            }
            is Step.CreateDescriptionPackets ->
                Outcome.DescriptionPacketsCreated(
                    createGroupDescriptionPackets(step.description, step.context).getOrThrow()
                )
            is Step.EnqueueDescriptionPackets -> {
                enqueueGroupPackets(step.packets, recordFailedRecipients = false).getOrThrow()
                Outcome.DescriptionPacketsEnqueued
            }
        }

    override fun transition(step: Step, outcome: Outcome): Transition<Step> =
        when (outcome) {
            is Outcome.AdminAuthorized -> Transition.Next(Step.LoadDescription(outcome.context, step.command))
            Outcome.DescriptionUnchanged -> Transition.Complete()
            is Outcome.DescriptionLoaded ->
                Transition.Next(Step.SaveDescription(outcome.previous, outcome.context, step.command))
            is Outcome.DescriptionSaved ->
                Transition.Next(Step.CreateDescriptionPackets(outcome.description, outcome.context, step.command))
            is Outcome.DescriptionPacketsCreated ->
                Transition.Next(Step.EnqueueDescriptionPackets(outcome.packets, step.command))
            Outcome.DescriptionPacketsEnqueued -> Transition.Complete()
        }

    private val GroupAdministrationCommand.ChangeDescription.normalizedDescription: String?
        get() = description.trim().takeIf(String::isNotEmpty)

    sealed interface Step : WorkflowStep {
        val command: GroupAdministrationCommand.ChangeDescription

        data class AuthorizeAdmin(
            override val command: GroupAdministrationCommand.ChangeDescription
        ) : Step

        data class LoadDescription(
            val context: GroupAdminContext,
            override val command: GroupAdministrationCommand.ChangeDescription
        ) : Step

        data class SaveDescription(
            val previous: GroupDescriptionState,
            val context: GroupAdminContext,
            override val command: GroupAdministrationCommand.ChangeDescription
        ) : Step

        data class CreateDescriptionPackets(
            val description: GroupDescriptionState,
            val context: GroupAdminContext,
            override val command: GroupAdministrationCommand.ChangeDescription
        ) : Step

        data class EnqueueDescriptionPackets(
            val packets: Map<String, GroupDescriptionUpdatedPacket>,
            override val command: GroupAdministrationCommand.ChangeDescription
        ) : Step
    }

    sealed interface Outcome {
        data class AdminAuthorized(
            val context: GroupAdminContext
        ) : Outcome

        data object DescriptionUnchanged : Outcome

        data class DescriptionLoaded(
            val previous: GroupDescriptionState,
            val context: GroupAdminContext
        ) : Outcome

        data class DescriptionSaved(
            val description: GroupDescriptionState,
            val context: GroupAdminContext
        ) : Outcome

        data class DescriptionPacketsCreated(
            val packets: Map<String, GroupDescriptionUpdatedPacket>
        ) : Outcome

        data object DescriptionPacketsEnqueued : Outcome
    }

    private companion object {
        const val ADMIN_ACTION = "group description"
    }
}
