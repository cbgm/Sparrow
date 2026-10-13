package com.cbgm.sparrow.feature.chats.domain.usecase.group

import com.cbgm.sparrow.core.message.domain.model.MessageContentStatus
import com.cbgm.sparrow.core.message.domain.model.MessageDeliveryStatus
import com.cbgm.sparrow.core.messagepart.domain.model.Expense
import com.cbgm.sparrow.core.messagepart.domain.model.ExpenseAllocation
import com.cbgm.sparrow.core.messagepart.domain.model.ExpenseBoard
import com.cbgm.sparrow.core.messagepart.domain.model.ExpenseMemberId
import com.cbgm.sparrow.core.messagepart.domain.model.MessagePart
import com.cbgm.sparrow.feature.attachments.domain.model.AttachmentMessageContext
import com.cbgm.sparrow.feature.attachments.domain.repository.MessageAttachmentOperationsRepository
import com.cbgm.sparrow.feature.chats.domain.model.MessageSecurity
import com.cbgm.sparrow.feature.chats.domain.model.group.GroupExpenseSummary
import com.cbgm.sparrow.feature.chats.domain.model.group.GroupMessage
import com.cbgm.sparrow.feature.chats.domain.model.group.GroupPin
import com.cbgm.sparrow.feature.chats.domain.repository.group.GroupPinReadRepository
import com.cbgm.sparrow.feature.expenses.domain.usecase.CalculateExpenseBalancesUseCase
import com.cbgm.sparrow.protocol.identity.LocalPublicIdentity
import com.cbgm.sparrow.protocol.identity.LocalPublicIdentityProvider
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class ObserveGroupExpenseSummaryUseCaseTest {
    private val localKey = byteArrayOf(1, 2, 3)
    private val localId = ExpenseMemberId.fromSigningPublicKey(localKey)
    private val board = ExpenseBoard("board-1", "EUR", 1L)

    @Test
    fun summarizesActiveBoardForLocalMember() = runTest {
        val expenses = listOf(
            expense("hotel", "alex", 9000L, listOf("alex", localId, "mia")),
            expense("dinner", localId, 3000L, listOf("alex", localId, "mia")),
            expense("other-board", "alex", 500L, listOf("alex"), boardId = "board-2")
        )

        val summary = useCase(pin(board), expenses)("group").first()

        assertEquals(
            GroupExpenseSummary(
                boardId = "board-1",
                currencyCode = "EUR",
                expenseCount = 2,
                totalMinor = 12000L,
                localBalanceMinor = -1000L
            ),
            summary
        )
    }

    @Test
    fun emitsNullWithoutActiveBoard() = runTest {
        val closed = board.copy(closedAtEpochMilliseconds = 2L)

        assertNull(useCase(pin(closed), emptyList())("group").first())
        assertNull(useCase(null, emptyList())("group").first())
    }

    @Test
    fun reportsSettledWhenLocalMemberHasNoShare() = runTest {
        val summary = useCase(pin(board), listOf(expense("taxi", "alex", 1000L, listOf("alex", "mia"))))("group").first()

        assertEquals(0L, summary?.localBalanceMinor)
    }

    private fun useCase(pin: GroupPin?, expenses: List<Expense>) =
        ObserveGroupExpenseSummaryUseCase(
            pins = FakePins(pin),
            attachments = FakeAttachments(expenses),
            localIdentity = FakeIdentity(LocalPublicIdentity(byteArrayOf(9), localKey)),
            calculateBalances = CalculateExpenseBalancesUseCase()
        )

    private fun expense(
        id: String,
        payer: String,
        amount: Long,
        members: List<String>,
        boardId: String = "board-1"
    ) = Expense(
        id = id,
        boardId = boardId,
        description = id,
        amountMinor = amount,
        currencyCode = "EUR",
        paidByMemberId = payer,
        allocations = members.map { ExpenseAllocation(it, amount / members.size) },
        occurredAtEpochMilliseconds = 2L
    )

    private fun pin(board: ExpenseBoard) = GroupPin(
        message = GroupMessage(
            id = "pin",
            isMine = true,
            timestamp = 1L,
            security = MessageSecurity.END_TO_END_ENCRYPTED,
            contentStatus = MessageContentStatus.READABLE,
            deliveryStatus = MessageDeliveryStatus.NOT_APPLICABLE,
            parts = listOf(board)
        ),
        pinnedAtEpochMilliseconds = 1L
    )

    private class FakePins(
        pin: GroupPin?
    ) : GroupPinReadRepository {
        private val state = MutableStateFlow(pin)

        override fun observe(groupId: String): Flow<GroupPin?> = state

        override suspend fun loadAttachment(groupId: String, attachmentId: String): Result<ByteArray> =
            error("Not used")
    }

    private class FakeIdentity(
        private val identity: LocalPublicIdentity
    ) : LocalPublicIdentityProvider {
        override suspend fun getLocalPublicIdentity(): Result<LocalPublicIdentity> = Result.success(identity)
    }

    private class FakeAttachments(
        private val expenses: List<Expense>
    ) : MessageAttachmentOperationsRepository {
        override fun observeExpenses(conversationId: String): Flow<List<Expense>> = flowOf(expenses)

        override suspend fun persistOutgoing(
            messageId: String,
            parts: List<MessagePart>,
            context: AttachmentMessageContext
        ): Result<List<MessagePart>> = error("Not used")

        override suspend fun persistIncoming(
            messageId: String,
            parts: List<MessagePart>,
            context: AttachmentMessageContext
        ) = error("Not used")

        override suspend fun updateConversationDisplayName(conversationId: String, displayName: String, isGroup: Boolean) =
            error("Not used")

        override suspend fun updateMessagePart(messageId: String, part: MessagePart): Result<Unit> = error("Not used")

        override suspend fun messageParts(messageId: String): Result<List<MessagePart>> = error("Not used")

        override suspend fun prepareOutgoing(messageId: String): Result<List<MessagePart>> = error("Not used")

        override suspend fun loadDetachedBytes(part: MessagePart): Result<ByteArray> = error("Not used")

        override suspend fun deleteForMessages(messageIds: List<String>) = error("Not used")

        override fun observeByMessageIds(messageIds: List<String>): Flow<Map<String, List<MessagePart>>> =
            error("Not used")

        override fun cacheIncoming(messageId: String) = error("Not used")
    }
}
