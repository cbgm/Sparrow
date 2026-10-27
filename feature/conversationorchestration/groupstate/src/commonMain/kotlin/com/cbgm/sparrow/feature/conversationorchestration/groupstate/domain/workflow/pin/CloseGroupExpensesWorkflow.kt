package com.cbgm.sparrow.feature.conversationorchestration.groupstate.domain.workflow.pin

import com.cbgm.sparrow.core.base.messagepart.domain.model.ExpenseBoard
import com.cbgm.sparrow.core.base.messagepart.domain.model.ExpenseBoardClosed
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
import com.cbgm.sparrow.feature.conversationorchestration.groupstate.domain.usecase.CreateExpenseBoardUseCase
import com.cbgm.sparrow.feature.conversationorchestration.groupstate.domain.usecase.CreateGroupPinPacketsUseCase
import com.cbgm.sparrow.feature.conversationorchestration.groupstate.domain.usecase.FindExpenseBoardClosureUseCase
import com.cbgm.sparrow.feature.conversationorchestration.groupstate.domain.usecase.PrepareEmptyGroupPinUseCase
import com.cbgm.sparrow.feature.conversationorchestration.groupstate.domain.usecase.RequireGroupAdminContextUseCase
import com.cbgm.sparrow.feature.conversationorchestration.groupstate.domain.usecase.SaveGroupPinUseCase
import com.cbgm.sparrow.feature.conversationorchestration.groupstate.domain.workflow.groupAdministrationLockKey
import com.cbgm.sparrow.feature.conversationorchestration.message.domain.usecase.DeleteLocalExpenseReceiptsUseCase
import com.cbgm.sparrow.feature.conversationorchestration.message.domain.usecase.EnqueueGroupPacketsUseCase
import com.cbgm.sparrow.protocol.packet.GroupPinUpdatedPacket
import kotlin.reflect.KClass

/**
 * Closes the open expense board: announces the closing as an ordinary encrypted group message,
 * then clears the pin slot the board occupies.
 *
 * AuthorizeAdmin -> FindPin -> FindBoardClosure -> [SendBoardClosedMessage] -> DeleteReceipts -> PrepareEmptyPin
 *   -> CreatePinPackets -> EnqueuePinPackets -> SaveEmptyPin.
 *
 * The closing message is queued before the pin is released, so a failed send never closes the board. Every step
 * can run again: a retry after a failure finds the stored closing message and does not announce it twice, and the
 * pin stays until its packets are queued, so the retry still releases it for every member.
 */
