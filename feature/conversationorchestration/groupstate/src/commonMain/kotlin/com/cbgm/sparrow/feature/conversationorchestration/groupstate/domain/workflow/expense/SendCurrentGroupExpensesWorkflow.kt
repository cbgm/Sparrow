package com.cbgm.sparrow.feature.conversationorchestration.groupstate.domain.workflow.expense

import com.cbgm.sparrow.core.messagepart.domain.model.ExpenseBoard
import com.cbgm.sparrow.core.messagepart.domain.model.MessagePart
import com.cbgm.sparrow.feature.attachments.domain.repository.MessageAttachmentOperationsRepository
import com.cbgm.sparrow.feature.conversationorchestration.engine.machine.Transition
import com.cbgm.sparrow.feature.conversationorchestration.engine.machine.WorkflowStateMachine
import com.cbgm.sparrow.feature.conversationorchestration.engine.machine.WorkflowStep
import com.cbgm.sparrow.feature.conversationorchestration.groupstate.domain.command.GroupStateCommand
import com.cbgm.sparrow.feature.conversationorchestration.groupstate.domain.repository.GroupPinRepository
import com.cbgm.sparrow.feature.conversationorchestration.groupstate.domain.usecase.RequireGroupAdminContextUseCase
import com.cbgm.sparrow.feature.conversationorchestration.message.domain.model.StoredMessage
import com.cbgm.sparrow.feature.conversationorchestration.message.domain.repository.GroupMessageRepository
import com.cbgm.sparrow.feature.conversationorchestration.message.domain.usecase.EncryptGroupMessageUseCase
import com.cbgm.sparrow.feature.conversationorchestration.message.domain.usecase.EnqueueGroupPacketsUseCase
import kotlin.reflect.KClass

/** FindBoard -> LoadExpenseMessages -> EnqueueExpenseMessages. */
class SendCurrentGroupExpensesWorkflow internal constructor(
    private val pinRepository: GroupPinRepository,
    private val requireGroupAdminContext: RequireGroupAdminContextUseCase,
    private val attachmentOperations: MessageAttachmentOperationsRepository,
    private val messageRepository: GroupMessageRepository,
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
            is Step.LoadExpenseMessages -> {
                val groupId = step.command.groupId
                val context = requireGroupAdminContext(groupId, ADMIN_ACTION).getOrThrow()
                check(step.command.contactId in context.recipientContactIds) {
                    "Contact is not an active group member"
                }
                val messages =
                    attachmentOperations.findExpenseMessageIds(groupId, step.board.id).getOrThrow()
                        .mapNotNull { messageId -> messageRepository.find(messageId).getOrThrow() }
                        .sortedBy(StoredMessage::createdAtEpochMilliseconds)
                        .map { message ->
                            ExpenseMessage(
                                message = message,
                                text = messageRepository.findText(message.id).getOrThrow().orEmpty(),
                                parts = attachmentOperations.messageParts(message.id).getOrThrow()
                            )
                        }
                Outcome.ExpenseMessagesLoaded(messages)
            }
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
            is Outcome.BoardFound -> Transition.Next(Step.LoadExpenseMessages(outcome.board, step.command))
            is Outcome.ExpenseMessagesLoaded ->
                Transition.Next(Step.EnqueueExpenseMessages(outcome.messages, step.command))
            Outcome.ExpenseMessagesEnqueued -> Transition.Complete()
        }

    data class ExpenseMessage(
        val message: StoredMessage,
        val text: String,
        val parts: List<MessagePart>
    )

    sealed interface Step : WorkflowStep {
        val command: GroupStateCommand.SendCurrentGroupExpenses

        data class FindBoard(
            override val command: GroupStateCommand.SendCurrentGroupExpenses
        ) : Step

        data class LoadExpenseMessages(
            val board: ExpenseBoard,
            override val command: GroupStateCommand.SendCurrentGroupExpenses
        ) : Step

        data class EnqueueExpenseMessages(
            val messages: List<ExpenseMessage>,
            override val command: GroupStateCommand.SendCurrentGroupExpenses
        ) : Step
    }

    sealed interface Outcome {
        data object NoOpenBoard : Outcome

        data class BoardFound(
            val board: ExpenseBoard
        ) : Outcome

        data class ExpenseMessagesLoaded(
            val messages: List<ExpenseMessage>
        ) : Outcome

        data object ExpenseMessagesEnqueued : Outcome
    }

    private companion object {
        const val ADMIN_ACTION = "expenses"
    }
}
