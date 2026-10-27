package com.cbgm.sparrow.feature.conversationorchestration.groupstate.domain.workflow

import com.cbgm.sparrow.core.base.messagepart.domain.model.ExpenseBoard
import com.cbgm.sparrow.core.base.messagepart.domain.model.ExpenseBoardClosed
import com.cbgm.sparrow.feature.conversationorchestration.api.command.GroupAdministrationCommand
import com.cbgm.sparrow.feature.conversationorchestration.api.command.GroupMessageCommand
import com.cbgm.sparrow.feature.conversationorchestration.engine.machine.Transition
import com.cbgm.sparrow.feature.conversationorchestration.engine.machine.WorkflowStateMachine
import com.cbgm.sparrow.feature.conversationorchestration.engine.machine.WorkflowStep
import com.cbgm.sparrow.feature.conversationorchestration.engine.runner.WorkflowEngine
import com.cbgm.sparrow.feature.conversationorchestration.engine.runner.WorkflowLocks
import com.cbgm.sparrow.feature.conversationorchestration.engine.runner.WorkflowRegistry
import com.cbgm.sparrow.feature.conversationorchestration.groupstate.domain.command.GroupStateCommand
import com.cbgm.sparrow.feature.conversationorchestration.groupstate.domain.model.GroupAdminContext
import com.cbgm.sparrow.feature.conversationorchestration.groupstate.domain.model.GroupPinState
import com.cbgm.sparrow.feature.conversationorchestration.groupstate.domain.model.GroupTitleState
import com.cbgm.sparrow.feature.conversationorchestration.groupstate.domain.packet.GroupTitlePacketCodec
import com.cbgm.sparrow.feature.conversationorchestration.groupstate.domain.usecase.CreateExpenseBoardUseCase
import com.cbgm.sparrow.feature.conversationorchestration.groupstate.domain.usecase.CreateGroupPinPacketsUseCase
import com.cbgm.sparrow.feature.conversationorchestration.groupstate.domain.usecase.CreateGroupTitlePacketsUseCase
import com.cbgm.sparrow.feature.conversationorchestration.groupstate.domain.usecase.FindExpenseBoardClosureUseCase
import com.cbgm.sparrow.feature.conversationorchestration.groupstate.domain.usecase.FindUnpublishedGroupStateUseCase
import com.cbgm.sparrow.feature.conversationorchestration.groupstate.domain.usecase.PrepareEmptyGroupPinUseCase
import com.cbgm.sparrow.feature.conversationorchestration.groupstate.domain.usecase.PrepareGroupTitleUseCase
import com.cbgm.sparrow.feature.conversationorchestration.groupstate.domain.usecase.RequireGroupAdminContextUseCase
import com.cbgm.sparrow.feature.conversationorchestration.groupstate.domain.usecase.SaveGroupPinUseCase
import com.cbgm.sparrow.feature.conversationorchestration.groupstate.domain.usecase.SaveGroupTitleUseCase
import com.cbgm.sparrow.feature.conversationorchestration.groupstate.domain.workflow.metadata.ChangeGroupTitleWorkflow
import com.cbgm.sparrow.feature.conversationorchestration.groupstate.domain.workflow.metadata.PublishGroupTitleWorkflow
import com.cbgm.sparrow.feature.conversationorchestration.groupstate.domain.workflow.pin.CloseGroupExpensesWorkflow
import com.cbgm.sparrow.feature.conversationorchestration.groupstate.domain.workflow.pin.PublishGroupPinWorkflow
import com.cbgm.sparrow.feature.conversationorchestration.groupstate.domain.workflow.pin.UnpinGroupMessageWorkflow
import com.cbgm.sparrow.feature.conversationorchestration.groupstate.runtime.UnpublishedGroupStateRunner
import com.cbgm.sparrow.feature.conversationorchestration.message.domain.usecase.DeleteLocalExpenseReceiptsUseCase
import com.cbgm.sparrow.feature.conversationorchestration.message.domain.usecase.EnqueueGroupPacketsUseCase
import com.cbgm.sparrow.feature.conversationorchestration.message.domain.usecase.RecordPacketSendFailureUseCase
import com.cbgm.sparrow.feature.membership.domain.usecase.AuthorizeGroupMetadataUseCase
import com.cbgm.sparrow.protocol.packet.GroupPinUpdatedPacket
import com.cbgm.sparrow.protocol.packet.GroupTitleUpdatedPacket
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import kotlin.reflect.KClass
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Local group state is saved and shown at once as unpublished; only the publishing waits for the group administration
 * lock. A failed or interrupted publish leaves it unpublished, so a retry or the recovery after a restart still
 * reaches every member, while a newer state from another admin wins and is never overwritten or republished.
 */
