package com.cbgm.sparrow.feature.conversationorchestration.groupstate.domain.workflow.metadata

import com.cbgm.sparrow.core.base.group.domain.model.GroupMetadataPolicy
import com.cbgm.sparrow.feature.conversationorchestration.api.command.GroupAdministrationCommand
import com.cbgm.sparrow.feature.conversationorchestration.engine.machine.ConversationWorkflow
import com.cbgm.sparrow.feature.conversationorchestration.engine.machine.Transition
import com.cbgm.sparrow.feature.conversationorchestration.engine.machine.WorkflowStep
import com.cbgm.sparrow.feature.conversationorchestration.groupstate.domain.model.GroupAdminContext
import com.cbgm.sparrow.feature.conversationorchestration.groupstate.domain.model.GroupDescriptionState
import com.cbgm.sparrow.feature.conversationorchestration.groupstate.domain.usecase.CreateGroupDescriptionPacketsUseCase
import com.cbgm.sparrow.feature.conversationorchestration.groupstate.domain.usecase.RequireGroupAdminContextUseCase
import com.cbgm.sparrow.feature.conversationorchestration.groupstate.domain.usecase.SaveGroupDescriptionUseCase
import com.cbgm.sparrow.feature.conversationorchestration.message.domain.usecase.EnqueueGroupPacketsUseCase
import com.cbgm.sparrow.protocol.packet.GroupDescriptionUpdatedPacket
import kotlin.reflect.KClass

/**
 * AuthorizeAdmin -> SaveDescription -> CreateDescriptionPackets -> EnqueueDescriptionPackets.
 * An unchanged description stops after SaveDescription.
 */
class ChangeGroupDescriptionWorkflow internal constructor(
    private val requireGroupAdminContext: RequireGroupAdminContextUseCase,
    private val saveGroupDescription: SaveGroupDescriptionUseCase,
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
                GroupMetadataPolicy.requireValidDescription(step.command.description)
                Outcome.AdminAuthorized(requireGroupAdminContext(step.command.groupId, ADMIN_ACTION).getOrThrow())
            }
            is Step.SaveDescription ->
                descriptionSaved(
                    saveGroupDescription(step.command.groupId, step.command.description).getOrThrow(),
                    step.context
                )
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
            is Outcome.AdminAuthorized -> Transition.Next(Step.SaveDescription(outcome.context, step.command))
            Outcome.DescriptionUnchanged -> Transition.Complete()
            is Outcome.DescriptionSaved ->
                Transition.Next(Step.CreateDescriptionPackets(outcome.description, outcome.context, step.command))
            is Outcome.DescriptionPacketsCreated ->
                Transition.Next(Step.EnqueueDescriptionPackets(outcome.packets, step.command))
            Outcome.DescriptionPacketsEnqueued -> Transition.Complete()
        }

    private fun descriptionSaved(description: GroupDescriptionState?, context: GroupAdminContext): Outcome =
        description?.let { saved -> Outcome.DescriptionSaved(saved, context) } ?: Outcome.DescriptionUnchanged

    sealed interface Step : WorkflowStep {
        val command: GroupAdministrationCommand.ChangeDescription

        data class AuthorizeAdmin(
            override val command: GroupAdministrationCommand.ChangeDescription
        ) : Step

        data class SaveDescription(
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
