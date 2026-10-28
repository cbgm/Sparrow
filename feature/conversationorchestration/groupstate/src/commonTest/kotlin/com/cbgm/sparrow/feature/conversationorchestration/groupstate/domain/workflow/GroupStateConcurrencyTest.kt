package com.cbgm.sparrow.feature.conversationorchestration.groupstate.domain.workflow

import com.cbgm.sparrow.core.base.messagepart.domain.model.ExpenseBoard
import com.cbgm.sparrow.core.base.messagepart.domain.model.MessagePart
import com.cbgm.sparrow.feature.conversationorchestration.api.command.GroupMessageCommand
import com.cbgm.sparrow.feature.conversationorchestration.api.model.ConversationResult
import com.cbgm.sparrow.feature.conversationorchestration.engine.machine.Transition
import com.cbgm.sparrow.feature.conversationorchestration.engine.machine.WorkflowStateMachine
import com.cbgm.sparrow.feature.conversationorchestration.engine.machine.WorkflowStep
import com.cbgm.sparrow.feature.conversationorchestration.engine.runner.WorkflowEngine
import com.cbgm.sparrow.feature.conversationorchestration.engine.runner.WorkflowLocks
import com.cbgm.sparrow.feature.conversationorchestration.engine.runner.WorkflowRegistry
import com.cbgm.sparrow.feature.conversationorchestration.groupstate.domain.model.GroupPinState
import com.cbgm.sparrow.feature.conversationorchestration.groupstate.domain.model.GroupTitleState
import com.cbgm.sparrow.feature.conversationorchestration.groupstate.domain.repository.GroupPinRepository
import com.cbgm.sparrow.feature.conversationorchestration.groupstate.domain.repository.GroupTitleRepository
import com.cbgm.sparrow.feature.conversationorchestration.groupstate.domain.usecase.CreateExpenseBoardUseCase
import com.cbgm.sparrow.feature.conversationorchestration.groupstate.domain.usecase.RequireGroupAdminContextUseCase
import com.cbgm.sparrow.feature.conversationorchestration.groupstate.domain.usecase.SaveGroupPinUseCase
import com.cbgm.sparrow.feature.conversationorchestration.groupstate.domain.usecase.SaveGroupTitleUseCase
import com.cbgm.sparrow.feature.conversationorchestration.groupstate.domain.workflow.expense.ActivateGroupExpensesWorkflow
import com.cbgm.sparrow.feature.membership.domain.usecase.AuthorizeGroupMetadataUseCase
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.yield
import kotlin.reflect.KClass
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

/**
 * Local group state is saved without a lock, so every save of a versioned state is a compare-and-set that a
 * concurrent save cannot undo, and activating expenses twice in a row opens only one board.
 */
class GroupStateConcurrencyTest {
    private val titles = FakeTitleRepository(GroupTitleState(GROUP_ID, "Old title", changedAtEpochMilliseconds = 5L))
    private val pins = FakePinRepository(stored = null)

    @Test
    fun localTitleDoesNotOverwriteANewerTitleSavedWhileItWasChecked() = runBlocking {
        val remote = GroupTitleState(GROUP_ID, "Remote title", changedAtEpochMilliseconds = 7L)
        val save = SaveGroupTitleUseCase(RacingTitleRepository(titles) { titles.stored = remote })

        assertFalse(save(GroupTitleState(GROUP_ID, "Local title", 6L, publishPending = true)).getOrThrow())
        assertEquals(remote, titles.stored)
    }

    @Test
    fun receivedTitleIsStoredOnlyWhenNewerAndRetriedAfterAConcurrentOlderSave() = runBlocking {
        val olderSave = { titles.stored = titles.stored.copy(changedAtEpochMilliseconds = 6L) }
        val save = SaveGroupTitleUseCase(RacingTitleRepository(titles, olderSave))

        assertFalse(save(GroupTitleState(GROUP_ID, "Replay", changedAtEpochMilliseconds = 5L)).getOrThrow())
        assertTrue(save(GroupTitleState(GROUP_ID, "Newer", changedAtEpochMilliseconds = 8L)).getOrThrow())
        assertEquals("Newer", titles.stored.title)
    }

    @Test
    fun receivedPinYieldsToABoardSavedWhileItWasChecked() = runBlocking {
        val board = boardPin(changedAt = 3L)
        val save = SaveGroupPinUseCase(RacingPinRepository(pins) { pins.stored = board })

        assertFalse(save.received(messagePin(changedAt = 4L)).getOrThrow())
        assertEquals(board, pins.stored)
    }

    @Test
    fun localPinKeepsItsOwnSlotCheckAndIsStoredWhenNewer() = runBlocking {
        pins.stored = boardPin(changedAt = 3L)

        assertFalse(SaveGroupPinUseCase(pins).received(messagePin(changedAt = 4L)).getOrThrow())
        assertTrue(SaveGroupPinUseCase(pins)(messagePin(changedAt = 4L)).getOrThrow())
    }