class GroupStateRetryTest {
    private val outbox = FakeOutbox()
    private val titles = FakeTitleRepository(GroupTitleState(GROUP_ID, "Old title", changedAtEpochMilliseconds = 5L))
    private val pins = FakePinRepository(boardPin())
    private val attachments = FakeAttachmentOperations()
    private val sentMessages = mutableListOf<GroupMessageCommand.SendMessage>()
    private val locks = WorkflowLocks()

    @Test
    fun changingTheTitleTakesNoLockSoItIsShownAtOnce() {
        assertNull(changeTitleWorkflow(engine).lockKey(GroupAdministrationCommand.ChangeTitle(GROUP_ID, "New title")))
        assertNull(unpinWorkflow(engine).lockKey(GroupMessageCommand.UnpinMessage(GROUP_ID)))
    }

    @Test
    fun newTitleIsShownWhileAMembershipChangeHoldsTheAdministrationLock() = runBlocking {
        val lockHeld = CompletableDeferred<Unit>()
        val membershipChangeDone = CompletableDeferred<Unit>()
        val membershipChange =
            launch {
                locks.withLock(groupAdministrationLockKey(GROUP_ID)) {
                    lockHeld.complete(Unit)
                    membershipChangeDone.await()
                }
            }
        lockHeld.await()

        val change = async { engine.run(GroupAdministrationCommand.ChangeTitle(GROUP_ID, "New title")) }
        withTimeout(5_000) { while (titles.stored.title != "New title") delay(1) }
        assertTrue(titles.stored.publishPending)
        assertTrue(outbox.enqueued.isEmpty())

        membershipChangeDone.complete(Unit)
        change.await().getOrThrow()
        membershipChange.join()
        assertFalse(titles.stored.publishPending)
        assertEquals(RECIPIENTS, titlePackets().filter { it.second.title == "New title" }.map { it.first }.toSet())
    }

    @Test
    fun titleThatCouldNotBeQueuedIsShownAndSentAgainOnRetry() = runBlocking {
        outbox.failures = RECIPIENTS.size

        assertTrue(engine.run(GroupAdministrationCommand.ChangeTitle(GROUP_ID, "New title")).isFailure)
        assertEquals("New title", titles.stored.title)
        assertTrue(titles.stored.publishPending)

        engine.run(GroupAdministrationCommand.ChangeTitle(GROUP_ID, "New title")).getOrThrow()
        assertFalse(titles.stored.publishPending)
        assertEquals(RECIPIENTS, titlePackets().filter { it.second.title == "New title" }.map { it.first }.toSet())
    }

    @Test
    fun recoveryPublishesATitleThatWasNeverQueued() = runBlocking {
        outbox.failures = RECIPIENTS.size
        assertTrue(engine.run(GroupAdministrationCommand.ChangeTitle(GROUP_ID, "New title")).isFailure)

        recoveryRunner().run().getOrThrow()

        assertFalse(titles.stored.publishPending)
        assertEquals(RECIPIENTS, titlePackets().filter { it.second.title == "New title" }.map { it.first }.toSet())
    }

    @Test
    fun unchangedPublishedTitleSendsNothing() = runBlocking {
        engine.run(GroupAdministrationCommand.ChangeTitle(GROUP_ID, "Old title")).getOrThrow()
        assertTrue(outbox.enqueued.isEmpty())
    }

    @Test
    fun newerRemoteTitleReplacesAnUnpublishedOneAndIsNotRepublished() = runBlocking {
        outbox.failures = RECIPIENTS.size
        assertTrue(engine.run(GroupAdministrationCommand.ChangeTitle(GROUP_ID, "New title")).isFailure)
        val remote = GroupTitleState(GROUP_ID, "Remote title", changedAtEpochMilliseconds = Long.MAX_VALUE)
        titles.stored = remote

        recoveryRunner().run().getOrThrow()
        engine.run(GroupStateCommand.PublishGroupTitle(GROUP_ID)).getOrThrow()

        assertEquals(remote, titles.stored)
        assertTrue(outbox.enqueued.isEmpty())
    }

