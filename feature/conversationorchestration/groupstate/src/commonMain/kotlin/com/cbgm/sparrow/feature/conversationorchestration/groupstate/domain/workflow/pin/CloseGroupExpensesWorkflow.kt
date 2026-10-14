package com.cbgm.sparrow.feature.conversationorchestration.groupstate.domain.workflow.pin

import com.cbgm.sparrow.core.id.IdGenerator
import com.cbgm.sparrow.core.messagepart.domain.model.ExpenseBoard
import com.cbgm.sparrow.core.messagepart.domain.model.ExpenseBoardClosed
import com.cbgm.sparrow.core.time.SystemClock
import com.cbgm.sparrow.feature.conversationorchestration.api.command.GroupMessageCommand
import com.cbgm.sparrow.feature.conversationorchestration.api.model.ConversationFailure
import com.cbgm.sparrow.feature.conversationorchestration.api.model.ConversationFailureReason
import com.cbgm.sparrow.feature.conversationorchestration.engine.machine.ConversationWorkflow
import com.cbgm.sparrow.feature.conversationorchestration.engine.machine.Transition
import com.cbgm.sparrow.feature.conversationorchestration.engine.machine.WorkflowStep
import com.cbgm.sparrow.feature.conversationorchestration.engine.runner.WorkflowEngine
import com.cbgm.sparrow.feature.conversationorchestration.groupstate.domain.model.GroupAdminContext
import com.cbgm.sparrow.feature.conversationorchestration.groupstate.domain.model.GroupPinState
import com.cbgm.sparrow.feature.conversationorchestration.groupstate.domain.repository.GroupPinRepository
import com.cbgm.sparrow.feature.conversationorchestration.groupstate.domain.usecase.CreateGroupPinPacketsUseCase
import com.cbgm.sparrow.feature.conversationorchestration.groupstate.domain.usecase.RequireGroupAdminContextUseCase
import com.cbgm.sparrow.feature.conversationorchestration.message.domain.usecase.DeleteLocalExpenseReceiptsUseCase
import com.cbgm.sparrow.feature.conversationorchestration.message.domain.usecase.EnqueueGroupPacketsUseCase
import com.cbgm.sparrow.protocol.packet.GroupPinUpdatedPacket
import kotlin.reflect.KClass

/**
 * Closes the open expense board: announces the closing as an ordinary encrypted group message,
 * then clears the pin slot the board occupies.
 *
 * AuthorizeAdmin -> FindPin -> SendBoardClosedMessage -> DeleteReceipts -> SaveEmptyPin -> CreatePinPackets
 *   -> EnqueuePinPackets.
 *
 * The closing message is queued before the pin is released, so a failed send never closes the board.
 */
