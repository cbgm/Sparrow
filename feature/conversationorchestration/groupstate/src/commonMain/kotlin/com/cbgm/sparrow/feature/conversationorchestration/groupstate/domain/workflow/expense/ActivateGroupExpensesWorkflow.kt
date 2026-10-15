package com.cbgm.sparrow.feature.conversationorchestration.groupstate.domain.workflow.expense

import com.cbgm.sparrow.core.id.IdGenerator
import com.cbgm.sparrow.core.messagepart.domain.model.ExpenseBoard
import com.cbgm.sparrow.core.messagepart.domain.model.ExpensePolicy
import com.cbgm.sparrow.core.time.SystemClock
import com.cbgm.sparrow.feature.conversationorchestration.api.command.GroupMessageCommand
import com.cbgm.sparrow.feature.conversationorchestration.api.model.ConversationResult
import com.cbgm.sparrow.feature.conversationorchestration.engine.machine.ConversationWorkflow
import com.cbgm.sparrow.feature.conversationorchestration.engine.machine.Transition
import com.cbgm.sparrow.feature.conversationorchestration.engine.machine.WorkflowStep
import com.cbgm.sparrow.feature.conversationorchestration.engine.runner.WorkflowEngine
import com.cbgm.sparrow.feature.conversationorchestration.groupstate.domain.repository.GroupPinRepository
import com.cbgm.sparrow.feature.conversationorchestration.groupstate.domain.usecase.RequireGroupAdminContextUseCase
import kotlin.reflect.KClass

/**
 * AuthorizeAdmin -> CheckNoActiveBoard -> CreateBoard -> SendBoard -> PinBoard.
 *
 * An expense board is an ordinary group message part that occupies the group's pin slot.
 */
class ActivateGroupExpensesWorkflow internal constructor(
    private val engine: WorkflowEngine,
    private val pinRepository: GroupPinRepository,
    private val requireGroupAdminContext: RequireGroupAdminContextUseCase
) : ConversationWorkflow<
        GroupMessageCommand.ActivateExpenses,
        ActivateGroupExpensesWorkflow.Step,
        ActivateGroupExpensesWorkflow.Outcome
    > {
    override val commandType: KClass<GroupMessageCommand.ActivateExpenses> = GroupMessageCommand.ActivateExpenses::class

    override fun firstStep(command: GroupMessageCommand.ActivateExpenses): Step = Step.AuthorizeAdmin(command)

    override suspend fun perform(step: Step): Outcome =
        when (step) {
            is Step.AuthorizeAdmin -> {
                require(step.command.groupId.isNotBlank()) { "Group ID is missing" }
                requireGroupAdminContext(step.command.groupId, ADMIN_ACTION).getOrThrow()
                Outcome.AdminAuthorized
            }
            is Step.CheckNoActiveBoard -> {
                check(pinRepository.find(step.command.groupId).getOrThrow()?.hasActiveExpenseBoard != true) {
                    "Group expenses are already active"
                }
                Outcome.NoActiveBoard
            }
            is Step.CreateBoard -> {
                val board =
                    ExpenseBoard(
                        id = IdGenerator.generate(prefix = "expense-board"),
                        currencyCode = step.command.currencyCode,
                        activatedAtEpochMilliseconds = SystemClock.nowEpochMilliseconds()
                    )
                ExpensePolicy.requireValid(board)
                Outcome.BoardCreated(board)
            }
            is Step.SendBoard -> {
                val result =
                    engine.run(
                        GroupMessageCommand.SendMessage(step.command.groupId, listOf(step.board))
                    )
                        .getOrThrow()
                Outcome.BoardSent((result as ConversationResult.GroupMessageSent).messageId)
            }
            is Step.PinBoard -> {
                engine.run(
                    GroupMessageCommand.PinMessage(step.command.groupId, step.messageId)
                ).getOrThrow()
                Outcome.BoardPinned
            }
        }

    override fun transition(step: Step, outcome: Outcome): Transition<Step> =
        when (outcome) {
            Outcome.AdminAuthorized -> Transition.Next(Step.CheckNoActiveBoard(step.command))
            Outcome.NoActiveBoard -> Transition.Next(Step.CreateBoard(step.command))
            is Outcome.BoardCreated -> Transition.Next(Step.SendBoard(outcome.board, step.command))
            is Outcome.BoardSent -> Transition.Next(Step.PinBoard(outcome.messageId, step.command))
            Outcome.BoardPinned -> Transition.Complete()
        }

    sealed interface Step : WorkflowStep {
        val command: GroupMessageCommand.ActivateExpenses

        data class AuthorizeAdmin(
            override val command: GroupMessageCommand.ActivateExpenses
        ) : Step

        data class CheckNoActiveBoard(
            override val command: GroupMessageCommand.ActivateExpenses
        ) : Step

        data class CreateBoard(
            override val command: GroupMessageCommand.ActivateExpenses
        ) : Step

        data class SendBoard(
            val board: ExpenseBoard,
            override val command: GroupMessageCommand.ActivateExpenses
        ) : Step

        data class PinBoard(
            val messageId: String,
            override val command: GroupMessageCommand.ActivateExpenses
        ) : Step
    }

    sealed interface Outcome {
        data object AdminAuthorized : Outcome

        data object NoActiveBoard : Outcome

        data class BoardCreated(
            val board: ExpenseBoard
        ) : Outcome

        data class BoardSent(
            val messageId: String
        ) : Outcome

        data object BoardPinned : Outcome
    }

    private companion object {
        const val ADMIN_ACTION = "pinned message"
    }
}