    @Test
    fun titleReplacedByANewerRemoteOneWhilePublishingStaysTheRemoteOne() = runBlocking {
        val remote = GroupTitleState(GROUP_ID, "Remote title", changedAtEpochMilliseconds = Long.MAX_VALUE)
        val engine = workflowEngine(titleCodec = ArrivingRemoteTitleCodec(remote))

        engine.run(GroupAdministrationCommand.ChangeTitle(GROUP_ID, "New title")).getOrThrow()

        assertEquals(remote, titles.stored)
    }

    @Test
    fun publishedTitleDoesNotReplaceANewerOneThatArrivedMeanwhile() = runBlocking {
        val newer = GroupTitleState(GROUP_ID, "Remote title", changedAtEpochMilliseconds = 100L)
        titles.stored = newer
        SaveGroupTitleUseCase(titles)(GroupTitleState(GROUP_ID, "Local title", changedAtEpochMilliseconds = 50L))
            .getOrThrow()
        assertEquals(newer, titles.stored)
    }

    @Test
    fun unpinThatCouldNotBeQueuedIsShownAndSentAgainOnRetry() = runBlocking {
        pins.stored = messagePin()
        outbox.failures = 1

        assertTrue(engine.run(GroupMessageCommand.UnpinMessage(GROUP_ID)).isFailure)
        assertNull(pins.stored?.messageId)
        assertEquals(true, pins.stored?.publishPending)

        engine.run(GroupMessageCommand.UnpinMessage(GROUP_ID)).getOrThrow()
        assertEquals(false, pins.stored?.publishPending)
        assertEquals(RECIPIENTS, pinPackets().filter { it.second.messageId == null }.map { it.first }.toSet())
    }

    @Test
    fun closingExpensesAgainAfterAFailureAnnouncesTheClosingOnceAndReleasesThePin() = runBlocking {
        outbox.failures = 1

        assertTrue(engine.run(GroupMessageCommand.CloseExpenses(GROUP_ID)).isFailure)
        assertEquals(BOARD_ID, pins.stored?.activeExpenseBoard?.id)

        engine.run(GroupMessageCommand.CloseExpenses(GROUP_ID)).getOrThrow()
        assertEquals(1, sentMessages.size)
        assertNull(pins.stored?.messageId)
        assertEquals(RECIPIENTS, pinPackets().filter { it.second.messageId == null }.map { it.first }.toSet())
    }

    private fun titlePackets(): List<Pair<String, GroupTitleUpdatedPacket>> =
        outbox.enqueued.mapNotNull { (contactId, packet) -> (packet as? GroupTitleUpdatedPacket)?.let { contactId to it } }

    private fun pinPackets(): List<Pair<String, GroupPinUpdatedPacket>> =
        outbox.enqueued.mapNotNull { (contactId, packet) -> (packet as? GroupPinUpdatedPacket)?.let { contactId to it } }

    private val enqueue = EnqueueGroupPacketsUseCase(outbox, RecordPacketSendFailureUseCase(WorkflowEngineStub))
    private val admin =
        RequireGroupAdminContextUseCase(
            AuthorizeGroupMetadataUseCase(FakeGroupMembershipRepository(recipients = RECIPIENTS)),
            FakeSigningKeys
        )
    private val findUnpublished =
        FindUnpublishedGroupStateUseCase(titles, FakeDescriptionRepository, FakeAvatarRepository, pins)
    private val engine = workflowEngine()

    private fun changeTitleWorkflow(engine: WorkflowEngine) =
        ChangeGroupTitleWorkflow(
            engine = engine,
            requireGroupAdminContext = admin,
            prepareGroupTitle = PrepareGroupTitleUseCase(titles),
            saveGroupTitle = SaveGroupTitleUseCase(titles)
        )

    private fun unpinWorkflow(engine: WorkflowEngine) =
        UnpinGroupMessageWorkflow(
            engine = engine,
            pinRepository = pins,
            requireGroupAdminContext = admin,
            prepareEmptyGroupPin = PrepareEmptyGroupPinUseCase(),
            saveGroupPin = SaveGroupPinUseCase(pins)
        )

    private fun recoveryRunner() = UnpublishedGroupStateRunner(engine, findUnpublished)

