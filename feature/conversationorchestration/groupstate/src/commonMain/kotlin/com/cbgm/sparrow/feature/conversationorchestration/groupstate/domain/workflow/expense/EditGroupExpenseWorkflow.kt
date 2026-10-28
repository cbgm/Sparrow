package com.cbgm.sparrow.feature.conversationorchestration.groupstate.domain.workflow.expense

import com.cbgm.sparrow.core.base.messagepart.domain.model.Expense
import com.cbgm.sparrow.core.base.messagepart.domain.model.expenseId
import com.cbgm.sparrow.feature.conversationorchestration.api.command.GroupMessageCommand
import com.cbgm.sparrow.feature.conversationorchestration.api.model.ConversationResult
import com.cbgm.sparrow.feature.conversationorchestration.engine.machine.ConversationWorkflow
import com.cbgm.sparrow.feature.conversationorchestration.engine.machine.Transition
import com.cbgm.sparrow.feature.conversationorchestration.engine.machine.WorkflowStep
import com.cbgm.sparrow.feature.conversationorchestration.engine.runner.WorkflowEngine
import com.cbgm.sparrow.feature.conversationorchestration.groupstate.domain.usecase.PrepareGroupExpenseRevisionUseCase
import com.cbgm.sparrow.feature.conversationorchestration.message.domain.usecase.DeleteLocalExpenseReceiptsUseCase
import kotlin.reflect.KClass

/** PrepareRevision -> SendExpense -> DeleteReplacedReceipts. */
class EditGroupExpenseWorkflow internal constructor(
    private val engine: WorkflowEngine,
    private val prepareGroupExpenseRevision: PrepareGroupExpenseRevisionUseCase,
    private val deleteLocalExpenseReceipts: DeleteLocalExpenseReceiptsUseCase
) : ConversationWorkflow<
        GroupMessageCommand.EditExpense,
        EditGroupExpenseWorkflow.Step,
        EditGroupExpenseWorkflow.Outcome
    > {
    override val commandType: KClass<GroupMessageCommand.EditExpense> = GroupMessageCommand.EditExpense::class

    override fun firstStep(command: GroupMessageCommand.EditExpense): Step = Step.PrepareRevision(command)

    override suspend fun perform(step: Step): Outcome =
        when (step) {
            is Step.PrepareRevision ->
                Outcome.RevisionPrepared(
                    prepareGroupExpenseRevision(step.command.groupId, step.command.expense).getOrThrow()
                )
            is Step.SendExpense -> {
                val result =
                    engine.run(GroupMessageCommand.SendMessage(step.command.groupId, listOf(step.expense)))
                        .getOrThrow()
                Outcome.ExpenseSent(result as ConversationResult.GroupMessageSent, step.expense)
            }
            is Step.DeleteReplacedReceipts -> {
                deleteLocalExpenseReceipts.forExpense(
                    groupId = step.command.groupId,
                    expenseId = step.expense.expenseId,
                    keepReceiptId = step.expense.receipt?.id
                ).getOrThrow()
                Outcome.ReplacedReceiptsDeleted(step.result)
            }
        }

    override fun transition(step: Step, outcome: Outcome): Transition<Step> =
        when (outcome) {
            is Outcome.RevisionPrepared -> Transition.Next(Step.SendExpense(outcome.expense, step.command))
            is Outcome.ExpenseSent ->
                Transition.Next(Step.DeleteReplacedReceipts(outcome.result, outcome.expense, step.command))
            is Outcome.ReplacedReceiptsDeleted -> Transition.Complete(outcome.result)
        }

    sealed interface Step : WorkflowStep {
        val command: GroupMessageCommand.EditExpense

        data class PrepareRevision(
            override val command: GroupMessageCommand.EditExpense
        ) : Step

        data class SendExpense(
            val expense: Expense,
            override val command: GroupMessageCommand.EditExpense
        ) : Step

        data class DeleteReplacedReceipts(
            val result: ConversationResult.GroupMessageSent,
            val expense: Expense,
            override val command: GroupMessageCommand.EditExpense
        ) : Step
    }

    sealed interface Outcome {
        data class RevisionPrepared(
            val expense: Expense
        ) : Outcome

        data class ExpenseSent(
            val result: ConversationResult.GroupMessageSent,
            val expense: Expense
        ) : Outcome

        data class ReplacedReceiptsDeleted(
            val result: ConversationResult.GroupMessageSent
        ) : Outcome
    }
}
