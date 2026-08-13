package com.cbgm.securechat.feature.chats.data.group.invitation

import kotlin.test.Test
import kotlin.test.assertEquals

class IncomingInvitationTimestampTest {
    @Test
    fun receiverClockBehindUsesSignedCreationTimestamp() {
        assertEquals(
            expected = 1_000L,
            actual =
                resolveInvitationUpdatedAt(
                    createdAtEpochMilliseconds = 1_000L,
                    candidateAtEpochMilliseconds = 967L
                )
        )
    }

    @Test
    fun receiverClockAheadUsesLocalTimestamp() {
        assertEquals(
            expected = 1_050L,
            actual =
                resolveInvitationUpdatedAt(
                    createdAtEpochMilliseconds = 1_000L,
                    candidateAtEpochMilliseconds = 1_050L
                )
        )
    }

    @Test
    fun acceptedInvitationNeverMovesUpdateTimestampBeforeCreation() {
        val invitationCreatedAt = 1_000L
        val receiverNow = 967L

        assertEquals(
            expected = invitationCreatedAt,
            actual =
                resolveInvitationUpdatedAt(
                    createdAtEpochMilliseconds = invitationCreatedAt,
                    candidateAtEpochMilliseconds = receiverNow
                )
        )
    }
}
