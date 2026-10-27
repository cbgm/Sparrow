package com.cbgm.sparrow.feature.identity.domain.model

import kotlin.test.Test
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/** Recovery picks the original invitation only when accepting it will succeed. */
class ApprovedIdentityReconnectionTest {
    @Test
    fun validOriginalInvitationIsUsable() {
        val approval = approval()
        assertTrue(approval.hasOriginalInvitation(NOW))
        approval.requireOriginalOffer(NOW)
    }

    @Test
    fun damagedOrExpiredOriginalInvitationIsNotUsable() {
        listOf(
            approval(signingKey = ByteArray(16)),
            approval(challenge = ByteArray(31)),
            approval(createdAt = EXPIRES_AT),
            approval(expiresAt = NOW),
            approval(challenge = null)
        ).forEach { approval ->
            assertFalse(approval.hasOriginalInvitation(NOW))
            assertFailsWith<Exception> { approval.requireOriginalOffer(NOW) }
        }
    }

    private fun approval(
        challenge: ByteArray? = ByteArray(32) { 3 },
        signingKey: ByteArray = ByteArray(32) { 2 },
        createdAt: Long = NOW - 10,
        expiresAt: Long = EXPIRES_AT
    ) = ApprovedIdentityReconnection(
        peerId = "peer",
        approvalId = "approval",
        originalInviteChallenge = challenge,
        originalInviteCreatedAtEpochMilliseconds = createdAt,
        originalInviteExpiresAtEpochMilliseconds = expiresAt,
        originalInviterEncryptionPublicKey = ByteArray(32) { 1 },
        originalInviterSigningPublicKey = signingKey
    )

    private companion object {
        const val NOW = 1_000L
        const val EXPIRES_AT = 2_000L
    }
}
