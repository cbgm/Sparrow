package com.cbgm.sparrow.feature.conversationorchestration.groupstate.domain.workflow.expense

import com.cbgm.sparrow.core.base.messagepart.domain.model.ExpenseBoard
import com.cbgm.sparrow.feature.conversationorchestration.engine.machine.Transition
import com.cbgm.sparrow.feature.conversationorchestration.engine.machine.WorkflowStateMachine
import com.cbgm.sparrow.feature.conversationorchestration.engine.machine.WorkflowStep
import com.cbgm.sparrow.feature.conversationorchestration.groupstate.domain.command.GroupStateCommand
import com.cbgm.sparrow.feature.conversationorchestration.groupstate.domain.model.GroupExpenseMessage
import com.cbgm.sparrow.feature.conversationorchestration.groupstate.domain.repository.GroupPinRepository
import com.cbgm.sparrow.feature.conversationorchestration.groupstate.domain.usecase.LoadGroupExpenseMessagesUseCase
import com.cbgm.sparrow.feature.conversationorchestration.groupstate.domain.usecase.RequireGroupAdminContextUseCase
import com.cbgm.sparrow.feature.conversationorchestration.message.domain.usecase.EncryptGroupMessageUseCase
import com.cbgm.sparrow.feature.conversationorchestration.message.domain.usecase.EnqueueGroupPacketsUseCase
import kotlin.reflect.KClass

/** FindBoard -> AuthorizeRecipient -> LoadExpenseMessages -> EnqueueExpenseMessages. */
class SendCurrentGroupExpensesWorkflow internal constructor(
    private val pinRepository: GroupPinRepository,
    private val requireGroupAdminContext: RequireGroupAdminContextUseCase,
    private val loadGroupExpenseMessages: LoadGroupExpenseMessagesUseCase,
    private val encryptGroupMessage: EncryptGroupMessageUseCase,
    private val enqueueGroupPackets: EnqueueGroupPacketsUseCase
) : WorkflowStateMachine<
        GroupStateCommand.SendCurrentGroupExpenses,
        SendCurrentGroupExpensesWorkflow.Step,
        SendCurrentGroupExpensesWorkflow.Outcome
    > {
    override val commandType: KClass<GroupStateCommand.SendCurrentGroupExpenses> =
        GroupStateCommand.SendCurrentGroupExpenses::class

    override fun firstStep(command: GroupStateCommand.SendCurrentGroupExpenses): Step = Step.FindBoard(command)

    override suspend fun perform(step: Step): Outcome =
        when (step) {
            is Step.FindBoard -> {
                require(step.command.contactId.isNotBlank()) { "Contact ID must not be blank" }
                pinRepository.find(step.command.groupId).getOrThrow()?.activeExpenseBoard
                    ?.let(Outcome::BoardFound)
                    ?: Outcome.NoOpenBoard
            }
            is Step.AuthorizeRecipient -> {
                requireGroupAdminContext.forRecipient(step.command.groupId, ADMIN_ACTION, step.command.contactId)
                    .getOrThrow()
                Outcome.RecipientAuthorized(step.board)
            }
            is Step.LoadExpenseMessages ->
                Outcome.ExpenseMessagesLoaded(
                    loadGroupExpenseMessages(step.command.groupId, step.board.id).getOrThrow()
                )
            is Step.EnqueueExpenseMessages -> {
                step.messages.forEach { expenseMessage ->
                    val packets =
                        encryptGroupMessage(
                            message = expenseMessage.message,
                            text = expenseMessage.text,
                            parts = expenseMessage.parts,
                            recipients = listOf(step.command.contactId)
                        ).getOrThrow()
                    enqueueGroupPackets(packets, recordFailedRecipients = false).getOrThrow()
                }
                Outcome.ExpenseMessagesEnqueued
            }
        }

    override fun transition(step: Step, outcome: Outcome): Transition<Step> =
        when (outcome) {
            Outcome.NoOpenBoard -> Transition.Complete()
            is Outcome.BoardFound -> Transition.Next(Step.AuthorizeRecipient(outcome.board, step.command))
            is Outcome.RecipientAuthorized -> Transition.Next(Step.LoadExpenseMessages(outcome.board, step.command))
            is Outcome.ExpenseMessagesLoaded ->
                Transition.Next(Step.EnqueueExpenseMessages(outcome.messages, step.command))
            Outcome.ExpenseMessagesEnqueued -> Transition.Complete()
        }

    sealed interface Step : WorkflowStep {
        val command: GroupStateCommand.SendCurrentGroupExpenses

        data class FindBoard(
            override val command: GroupStateCommand.SendCurrentGroupExpenses
        ) : Step

        data class AuthorizeRecipient(
            val board: ExpenseBoard,
            override val command: GroupStateCommand.SendCurrentGroupExpenses
        ) : Step

        data class LoadExpenseMessages(
            val board: ExpenseBoard,
            override val command: GroupStateCommand.SendCurrentGroupExpenses
        ) : Step

        data class EnqueueExpenseMessages(
            val messages: List<GroupExpenseMessage>,
            override val command: GroupStateCommand.SendCurrentGroupExpenses
        ) : Step
    }

    sealed interface Outcome {
        data object NoOpenBoard : Outcome

        data class BoardFound(
            val board: ExpenseBoard
        ) : Outcome

        data class RecipientAuthorized(
            val board: ExpenseBoard
        ) : Outcome

        data class ExpenseMessagesLoaded(
            val messages: List<GroupExpenseMessage>
        ) : Outcome

        data object ExpenseMessagesEnqueued : Outcome
    }

    private companion object {
        const val ADMIN_ACTION = "expenses"
    }
}
