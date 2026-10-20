package com.cbgm.sparrow.feature.conversationorchestration.groupstate.domain.workflow.expense

import com.cbgm.sparrow.core.base.messagepart.domain.model.Expense
import com.cbgm.sparrow.core.base.messagepart.domain.model.ExpensePolicy
import com.cbgm.sparrow.core.base.messagepart.domain.model.Image
import com.cbgm.sparrow.core.base.messagepart.domain.model.expenseId
import com.cbgm.sparrow.core.util.id.IdGenerator
import com.cbgm.sparrow.core.util.time.SystemClock
import com.cbgm.sparrow.feature.attachments.domain.repository.MessageAttachmentOperationsRepository
import com.cbgm.sparrow.feature.attachments.domain.repository.MessageAttachmentRepository
import com.cbgm.sparrow.feature.conversationorchestration.api.command.GroupMessageCommand
import com.cbgm.sparrow.feature.conversationorchestration.api.model.ConversationResult
import com.cbgm.sparrow.feature.conversationorchestration.engine.machine.ConversationWorkflow
import com.cbgm.sparrow.feature.conversationorchestration.engine.machine.Transition
import com.cbgm.sparrow.feature.conversationorchestration.engine.machine.WorkflowStep
import com.cbgm.sparrow.feature.conversationorchestration.engine.runner.WorkflowEngine
import com.cbgm.sparrow.feature.conversationorchestration.groupstate.domain.model.GroupExpenseMembers
import com.cbgm.sparrow.feature.conversationorchestration.groupstate.domain.usecase.FindGroupExpenseMembersUseCase
import com.cbgm.sparrow.feature.conversationorchestration.message.domain.usecase.DeleteLocalExpenseReceiptsUseCase
import kotlinx.coroutines.flow.first
import kotlin.reflect.KClass

/** FindExpenseMembers -> ReviseExpense -> SendExpense -> DeleteReplacedReceipts. */
class EditGroupExpenseWorkflow internal constructor(
    private val engine: WorkflowEngine,
    private val findGroupExpenseMembers: FindGroupExpenseMembersUseCase,
    private val attachmentOperations: MessageAttachmentOperationsRepository,
    private val attachmentRepository: MessageAttachmentRepository,
    private val deleteLocalExpenseReceipts: DeleteLocalExpenseReceiptsUseCase
) : ConversationWorkflow<
        GroupMessageCommand.EditExpense,
        EditGroupExpenseWorkflow.Step,
        EditGroupExpenseWorkflow.Outcome
    > {
    override val commandType: KClass<GroupMessageCommand.EditExpense> = GroupMessageCommand.EditExpense::class

    override fun firstStep(command: GroupMessageCommand.EditExpense): Step = Step.FindExpenseMembers(command)

    override suspend fun perform(step: Step): Outcome =
        when (step) {
            is Step.FindExpenseMembers ->
                Outcome.ExpenseMembersFound(findGroupExpenseMembers(step.command.groupId).getOrThrow())
            is Step.ReviseExpense ->
                Outcome.ExpenseRevised(reviseExpense(step.members, step.command.groupId, step.command.expense))
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
            is Outcome.ExpenseMembersFound -> Transition.Next(Step.ReviseExpense(outcome.members, step.command))
            is Outcome.ExpenseRevised -> Transition.Next(Step.SendExpense(outcome.expense, step.command))
            is Outcome.ExpenseSent ->
                Transition.Next(Step.DeleteReplacedReceipts(outcome.result, outcome.expense, step.command))
            is Outcome.ReplacedReceiptsDeleted -> Transition.Complete(outcome.result)
        }

    private suspend fun reviseExpense(
        members: GroupExpenseMembers,
        groupId: String,
        expense: Expense
    ): Expense {
        check(members.board.id == expense.boardId) { "Expense board has changed" }
        check(expense.deletedAtEpochMilliseconds == null) { "A deleted expense cannot be edited" }
        check(members.isLocalAdmin || expense.createdByMemberId == members.localMemberId) {
            "Only the creator or an admin can edit this expense"
        }
        check(expense.paidByMemberId in members.memberIds) { "Payer is no longer a group member" }
        check(expense.allocations.all { it.memberId in members.memberIds }) { "Expense includes a former group member" }
        val revised = expense.copy(
            id = IdGenerator.generate(prefix = "expense"),
            revisionOf = expense.expenseId,
            allocations = expense.allocations.sortedBy { it.memberId },
            receipt = expense.receipt?.let { receipt -> receiptForRevision(groupId, receipt) },
            editedAtEpochMilliseconds = SystemClock.nowEpochMilliseconds()
        )
        ExpensePolicy.requireValid(revised)
        return revised
    }

    private suspend fun receiptForRevision(groupId: String, receipt: Image): Image {
        val stored = attachmentOperations.observeExpenses(groupId).first()
        if (stored.none { it.receipt?.id == receipt.id }) return receipt
        val localFilePath =
            receipt.localFilePath ?: attachmentRepository.loadLocalFile(receipt.id, groupId).getOrThrow()
        return receipt.copy(id = IdGenerator.generate(prefix = RECEIPT_ID_PREFIX), localFilePath = localFilePath)
    }

    sealed interface Step : WorkflowStep {
        val command: GroupMessageCommand.EditExpense

        data class FindExpenseMembers(
            override val command: GroupMessageCommand.EditExpense
        ) : Step

        data class ReviseExpense(
            val members: GroupExpenseMembers,
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
        data class ExpenseMembersFound(
            val members: GroupExpenseMembers
        ) : Outcome

        data class ExpenseRevised(
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

    private companion object {
        const val RECEIPT_ID_PREFIX = "expense-receipt"
    }
}
