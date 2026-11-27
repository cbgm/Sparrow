package com.cbgm.sparrow.feature.conversationorchestration.groupstate.domain.workflow.expense

import com.cbgm.sparrow.core.base.messagepart.domain.model.Expense
import com.cbgm.sparrow.feature.conversationorchestration.api.command.GroupMessageCommand
import com.cbgm.sparrow.feature.conversationorchestration.api.model.ConversationResult
import com.cbgm.sparrow.feature.conversationorchestration.engine.machine.ConversationWorkflow
import com.cbgm.sparrow.feature.conversationorchestration.engine.machine.Transition
import com.cbgm.sparrow.feature.conversationorchestration.engine.machine.WorkflowStep
import com.cbgm.sparrow.feature.conversationorchestration.engine.runner.WorkflowEngine
import com.cbgm.sparrow.feature.conversationorchestration.groupstate.domain.usecase.PrepareGroupExpenseUseCase
import kotlin.reflect.KClass

/** PrepareExpense -> SendExpense. */
class CreateGroupExpenseWorkflow internal constructor(
    private val engine: WorkflowEngine,
    private val prepareGroupExpense: PrepareGroupExpenseUseCase
) : ConversationWorkflow<
        GroupMessageCommand.CreateExpense,
        CreateGroupExpenseWorkflow.Step,
        CreateGroupExpenseWorkflow.Outcome
    > {
    override val commandType: KClass<GroupMessageCommand.CreateExpense> = GroupMessageCommand.CreateExpense::class

    override fun firstStep(command: GroupMessageCommand.CreateExpense): Step = Step.PrepareExpense(command)

    override suspend fun perform(step: Step): Outcome =
        when (step) {
            is Step.PrepareExpense -> Outcome.ExpensePrepared(prepareGroupExpense(step.command).getOrThrow())
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
            is Outcome.ExpensePrepared -> Transition.Next(Step.SendExpense(outcome.expense, step.command))
            is Outcome.ExpenseSent -> Transition.Complete(outcome.result)
        }

    sealed interface Step : WorkflowStep {
        val command: GroupMessageCommand.CreateExpense

        data class PrepareExpense(
            override val command: GroupMessageCommand.CreateExpense
        ) : Step

        data class SendExpense(
            val expense: Expense,
            override val command: GroupMessageCommand.CreateExpense
        ) : Step
    }

    sealed interface Outcome {
        data class ExpensePrepared(
            val expense: Expense
        ) : Outcome

        data class ExpenseSent(
            val result: ConversationResult.GroupMessageSent
        ) : Outcome
    }
}
