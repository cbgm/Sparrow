package com.cbgm.sparrow.feature.conversationorchestration.groupstate.domain.workflow.expense

import com.cbgm.sparrow.core.id.IdGenerator
import com.cbgm.sparrow.core.messagepart.domain.model.Expense
import com.cbgm.sparrow.core.messagepart.domain.model.ExpensePolicy
import com.cbgm.sparrow.core.time.SystemClock
import com.cbgm.sparrow.feature.conversationorchestration.api.command.GroupMessageCommand
import com.cbgm.sparrow.feature.conversationorchestration.api.model.ConversationResult
import com.cbgm.sparrow.feature.conversationorchestration.engine.machine.ConversationWorkflow
import com.cbgm.sparrow.feature.conversationorchestration.engine.machine.Transition
import com.cbgm.sparrow.feature.conversationorchestration.engine.machine.WorkflowStep
import com.cbgm.sparrow.feature.conversationorchestration.engine.runner.WorkflowEngine
import com.cbgm.sparrow.feature.conversationorchestration.groupstate.domain.model.GroupExpenseMembers
import com.cbgm.sparrow.feature.conversationorchestration.groupstate.domain.usecase.FindGroupExpenseMembersUseCase
import kotlin.reflect.KClass

/** FindExpenseMembers -> CreateExpense -> SendExpense. */
class CreateGroupExpenseWorkflow internal constructor(
    private val engine: WorkflowEngine,
    private val findGroupExpenseMembers: FindGroupExpenseMembersUseCase
) : ConversationWorkflow<
        GroupMessageCommand.CreateExpense,
        CreateGroupExpenseWorkflow.Step,
        CreateGroupExpenseWorkflow.Outcome
    > {
    override val commandType: KClass<GroupMessageCommand.CreateExpense> = GroupMessageCommand.CreateExpense::class

    override fun firstStep(command: GroupMessageCommand.CreateExpense): Step = Step.FindExpenseMembers(command)

    override suspend fun perform(step: Step): Outcome =
        when (step) {
            is Step.FindExpenseMembers ->
                Outcome.ExpenseMembersFound(findGroupExpenseMembers(step.command.groupId).getOrThrow())
            is Step.CreateExpense -> Outcome.ExpenseCreated(createExpense(step.members, step.command))
            is Step.SendExpense -> {
                val result =
                    engine.run(
                        GroupMessageCommand.SendMessage(step.command.groupId, listOf(step.expense))
                    )
                        .getOrThrow()
                Outcome.ExpenseSent(result as ConversationResult.GroupMessageSent)
            }
        }

    override fun transition(step: Step, outcome: Outcome): Transition<Step> =
        when (outcome) {
            is Outcome.ExpenseMembersFound -> Transition.Next(Step.CreateExpense(outcome.members, step.command))
            is Outcome.ExpenseCreated -> Transition.Next(Step.SendExpense(outcome.expense, step.command))
            is Outcome.ExpenseSent -> Transition.Complete(outcome.result)
        }

    private fun createExpense(members: GroupExpenseMembers, command: GroupMessageCommand.CreateExpense): Expense {
        check(members.board.id == command.boardId) { "Expense board has changed" }
        check(command.paidByMemberId in members.memberIds) { "Payer is no longer a group member" }
        check(command.allocations.all { it.memberId in members.memberIds }) { "Expense includes a former group member" }
        val expense =
            Expense(
                id = IdGenerator.generate(prefix = "expense"),
                boardId = command.boardId,
                description = command.description.trim(),
                amountMinor = command.amountMinor,
                currencyCode = members.board.currencyCode,
                paidByMemberId = command.paidByMemberId,
                allocations = command.allocations.sortedBy { it.memberId },
                occurredAtEpochMilliseconds = SystemClock.nowEpochMilliseconds(),
                category = command.category,
                receipt = command.receipt,
                createdByMemberId = members.localMemberId,
                conversion = command.conversion
            )
        ExpensePolicy.requireValid(expense)
        return expense
    }

    sealed interface Step : WorkflowStep {
        val command: GroupMessageCommand.CreateExpense

        data class FindExpenseMembers(
            override val command: GroupMessageCommand.CreateExpense
        ) : Step

        data class CreateExpense(
            val members: GroupExpenseMembers,
            override val command: GroupMessageCommand.CreateExpense
        ) : Step

        data class SendExpense(
            val expense: Expense,
            override val command: GroupMessageCommand.CreateExpense
        ) : Step
    }

    sealed interface Outcome {
        data class ExpenseMembersFound(
            val members: GroupExpenseMembers
        ) : Outcome

        data class ExpenseCreated(
            val expense: Expense
        ) : Outcome

        data class ExpenseSent(
            val result: ConversationResult.GroupMessageSent
        ) : Outcome
    }
}
