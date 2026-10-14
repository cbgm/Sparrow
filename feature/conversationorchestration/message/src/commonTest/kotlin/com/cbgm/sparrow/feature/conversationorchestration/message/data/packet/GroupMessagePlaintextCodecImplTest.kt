package com.cbgm.sparrow.feature.conversationorchestration.message.data.packet

import com.cbgm.sparrow.core.messagepart.domain.model.Expense
import com.cbgm.sparrow.core.messagepart.domain.model.ExpenseAllocation
import com.cbgm.sparrow.core.messagepart.domain.model.Image
import com.cbgm.sparrow.feature.conversationorchestration.message.domain.model.GroupMessagePlaintext
import com.cbgm.sparrow.protocol.codec.createProtocolJson
import com.cbgm.sparrow.protocol.message.GroupMessageContentCodec
import com.cbgm.sparrow.protocol.message.OperationMessageCodec
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull

class GroupMessagePlaintextCodecImplTest {
    private val json = createProtocolJson()
    private val codec =
        GroupMessagePlaintextCodecImpl(GroupMessageContentCodec(json), OperationMessageCodec(json))

    @Test
    fun receiptBytesTravelInsideTheExpense() {
        val bytes = byteArrayOf(1, 2, 3, 4)

        val decoded = roundTrip(expense(Image("receipt", "image/jpeg", 4L, 10, 20, bytes = bytes)))

        val receipt = assertIs<Expense>(decoded.attachments.single()).receipt
        assertEquals("receipt", receipt?.id)
        assertContentEquals(bytes, receipt?.bytes)
        assertNull(receipt?.localFilePath)
    }

    @Test
    fun receiptWithoutBytesIsSentAsReferenceOnly() {
        val decoded = roundTrip(expense(Image("receipt", "image/jpeg", 4L, 10, 20)))

        val receipt = assertIs<Expense>(decoded.attachments.single()).receipt
        assertEquals("receipt", receipt?.id)
        assertNull(receipt?.bytes)
    }

    private fun roundTrip(expense: Expense): GroupMessagePlaintext.Message =
        assertIs<GroupMessagePlaintext.Message>(
            codec.decode(
                codec.encodeMessage(
                    messageId = "message",
                    text = "",
                    parts = listOf(expense),
                    replyToMessageId = null
                )
            )
        )

    private fun expense(receipt: Image) =
        Expense(
            id = "expense",
            boardId = "board",
            description = "Dinner",
            amountMinor = 1_000L,
            currencyCode = "EUR",
            paidByMemberId = "member",
            allocations = listOf(ExpenseAllocation("member", 1_000L)),
            occurredAtEpochMilliseconds = 1L,
            receipt = receipt
        )
}