class CloseGroupExpensesWorkflow internal constructor(
    private val engine: WorkflowEngine,
    private val pinRepository: GroupPinRepository,
    private val requireGroupAdminContext: RequireGroupAdminContextUseCase,
    private val findExpenseBoardClosure: FindExpenseBoardClosureUseCase,
    private val createExpenseBoard: CreateExpenseBoardUseCase,
    private val prepareEmptyGroupPin: PrepareEmptyGroupPinUseCase,
    private val createGroupPinPackets: CreateGroupPinPacketsUseCase,
    private val enqueueGroupPackets: EnqueueGroupPacketsUseCase,
    private val saveGroupPin: SaveGroupPinUseCase,
    private val deleteLocalExpenseReceipts: DeleteLocalExpenseReceiptsUseCase
) : ConversationWorkflow<
        GroupMessageCommand.CloseExpenses,
        CloseGroupExpensesWorkflow.Step,
        CloseGroupExpensesWorkflow.Outcome
    > {
    override val commandType: KClass<GroupMessageCommand.CloseExpenses> = GroupMessageCommand.CloseExpenses::class

    override fun lockKey(command: GroupMessageCommand.CloseExpenses): String =
        groupAdministrationLockKey(command.groupId)

    override fun firstStep(command: GroupMessageCommand.CloseExpenses): Step = Step.AuthorizeAdmin(command)

    override suspend fun perform(step: Step): Outcome =
        when (step) {
            is Step.AuthorizeAdmin ->
                Outcome.AdminAuthorized(requireGroupAdminContext(step.command.groupId, ADMIN_ACTION).getOrThrow())
            is Step.FindPin -> pinFound(pinRepository.find(step.command.groupId).getOrThrow(), step.context)
            is Step.FindBoardClosure ->
                boardClosureFound(
                    findExpenseBoardClosure(step.command.groupId, step.board.id).getOrThrow(),
                    step.pin,
                    step.board,
                    step.context
                )
            is Step.SendBoardClosedMessage -> {
                engine.run(
                    GroupMessageCommand.SendMessage(
                        groupId = step.command.groupId,
                        parts = listOf(createExpenseBoard.closing(step.board).getOrThrow())
                    )
                ).getOrThrow()
                Outcome.BoardClosedMessageSent(step.pin, step.board, step.context)
            }
            is Step.DeleteReceipts -> {
                deleteLocalExpenseReceipts.forBoard(step.command.groupId, step.board.id).getOrThrow()
                Outcome.ReceiptsDeleted(step.pin, step.context)
            }
            is Step.PrepareEmptyPin ->
                Outcome.EmptyPinPrepared(
                    prepareEmptyGroupPin(step.command.groupId, step.pin).getOrThrow(),
                    step.context
                )
            is Step.CreatePinPackets ->
                Outcome.PinPacketsCreated(step.pin, createGroupPinPackets(step.pin, step.context).getOrThrow())
            is Step.EnqueuePinPackets -> {
                enqueueGroupPackets(step.packets, recordFailedRecipients = false).getOrThrow()
                Outcome.PinPacketsEnqueued(step.pin)
            }
            is Step.SaveEmptyPin -> {
                saveGroupPin(step.pin).getOrThrow()
                Outcome.EmptyPinSaved
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
                Transition.Next(Step.FindBoardClosure(outcome.pin, outcome.board, outcome.context, step.command))
            is Outcome.BoardNotAnnounced ->
                Transition.Next(Step.SendBoardClosedMessage(outcome.pin, outcome.board, outcome.context, step.command))
            is Outcome.BoardClosedMessageSent ->
                Transition.Next(Step.DeleteReceipts(outcome.pin, outcome.board, outcome.context, step.command))
            is Outcome.ReceiptsDeleted ->
                Transition.Next(Step.PrepareEmptyPin(outcome.pin, outcome.context, step.command))
            is Outcome.EmptyPinPrepared ->
                Transition.Next(Step.CreatePinPackets(outcome.pin, outcome.context, step.command))
            is Outcome.PinPacketsCreated ->
                Transition.Next(Step.EnqueuePinPackets(outcome.pin, outcome.packets, step.command))
            is Outcome.PinPacketsEnqueued -> Transition.Next(Step.SaveEmptyPin(outcome.pin, step.command))
            Outcome.EmptyPinSaved -> Transition.Complete()
        }

    private fun pinFound(pin: GroupPinState?, context: GroupAdminContext): Outcome {
        val board = pin?.activeExpenseBoard
        return when {
            pin?.messageId == null -> Outcome.NothingPinned
            board == null -> Outcome.NoActiveExpenseBoard
            else -> Outcome.ExpenseBoardFound(pin, board, context)
        }
    }

    /** A closing message stored by an earlier, interrupted attempt is not sent again. */
    private fun boardClosureFound(
        closure: ExpenseBoardClosed?,
        pin: GroupPinState,
        board: ExpenseBoard,
        context: GroupAdminContext
    ): Outcome =
        if (closure == null) {
            Outcome.BoardNotAnnounced(pin, board, context)
        } else {
            Outcome.BoardClosedMessageSent(pin, board, context)
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

        data class FindBoardClosure(
            val pin: GroupPinState,
            val board: ExpenseBoard,
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

        data class PrepareEmptyPin(
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
            val pin: GroupPinState,
            val packets: Map<String, GroupPinUpdatedPacket>,
            override val command: GroupMessageCommand.CloseExpenses
        ) : Step

        data class SaveEmptyPin(
            val pin: GroupPinState,
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

        data class BoardNotAnnounced(
            val pin: GroupPinState,
            val board: ExpenseBoard,
            val context: GroupAdminContext
        ) : Outcome

        /** The closing message is queued, now or by an earlier attempt. */
        data class BoardClosedMessageSent(
            val pin: GroupPinState,
            val board: ExpenseBoard,
            val context: GroupAdminContext
        ) : Outcome

        data class ReceiptsDeleted(
            val pin: GroupPinState,
            val context: GroupAdminContext
        ) : Outcome

        data class EmptyPinPrepared(
            val pin: GroupPinState,
            val context: GroupAdminContext
        ) : Outcome

        data class PinPacketsCreated(
            val pin: GroupPinState,
            val packets: Map<String, GroupPinUpdatedPacket>
        ) : Outcome

        data class PinPacketsEnqueued(
            val pin: GroupPinState
        ) : Outcome

        data object EmptyPinSaved : Outcome
    }

    private companion object {
        const val ADMIN_ACTION = "pinned message"
    }
}
