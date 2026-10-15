package com.cbgm.sparrow.feature.conversationorchestration.groupstate.domain.workflow.expense

import com.cbgm.sparrow.core.id.IdGenerator
import com.cbgm.sparrow.core.messagepart.domain.model.Expense
import com.cbgm.sparrow.core.messagepart.domain.model.ExpensePolicy
import com.cbgm.sparrow.core.messagepart.domain.model.expenseId
import com.cbgm.sparrow.core.time.SystemClock
import com.cbgm.sparrow.feature.conversationorchestration.api.command.GroupMessageCommand
import com.cbgm.sparrow.feature.conversationorchestration.api.model.ConversationResult
import com.cbgm.sparrow.feature.conversationorchestration.engine.machine.ConversationWorkflow
import com.cbgm.sparrow.feature.conversationorchestration.engine.machine.Transition
import com.cbgm.sparrow.feature.conversationorchestration.engine.machine.WorkflowStep
import com.cbgm.sparrow.feature.conversationorchestration.engine.runner.WorkflowEngine
import com.cbgm.sparrow.feature.conversationorchestration.groupstate.domain.model.GroupExpenseMembers
import com.cbgm.sparrow.feature.conversationorchestration.groupstate.domain.usecase.FindGroupExpenseMembersUseCase
import com.cbgm.sparrow.feature.conversationorchestration.message.domain.usecase.DeleteLocalExpenseReceiptsUseCase
import kotlin.reflect.KClass

/** FindExpenseMembers -> MarkExpenseDeleted -> SendExpense -> DeleteReceipts. */
class DeleteGroupExpenseWorkflow internal constructor(
    private val engine: WorkflowEngine,
    private val findGroupExpenseMembers: FindGroupExpenseMembersUseCase,
    private val deleteLocalExpenseReceipts: DeleteLocalExpenseReceiptsUseCase
) : ConversationWorkflow<
        GroupMessageCommand.DeleteExpense,
        DeleteGroupExpenseWorkflow.Step,
        DeleteGroupExpenseWorkflow.Outcome
    > {
    override val commandType: KClass<GroupMessageCommand.DeleteExpense> = GroupMessageCommand.DeleteExpense::class

    override fun firstStep(command: GroupMessageCommand.DeleteExpense): Step = Step.FindExpenseMembers(command)

    override suspend fun perform(step: Step): Outcome =
        when (step) {
            is Step.FindExpenseMembers ->
                Outcome.ExpenseMembersFound(findGroupExpenseMembers(step.command.groupId).getOrThrow())
            is Step.MarkExpenseDeleted -> Outcome.ExpenseMarkedDeleted(markDeleted(step.members, step.command.expense))
            is Step.SendExpense -> {
                val result =
                    engine.run(GroupMessageCommand.SendMessage(step.command.groupId, listOf(step.expense)))
                        .getOrThrow()
                Outcome.ExpenseSent(result as ConversationResult.GroupMessageSent)
            }
            is Step.DeleteReceipts -> {
                deleteLocalExpenseReceipts.forExpense(step.command.groupId, step.command.expense.expenseId)
                    .getOrThrow()
                Outcome.ReceiptsDeleted(step.result)
            }
        }

    override fun transition(step: Step, outcome: Outcome): Transition<Step> =
        when (outcome) {
            is Outcome.ExpenseMembersFound -> Transition.Next(Step.MarkExpenseDeleted(outcome.members, step.command))
            is Outcome.ExpenseMarkedDeleted -> Transition.Next(Step.SendExpense(outcome.expense, step.command))
            is Outcome.ExpenseSent -> Transition.Next(Step.DeleteReceipts(outcome.result, step.command))
            is Outcome.ReceiptsDeleted -> Transition.Complete(outcome.result)
        }

    private fun markDeleted(members: GroupExpenseMembers, expense: Expense): Expense {
        check(members.board.id == expense.boardId) { "Expense board has changed" }
        check(expense.deletedAtEpochMilliseconds == null) { "Expense is already deleted" }
        check(members.isLocalAdmin || expense.createdByMemberId == members.localMemberId) {
            "Only the creator or an admin can delete this expense"
        }
        val deleted = expense.copy(
            id = IdGenerator.generate(prefix = "expense"),
            revisionOf = expense.expenseId,
            receipt = null,
            editedAtEpochMilliseconds = null,
            deletedAtEpochMilliseconds = SystemClock.nowEpochMilliseconds()
        )
        ExpensePolicy.requireValid(deleted)
        return deleted
    }

    sealed interface Step : WorkflowStep {
        val command: GroupMessageCommand.DeleteExpense

        data class FindExpenseMembers(
            override val command: GroupMessageCommand.DeleteExpense
        ) : Step

        data class MarkExpenseDeleted(
            val members: GroupExpenseMembers,
            override val command: GroupMessageCommand.DeleteExpense
        ) : Step

        data class SendExpense(
            val expense: Expense,
            override val command: GroupMessageCommand.DeleteExpense
        ) : Step

        data class DeleteReceipts(
            val result: ConversationResult.GroupMessageSent,
            override val command: GroupMessageCommand.DeleteExpense
        ) : Step
    }

    sealed interface Outcome {
        data class ExpenseMembersFound(
            val members: GroupExpenseMembers
        ) : Outcome

        data class ExpenseMarkedDeleted(
            val expense: Expense
        ) : Outcome

        data class ExpenseSent(
            val result: ConversationResult.GroupMessageSent
        ) : Outcome

        data class ReceiptsDeleted(
            val result: ConversationResult.GroupMessageSent
        ) : Outcome
    }
}
