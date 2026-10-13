package com.cbgm.sparrow.feature.chats.presentation.common.history.mapper

import com.cbgm.sparrow.core.message.domain.model.MessageContentStatus
import com.cbgm.sparrow.core.message.domain.model.MessageDeliveryStatus
import com.cbgm.sparrow.core.messagepart.ui.model.ExpenseBoardUi
import com.cbgm.sparrow.core.messagepart.ui.model.FileUi
import com.cbgm.sparrow.core.messagepart.ui.model.ImageUi
import com.cbgm.sparrow.core.messagepart.ui.model.LocationUi
import com.cbgm.sparrow.core.messagepart.ui.model.MessagePartUi
import com.cbgm.sparrow.core.messagepart.ui.model.TextUi
import com.cbgm.sparrow.feature.chats.domain.model.MessageSecurity
import com.cbgm.sparrow.feature.chats.presentation.common.history.model.MessageBubbleUi
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class MessagePartUiMapperTest {
    private val text = TextUi(id = "text", text = "Hello", isContentFailed = false)
    private val image = ImageUi(id = "image", mimeType = "image/jpeg", byteSize = 1L)
    private val file = FileUi(id = "file", mimeType = "application/pdf", byteSize = 1L, fileName = "a.pdf")
    private val board = ExpenseBoardUi(id = "board", currencyCode = "EUR", activatedAtEpochMilliseconds = 1L)

    @Test
    fun textMediaAndFilesAreNotStandalone() {
        val message = bubble(listOf(text, image, file))

        assertNull(message.parts.standalonePartOrNull())
        assertEquals("Hello", message.textOrNull())
        assertEquals(listOf(image, file), message.toMessageAttachmentsUi())
    }

    @Test
    fun standalonePartsAreFound() {
        val location = LocationUi(id = "location")

        assertEquals(location, listOf(location).standalonePartOrNull())
        assertEquals(board, bubble(listOf(board)).activeExpenseBoard)
        assertNull(bubble(listOf(board.copy(closedAtEpochMilliseconds = 2L))).activeExpenseBoard)
        assertEquals(board, bubble(listOf(board)).expenseActivityPartOrNull())
        assertNull(bubble(listOf(text)).expenseActivityPartOrNull())
    }

    private fun bubble(parts: List<MessagePartUi>) =
        MessageBubbleUi(
            id = "message",
            isMine = true,
            security = MessageSecurity.END_TO_END_ENCRYPTED,
            contentStatus = MessageContentStatus.READABLE,
            deliveryStatus = MessageDeliveryStatus.DELIVERED,
            parts = parts
        )
}
