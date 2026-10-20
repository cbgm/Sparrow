package com.cbgm.sparrow.feature.conversationorchestration.membership.domain.workflow.administration

import com.cbgm.sparrow.feature.attachments.domain.repository.MessageAttachmentRepository
import com.cbgm.sparrow.feature.conversationorchestration.api.command.GroupAdministrationCommand
import com.cbgm.sparrow.feature.conversationorchestration.engine.machine.ConversationWorkflow
import com.cbgm.sparrow.feature.conversationorchestration.engine.machine.Transition
import com.cbgm.sparrow.feature.conversationorchestration.engine.machine.WorkflowStep
import com.cbgm.sparrow.feature.conversationorchestration.groupstate.domain.usecase.ClearGroupStateUseCase
import com.cbgm.sparrow.feature.conversationorchestration.membership.domain.repository.GroupConversationRepository
import com.cbgm.sparrow.feature.membership.domain.model.GroupMembershipContext
import com.cbgm.sparrow.feature.membership.domain.usecase.DeleteGroupMembershipUseCase
import kotlin.reflect.KClass

/** LoadContext -> DeleteAttachments -> DeleteMembership -> DeleteLocalConversation -> ClearGroupState. */
class DeleteGroupWorkflow internal constructor(
    private val conversationRepository: GroupConversationRepository,
    private val attachmentRepository: MessageAttachmentRepository,
    private val deleteGroupMembership: DeleteGroupMembershipUseCase,
    private val clearGroupState: ClearGroupStateUseCase
) : ConversationWorkflow<
        GroupAdministrationCommand.DeleteGroup,
        DeleteGroupWorkflow.Step,
        DeleteGroupWorkflow.Outcome
    > {
    override val commandType: KClass<GroupAdministrationCommand.DeleteGroup> =
        GroupAdministrationCommand.DeleteGroup::class

    override fun lockKey(command: GroupAdministrationCommand.DeleteGroup): String =
        groupAdministrationLockKey(command.groupId)

    override fun firstStep(command: GroupAdministrationCommand.DeleteGroup): Step = Step.LoadContext(command)

    override suspend fun perform(step: Step): Outcome =
        when (step) {
            is Step.LoadContext ->
                Outcome.ContextLoaded(conversationRepository.getMembershipContext(step.command.groupId).getOrThrow())
            is Step.DeleteAttachments -> {
                attachmentRepository.deleteLocalAttachmentsForConversation(step.command.groupId).getOrThrow()
                Outcome.AttachmentsDeleted(step.context)
            }
            is Step.DeleteMembership ->
                Outcome.MembershipDeleted(deleteGroupMembership(step.command.groupId, step.context).getOrThrow())
            is Step.DeleteLocalConversation -> {
                conversationRepository.deleteLocalConversation(
                    step.command.groupId,
                    step.deletedAtEpochMilliseconds
                ).getOrThrow()
                Outcome.LocalConversationDeleted
            }
            is Step.ClearGroupState -> {
                clearGroupState(step.command.groupId).getOrThrow()
                Outcome.GroupStateCleared
            }
        }

    override fun transition(step: Step, outcome: Outcome): Transition<Step> =
        when (outcome) {
            is Outcome.ContextLoaded -> Transition.Next(Step.DeleteAttachments(outcome.context, step.command))
            is Outcome.AttachmentsDeleted -> Transition.Next(Step.DeleteMembership(outcome.context, step.command))
            is Outcome.MembershipDeleted ->
                Transition.Next(Step.DeleteLocalConversation(outcome.deletedAtEpochMilliseconds, step.command))
            Outcome.LocalConversationDeleted -> Transition.Next(Step.ClearGroupState(step.command))
            Outcome.GroupStateCleared -> Transition.Complete()
        }

    sealed interface Step : WorkflowStep {
        val command: GroupAdministrationCommand.DeleteGroup

        data class LoadContext(
            override val command: GroupAdministrationCommand.DeleteGroup
        ) : Step

        data class DeleteAttachments(
            val context: GroupMembershipContext,
            override val command: GroupAdministrationCommand.DeleteGroup
        ) : Step

        data class DeleteMembership(
            val context: GroupMembershipContext,
            override val command: GroupAdministrationCommand.DeleteGroup
        ) : Step

        data class DeleteLocalConversation(
            val deletedAtEpochMilliseconds: Long,
            override val command: GroupAdministrationCommand.DeleteGroup
        ) : Step

        data class ClearGroupState(
            override val command: GroupAdministrationCommand.DeleteGroup
        ) : Step
    }

    sealed interface Outcome {
        data class ContextLoaded(
            val context: GroupMembershipContext
        ) : Outcome

        data class AttachmentsDeleted(
            val context: GroupMembershipContext
        ) : Outcome

        data class MembershipDeleted(
            val deletedAtEpochMilliseconds: Long
        ) : Outcome

        data object LocalConversationDeleted : Outcome

        data object GroupStateCleared : Outcome
    }
}