    @Test
    fun activatingExpensesTwiceAtOnceSendsOneBoard() = runBlocking {
        val pinned = CompletableDeferred<Unit>()
        val sent = mutableListOf<ExpenseBoard>()
        lateinit var engine: WorkflowEngine
        val registry =
            WorkflowRegistry {
                listOf(activateWorkflow(engine), SendBoardWorkflow(sent), PinBoardWorkflow(sent, pinned))
            }
        engine = WorkflowEngine(WorkflowLocks(), registry)
        val command = GroupMessageCommand.ActivateExpenses(GROUP_ID, currencyCode = "EUR")

        val first = async { engine.run(command) }
        val second = async { engine.run(command) }
        yield()
        pinned.complete(Unit)

        val results = listOf(first.await(), second.await())
        assertEquals(1, sent.size)
        assertEquals(1, results.count { result -> result.isSuccess })
        val failure = results.single { result -> result.isFailure }.exceptionOrNull()
        assertEquals("Group expenses are already active", failure?.message)
        assertNotEquals(groupAdministrationLockKey(GROUP_ID), activateWorkflow(engine).lockKey(command))
    }

    private fun activateWorkflow(engine: WorkflowEngine) =
        ActivateGroupExpensesWorkflow(
            engine = engine,
            pinRepository = pins,
            requireGroupAdminContext =
                RequireGroupAdminContextUseCase(
                    AuthorizeGroupMetadataUseCase(FakeGroupMembershipRepository()),
                    FakeSigningKeys
                ),
            createExpenseBoard = CreateExpenseBoardUseCase()
        )

    /** Stores the board message and reports its id like the group send does. */
    private class SendBoardWorkflow(
        private val sent: MutableList<ExpenseBoard>
    ) : WorkflowStateMachine<GroupMessageCommand.SendMessage, SendBoardWorkflow.Send, Unit> {
        override val commandType: KClass<GroupMessageCommand.SendMessage> = GroupMessageCommand.SendMessage::class

        override fun firstStep(command: GroupMessageCommand.SendMessage): Send = Send(command)

        override suspend fun perform(step: Send) {
            sent += step.command.parts.filterIsInstance<ExpenseBoard>()
        }

        override fun transition(step: Send, outcome: Unit): Transition<Send> =
            Transition.Complete(ConversationResult.GroupMessageSent("board-message-${sent.size}"))

        class Send(
            val command: GroupMessageCommand.SendMessage
        ) : WorkflowStep
    }

    /** Pins the sent board once [pinned] completes, so the second activation runs while the first one waits. */
    private inner class PinBoardWorkflow(
        private val sent: List<ExpenseBoard>,
        private val pinned: CompletableDeferred<Unit>
    ) : WorkflowStateMachine<GroupMessageCommand.PinMessage, PinBoardWorkflow.Pin, Unit> {
        override val commandType: KClass<GroupMessageCommand.PinMessage> = GroupMessageCommand.PinMessage::class

        override fun firstStep(command: GroupMessageCommand.PinMessage): Pin = Pin(command)

        override suspend fun perform(step: Pin) {
            pinned.await()
            pins.stored = pin(step.command.messageId, listOf(sent.last()), changedAt = 9L)
        }

        override fun transition(step: Pin, outcome: Unit): Transition<Pin> = Transition.Complete()

        inner class Pin(
            val command: GroupMessageCommand.PinMessage
        ) : WorkflowStep
    }

    /** Runs [beforeFirstSave] right before the first compare-and-set, like a save that wins the race. */
    private class RacingTitleRepository(
        private val delegate: GroupTitleRepository,
        private var beforeFirstSave: (() -> Unit)?
    ) : GroupTitleRepository by delegate {
        override suspend fun saveIfVersion(
            state: GroupTitleState,
            expectedChangedAtEpochMilliseconds: Long
        ): Result<Boolean> {
            beforeFirstSave?.invoke()
            beforeFirstSave = null
            return delegate.saveIfVersion(state, expectedChangedAtEpochMilliseconds)
        }
    }

    private class RacingPinRepository(
        private val delegate: GroupPinRepository,
        private var beforeFirstSave: (() -> Unit)?
    ) : GroupPinRepository by delegate {
        override suspend fun saveIfVersion(
            pin: GroupPinState,
            expectedChangedAtEpochMilliseconds: Long
        ): Result<Boolean> {
            beforeFirstSave?.invoke()
            beforeFirstSave = null
            return delegate.saveIfVersion(pin, expectedChangedAtEpochMilliseconds)
        }
    }

    private companion object {
        fun messagePin(changedAt: Long) = pin("message-1", emptyList(), changedAt)

        fun boardPin(changedAt: Long) =
            pin(
                "board-message",
                listOf(ExpenseBoard("board-1", currencyCode = "EUR", activatedAtEpochMilliseconds = 1L)),
                changedAt
            )

        fun pin(messageId: String, parts: List<MessagePart>, changedAt: Long) =
            GroupPinState(
                groupId = GROUP_ID,
                messageId = messageId,
                messageSentAtEpochMilliseconds = 1L,
                messageSenderSigningPublicKey = ADMIN_KEYS.publicKey,
                senderContactId = null,
                isMine = true,
                parts = parts,
                replyToMessageId = null,
                pinnedAtEpochMilliseconds = changedAt,
                changedAtEpochMilliseconds = changedAt
            )
    }
}
