package com.cbgm.sparrow.core.messagepart.domain.model

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class PollPolicyTest {
    @Test
    fun `poll is open before expiry`() {
        assertFalse(poll(expiresAtEpochMilliseconds = 60_000L).isClosedAt(59_999L))
    }

    @Test
    fun `poll is closed at expiry`() {
        assertTrue(poll(expiresAtEpochMilliseconds = 60_000L).isClosedAt(60_000L))
    }

    @Test
    fun `manually closed poll stays closed before expiry`() {
        assertTrue(
            poll(
                expiresAtEpochMilliseconds = 60_000L,
                closedAtEpochMilliseconds = 10_000L
            ).isClosedAt(20_000L)
        )
    }

    private fun poll(
        expiresAtEpochMilliseconds: Long? = null,
        closedAtEpochMilliseconds: Long? = null
    ): Poll =
        Poll(
            id = "poll-1",
            question = "Question?",
            options =
                listOf(
                    PollOption(id = "1", text = "One"),
                    PollOption(id = "2", text = "Two")
                ),
            expiresAtEpochMilliseconds = expiresAtEpochMilliseconds,
            closedAtEpochMilliseconds = closedAtEpochMilliseconds
        )
}
