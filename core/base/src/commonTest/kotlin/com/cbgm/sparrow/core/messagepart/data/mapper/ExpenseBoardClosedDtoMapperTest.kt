package com.cbgm.sparrow.core.messagepart.data.mapper

import com.cbgm.sparrow.core.messagepart.data.model.ExpenseBoardClosedDto
import com.cbgm.sparrow.core.messagepart.domain.model.ExpenseBoardClosed
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class ExpenseBoardClosedDtoMapperTest {
    @Test
    fun closeEventRoundTripsWithOriginalBoardId() {
        val original = ExpenseBoardClosedDto("close-1", "board-1", 101L)
        val domain = assertIs<ExpenseBoardClosed>(original.toMessagePart())
        assertEquals("board-1", domain.boardId)
        assertEquals(original, domain.toDto())
    }
}
