package com.cbgm.sparrow.feature.conversationorchestration.membership.domain.workflow.administration

import com.cbgm.sparrow.feature.conversationorchestration.api.command.GroupAdministrationCommand
import com.cbgm.sparrow.feature.conversationorchestration.engine.machine.ConversationWorkflow
import com.cbgm.sparrow.feature.conversationorchestration.engine.machine.Transition
import com.cbgm.sparrow.feature.conversationorchestration.engine.machine.WorkflowStep
import com.cbgm.sparrow.feature.conversationorchestration.membership.domain.repository.GroupConversationRepository
import com.cbgm.sparrow.feature.conversationorchestration.message.domain.usecase.DeleteLocalExpenseReceiptsUseCase
import com.cbgm.sparrow.feature.membership.domain.model.GroupLocalMembershipEnd
import com.cbgm.sparrow.feature.membership.domain.model.GroupMembershipContext
import com.cbgm.sparrow.feature.membership.domain.usecase.TransferGroupAdminAndLeaveUseCase
import kotlin.reflect.KClass

/** LoadContext -> TransferAdminAndLeave -> EndLocalMembership -> DeleteExpenseReceipts. */
class TransferGroupAdminAndLeaveWorkflow internal constructor(
    private val conversationRepository: GroupConversationRepository,
    private val transferGroupAdminAndLeave: TransferGroupAdminAndLeaveUseCase,
    private val deleteLocalExpenseReceipts: DeleteLocalExpenseReceiptsUseCase
) : ConversationWorkflow<
        GroupAdministrationCommand.TransferAdminAndLeave,
        TransferGroupAdminAndLeaveWorkflow.Step,
        TransferGroupAdminAndLeaveWorkflow.Outcome
    > {
    override val commandType: KClass<GroupAdministrationCommand.TransferAdminAndLeave> =
        GroupAdministrationCommand.TransferAdminAndLeave::class

    override fun lockKey(command: GroupAdministrationCommand.TransferAdminAndLeave): String =
        groupAdministrationLockKey(command.groupId)

    override fun firstStep(command: GroupAdministrationCommand.TransferAdminAndLeave): Step = Step.LoadContext(command)

    override suspend fun perform(step: Step): Outcome =
        when (step) {
            is Step.LoadContext ->
                Outcome.ContextLoaded(conversationRepository.getMembershipContext(step.command.groupId).getOrThrow())
            is Step.TransferAdminAndLeave ->
                Outcome.MembershipEnded(
                    transferGroupAdminAndLeave(step.command.groupId, step.command.contactId, step.context).getOrThrow()
                )
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
            is Outcome.ContextLoaded -> Transition.Next(Step.TransferAdminAndLeave(outcome.context, step.command))
            is Outcome.MembershipEnded -> Transition.Next(Step.EndLocalMembership(outcome.end, step.command))
            Outcome.LocalMembershipEnded -> Transition.Next(Step.DeleteExpenseReceipts(step.command))
            Outcome.ExpenseReceiptsDeleted -> Transition.Complete()
        }

    sealed interface Step : WorkflowStep {
        val command: GroupAdministrationCommand.TransferAdminAndLeave

        data class LoadContext(
            override val command: GroupAdministrationCommand.TransferAdminAndLeave
        ) : Step

        data class TransferAdminAndLeave(
            val context: GroupMembershipContext,
            override val command: GroupAdministrationCommand.TransferAdminAndLeave
        ) : Step

        data class EndLocalMembership(
            val end: GroupLocalMembershipEnd,
            override val command: GroupAdministrationCommand.TransferAdminAndLeave
        ) : Step

        data class DeleteExpenseReceipts(
            override val command: GroupAdministrationCommand.TransferAdminAndLeave
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
