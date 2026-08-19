package com.cbgm.sparrow.feature.chats.domain.model.direct

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class DirectPendingAuthorizationMessagePolicyTest {
    @Test
    fun messageExpiresExactlyTwoDaysAfterItWasQueued() {
        val createdAt = 1_000L
        val expiresAt = DirectPendingAuthorizationMessagePolicy.expiresAtEpochMilliseconds(createdAt)

        assertFalse(
            DirectPendingAuthorizationMessagePolicy.isExpired(
                createdAtEpochMilliseconds = createdAt,
                nowEpochMilliseconds = createdAt + 24L * 60L * 60L * 1_000L
            )
        )
        assertFalse(DirectPendingAuthorizationMessagePolicy.isExpired(createdAt, expiresAt - 1L))
        assertTrue(DirectPendingAuthorizationMessagePolicy.isExpired(createdAt, expiresAt))
    }
}