class CloseGroupExpensesWorkflow internal constructor(
    private val engine: WorkflowEngine,
    private val pinRepository: GroupPinRepository,
    private val requireGroupAdminContext: RequireGroupAdminContextUseCase,
    private val createGroupPinPackets: CreateGroupPinPacketsUseCase,
    private val enqueueGroupPackets: EnqueueGroupPacketsUseCase,
    private val deleteLocalExpenseReceipts: DeleteLocalExpenseReceiptsUseCase
) : ConversationWorkflow<
        GroupMessageCommand.CloseExpenses,
        CloseGroupExpensesWorkflow.Step,
        CloseGroupExpensesWorkflow.Outcome
    > {
    override val commandType: KClass<GroupMessageCommand.CloseExpenses> = GroupMessageCommand.CloseExpenses::class

    override fun lockKey(command: GroupMessageCommand.CloseExpenses): String = groupPinLockKey(command.groupId)

    override fun firstStep(command: GroupMessageCommand.CloseExpenses): Step = Step.AuthorizeAdmin(command)

    override suspend fun perform(step: Step): Outcome =
        when (step) {
            is Step.AuthorizeAdmin ->
                Outcome.AdminAuthorized(requireGroupAdminContext(step.command.groupId, ADMIN_ACTION).getOrThrow())
            is Step.FindPin -> {
                val pin = pinRepository.find(step.command.groupId).getOrThrow()
                val board = pin?.activeExpenseBoard
                when {
                    pin?.messageId == null -> Outcome.NothingPinned
                    board == null -> Outcome.NoActiveExpenseBoard
                    else -> Outcome.ExpenseBoardFound(pin, board, step.context)
                }
            }
            is Step.SendBoardClosedMessage -> {
                engine.run(
                    GroupMessageCommand.SendMessage(
                        groupId = step.command.groupId,
                        parts = listOf(step.board.toClosedPart())
                    )
                ).getOrThrow()
                Outcome.BoardClosedMessageSent(step.pin, step.board, step.context)
            }
            is Step.DeleteReceipts -> {
                deleteLocalExpenseReceipts.forBoard(step.command.groupId, step.board.id).getOrThrow()
                Outcome.ReceiptsDeleted(step.pin, step.context)
            }
            is Step.SaveEmptyPin -> {
                val emptyPin = GroupPinState.unpinned(
                    groupId = step.command.groupId,
                    changedAtEpochMilliseconds = maxOf(
                        SystemClock.nowEpochMilliseconds(),
                        step.pin.changedAtEpochMilliseconds + 1L
                    )
                )
                pinRepository.save(emptyPin).getOrThrow()
                Outcome.EmptyPinSaved(emptyPin, step.context)
            }
            is Step.CreatePinPackets ->
                Outcome.PinPacketsCreated(createGroupPinPackets(step.pin, step.context).getOrThrow())
            is Step.EnqueuePinPackets -> {
                enqueueGroupPackets(step.packets, recordFailedRecipients = false).getOrThrow()
                Outcome.PinPacketsEnqueued
            }
        }

    override fun transition(step: Step, outcome: Outcome): Transition<Step> =
        when (outcome) {
            is Outcome.AdminAuthorized -> Transition.Next(Step.FindPin(outcome.context, step.command))
            Outcome.NothingPinned -> Transition.Complete()
            Outcome.NoActiveExpenseBoard ->
                Transition.Reject(
                    ConversationFailure(ConversationFailureReason.NOT_ALLOWED, "There is no active expense board")
                )
            is Outcome.ExpenseBoardFound ->
                Transition.Next(Step.SendBoardClosedMessage(outcome.pin, outcome.board, outcome.context, step.command))
            is Outcome.BoardClosedMessageSent ->
                Transition.Next(Step.DeleteReceipts(outcome.pin, outcome.board, outcome.context, step.command))
            is Outcome.ReceiptsDeleted ->
                Transition.Next(Step.SaveEmptyPin(outcome.pin, outcome.context, step.command))
            is Outcome.EmptyPinSaved ->
                Transition.Next(Step.CreatePinPackets(outcome.pin, outcome.context, step.command))
            is Outcome.PinPacketsCreated -> Transition.Next(Step.EnqueuePinPackets(outcome.packets, step.command))
            Outcome.PinPacketsEnqueued -> Transition.Complete()
        }

    sealed interface Step : WorkflowStep {
        val command: GroupMessageCommand.CloseExpenses

        data class AuthorizeAdmin(
            override val command: GroupMessageCommand.CloseExpenses
        ) : Step

        data class FindPin(
            val context: GroupAdminContext,
            override val command: GroupMessageCommand.CloseExpenses
        ) : Step

        data class SendBoardClosedMessage(
            val pin: GroupPinState,
            val board: ExpenseBoard,
            val context: GroupAdminContext,
            override val command: GroupMessageCommand.CloseExpenses
        ) : Step

        data class DeleteReceipts(
            val pin: GroupPinState,
            val board: ExpenseBoard,
            val context: GroupAdminContext,
            override val command: GroupMessageCommand.CloseExpenses
        ) : Step

        data class SaveEmptyPin(
            val pin: GroupPinState,
            val context: GroupAdminContext,
            override val command: GroupMessageCommand.CloseExpenses
        ) : Step

        data class CreatePinPackets(
            val pin: GroupPinState,
            val context: GroupAdminContext,
            override val command: GroupMessageCommand.CloseExpenses
        ) : Step

        data class EnqueuePinPackets(
            val packets: Map<String, GroupPinUpdatedPacket>,
            override val command: GroupMessageCommand.CloseExpenses
        ) : Step
    }

    sealed interface Outcome {
        data class AdminAuthorized(
            val context: GroupAdminContext
        ) : Outcome

        data object NothingPinned : Outcome

        data object NoActiveExpenseBoard : Outcome

        data class ExpenseBoardFound(
            val pin: GroupPinState,
            val board: ExpenseBoard,
            val context: GroupAdminContext
        ) : Outcome

        data class BoardClosedMessageSent(
            val pin: GroupPinState,
            val board: ExpenseBoard,
            val context: GroupAdminContext
        ) : Outcome

        data class ReceiptsDeleted(
            val pin: GroupPinState,
            val context: GroupAdminContext
        ) : Outcome

        data class EmptyPinSaved(
            val pin: GroupPinState,
            val context: GroupAdminContext
        ) : Outcome

        data class PinPacketsCreated(
            val packets: Map<String, GroupPinUpdatedPacket>
        ) : Outcome

        data object PinPacketsEnqueued : Outcome
    }

    private fun ExpenseBoard.toClosedPart(): ExpenseBoardClosed =
        ExpenseBoardClosed(
            id = IdGenerator.generate(prefix = "expense-closed"),
            boardId = id,
            closedAtEpochMilliseconds = SystemClock.nowEpochMilliseconds()
        )

    private companion object {
        const val ADMIN_ACTION = "pinned message"
    }
}
