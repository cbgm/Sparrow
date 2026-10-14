package com.cbgm.sparrow.feature.conversationorchestration.membership.domain.workflow.administration

import com.cbgm.sparrow.feature.conversationorchestration.api.command.GroupAdministrationCommand
import com.cbgm.sparrow.feature.conversationorchestration.engine.machine.ConversationWorkflow
import com.cbgm.sparrow.feature.conversationorchestration.engine.machine.Transition
import com.cbgm.sparrow.feature.conversationorchestration.engine.machine.WorkflowStep
import com.cbgm.sparrow.feature.conversationorchestration.membership.domain.repository.GroupConversationRepository
import com.cbgm.sparrow.feature.conversationorchestration.message.domain.usecase.DeleteLocalExpenseReceiptsUseCase
import com.cbgm.sparrow.feature.membership.domain.model.GroupLocalMembershipEnd
import com.cbgm.sparrow.feature.membership.domain.model.GroupMembershipContext
import com.cbgm.sparrow.feature.membership.domain.usecase.LeaveGroupUseCase
import kotlin.reflect.KClass

/** LoadContext -> LeaveGroup -> EndLocalMembership -> DeleteExpenseReceipts. */
class LeaveGroupWorkflow internal constructor(
    private val conversationRepository: GroupConversationRepository,
    private val leaveGroup: LeaveGroupUseCase,
    private val deleteLocalExpenseReceipts: DeleteLocalExpenseReceiptsUseCase
) : ConversationWorkflow<GroupAdministrationCommand.LeaveGroup, LeaveGroupWorkflow.Step, LeaveGroupWorkflow.Outcome> {
    override val commandType: KClass<GroupAdministrationCommand.LeaveGroup> =
        GroupAdministrationCommand.LeaveGroup::class

    override fun lockKey(command: GroupAdministrationCommand.LeaveGroup): String =
        groupAdministrationLockKey(command.groupId)

    override fun firstStep(command: GroupAdministrationCommand.LeaveGroup): Step = Step.LoadContext(command)

    override suspend fun perform(step: Step): Outcome =
        when (step) {
            is Step.LoadContext ->
                Outcome.ContextLoaded(conversationRepository.getMembershipContext(step.command.groupId).getOrThrow())
            is Step.LeaveGroup -> Outcome.MembershipEnded(leaveGroup(step.command.groupId, step.context).getOrThrow())
            is Step.EndLocalMembership -> {
                conversationRepository.endLocalMembership(step.end).getOrThrow()
                Outcome.LocalMembershipEnded
            }
            is Step.DeleteExpenseReceipts -> {
                deleteLocalExpenseReceipts.forGroup(step.command.groupId).getOrThrow()
                Outcome.ExpenseReceiptsDeleted
            }
        }

    override fun transition(step: Step, outcome: Outcome): Transition<Step> =
        when (outcome) {
            is Outcome.ContextLoaded -> Transition.Next(Step.LeaveGroup(outcome.context, step.command))
            is Outcome.MembershipEnded -> Transition.Next(Step.EndLocalMembership(outcome.end, step.command))
            Outcome.LocalMembershipEnded -> Transition.Next(Step.DeleteExpenseReceipts(step.command))
            Outcome.ExpenseReceiptsDeleted -> Transition.Complete()
        }

    sealed interface Step : WorkflowStep {
        val command: GroupAdministrationCommand.LeaveGroup

        data class LoadContext(
            override val command: GroupAdministrationCommand.LeaveGroup
        ) : Step

        data class LeaveGroup(
            val context: GroupMembershipContext,
            override val command: GroupAdministrationCommand.LeaveGroup
        ) : Step

        data class EndLocalMembership(
            val end: GroupLocalMembershipEnd,
            override val command: GroupAdministrationCommand.LeaveGroup
        ) : Step

        data class DeleteExpenseReceipts(
            override val command: GroupAdministrationCommand.LeaveGroup
        ) : Step
    }

    sealed interface Outcome {
        data class ContextLoaded(
            val context: GroupMembershipContext
        ) : Outcome

        data class MembershipEnded(
            val end: GroupLocalMembershipEnd
        ) : Outcome

        data object LocalMembershipEnded : Outcome

        data object ExpenseReceiptsDeleted : Outcome
    }
}