    private fun workflowEngine(titleCodec: GroupTitlePacketCodec = FakeTitleCodec): WorkflowEngine {
        lateinit var engine: WorkflowEngine
        val registry =
            WorkflowRegistry {
                listOf(
                    changeTitleWorkflow(engine),
                    PublishGroupTitleWorkflow(
                        titleRepository = titles,
                        findUnpublishedGroupState = findUnpublished,
                        requireGroupAdminContext = admin,
                        createGroupTitlePackets = CreateGroupTitlePacketsUseCase(titleCodec),
                        enqueueGroupPackets = enqueue
                    ),
                    unpinWorkflow(engine),
                    PublishGroupPinWorkflow(
                        pinRepository = pins,
                        findUnpublishedGroupState = findUnpublished,
                        requireGroupAdminContext = admin,
                        createGroupPinPackets = CreateGroupPinPacketsUseCase(FakePinCodec),
                        enqueueGroupPackets = enqueue
                    ),
                    CloseGroupExpensesWorkflow(
                        engine = engine,
                        pinRepository = pins,
                        requireGroupAdminContext = admin,
                        findExpenseBoardClosure = FindExpenseBoardClosureUseCase(attachments),
                        createExpenseBoard = CreateExpenseBoardUseCase(),
                        prepareEmptyGroupPin = PrepareEmptyGroupPinUseCase(),
                        createGroupPinPackets = CreateGroupPinPacketsUseCase(FakePinCodec),
                        enqueueGroupPackets = enqueue,
                        saveGroupPin = SaveGroupPinUseCase(pins),
                        deleteLocalExpenseReceipts = DeleteLocalExpenseReceiptsUseCase(attachments, FakeAttachmentFiles)
                    ),
                    RecordingSendMessageWorkflow()
                )
            }
        engine = WorkflowEngine(locks, registry)
        return engine
    }

    /** Stores a newer title from another admin while the local one is being signed. */
    private inner class ArrivingRemoteTitleCodec(
        private val remote: GroupTitleState
    ) : GroupTitlePacketCodec by FakeTitleCodec {
        override suspend fun create(state: GroupTitleState, context: GroupAdminContext): Result<GroupTitleUpdatedPacket> {
            titles.stored = remote
            return FakeTitleCodec.create(state, context)
        }
    }

    /** Stores the sent closing announcement like the real send does. */
    private inner class RecordingSendMessageWorkflow : WorkflowStateMachine<GroupMessageCommand.SendMessage, RecordingSendMessageWorkflow.Send, Unit> {
        override val commandType: KClass<GroupMessageCommand.SendMessage> = GroupMessageCommand.SendMessage::class

        override fun firstStep(command: GroupMessageCommand.SendMessage): Send = Send(command)

        override suspend fun perform(step: Send) {
            sentMessages += step.command
            attachments.closures.value += step.command.parts.filterIsInstance<ExpenseBoardClosed>()
        }

        override fun transition(step: Send, outcome: Unit): Transition<Send> = Transition.Complete()

        inner class Send(
            val command: GroupMessageCommand.SendMessage
        ) : WorkflowStep
    }

    private companion object {
        const val MESSAGE_ID = "message-1"
        const val BOARD_ID = "board-1"
        val RECIPIENTS = setOf("alice", "bob")

        /** Packet send failures are not recorded for group state packets, so this engine is never used. */
        val WorkflowEngineStub = WorkflowEngine(WorkflowLocks(), WorkflowRegistry { emptyList() })

        fun messagePin() = pin(messageId = MESSAGE_ID, parts = emptyList())

        fun boardPin() =
            pin(
                messageId = "board-message",
                parts = listOf(ExpenseBoard(BOARD_ID, currencyCode = "EUR", activatedAtEpochMilliseconds = 1L))
            )

        fun pin(messageId: String, parts: List<com.cbgm.sparrow.core.base.messagepart.domain.model.MessagePart>) =
            GroupPinState(
                groupId = GROUP_ID,
                messageId = messageId,
                messageSentAtEpochMilliseconds = 1L,
                messageSenderSigningPublicKey = ADMIN_KEYS.publicKey,
                senderContactId = null,
                isMine = true,
                parts = parts,
                replyToMessageId = null,
                pinnedAtEpochMilliseconds = 2L,
                changedAtEpochMilliseconds = 2L
            )
    }
}
