package com.cbgm.sparrow.feature.conversationorchestration.groupstate.domain.workflow.expense

import com.cbgm.sparrow.core.base.messagepart.domain.model.Expense
import com.cbgm.sparrow.core.base.messagepart.domain.model.expenseId
import com.cbgm.sparrow.feature.conversationorchestration.api.command.GroupMessageCommand
import com.cbgm.sparrow.feature.conversationorchestration.api.model.ConversationResult
import com.cbgm.sparrow.feature.conversationorchestration.engine.machine.ConversationWorkflow
import com.cbgm.sparrow.feature.conversationorchestration.engine.machine.Transition
import com.cbgm.sparrow.feature.conversationorchestration.engine.machine.WorkflowStep
import com.cbgm.sparrow.feature.conversationorchestration.engine.runner.WorkflowEngine
import com.cbgm.sparrow.feature.conversationorchestration.groupstate.domain.usecase.PrepareGroupExpenseDeletionUseCase
import com.cbgm.sparrow.feature.conversationorchestration.message.domain.usecase.DeleteLocalExpenseReceiptsUseCase
import kotlin.reflect.KClass

/** PrepareDeletion -> SendExpense -> DeleteReceipts. */
class DeleteGroupExpenseWorkflow internal constructor(
    private val engine: WorkflowEngine,
    private val prepareGroupExpenseDeletion: PrepareGroupExpenseDeletionUseCase,
    private val deleteLocalExpenseReceipts: DeleteLocalExpenseReceiptsUseCase
) : ConversationWorkflow<
        GroupMessageCommand.DeleteExpense,
        DeleteGroupExpenseWorkflow.Step,
        DeleteGroupExpenseWorkflow.Outcome
    > {
    override val commandType: KClass<GroupMessageCommand.DeleteExpense> = GroupMessageCommand.DeleteExpense::class

    override fun firstStep(command: GroupMessageCommand.DeleteExpense): Step = Step.PrepareDeletion(command)

    override suspend fun perform(step: Step): Outcome =
        when (step) {
            is Step.PrepareDeletion ->
                Outcome.DeletionPrepared(
                    prepareGroupExpenseDeletion(step.command.groupId, step.command.expense).getOrThrow()
                )
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
            is Outcome.DeletionPrepared -> Transition.Next(Step.SendExpense(outcome.expense, step.command))
            is Outcome.ExpenseSent -> Transition.Next(Step.DeleteReceipts(outcome.result, step.command))
            is Outcome.ReceiptsDeleted -> Transition.Complete(outcome.result)
        }

    sealed interface Step : WorkflowStep {
        val command: GroupMessageCommand.DeleteExpense

        data class PrepareDeletion(
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
        data class DeletionPrepared(
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
