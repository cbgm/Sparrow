package com.cbgm.sparrow.feature.membership.domain.model

import kotlin.test.Test
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/** Only a member-side handshake stores the owner's keys; every owner-side check uses the same rule. */
class MembershipHandshakeTest {
    @Test
    fun handshakeWithoutOwnerKeysIsOwnerSide() {
        val handshake = handshake()
        assertTrue(handshake.isOwnerSide)
        assertTrue(handshake.isOwnerSideInvitationOf(GROUP_ID, PEER_ID))
        handshake.requireOwnerSideJoinOf(PEER_ID)
    }

    @Test
    fun anyStoredOwnerKeyMakesItMemberSide() {
        listOf(
            handshake(ownerEncryptionPublicKey = ByteArray(32)),
            handshake(ownerSigningPublicKey = ByteArray(32))
        ).forEach { handshake ->
            assertFalse(handshake.isOwnerSide)
            assertFalse(handshake.isOwnerSideInvitationOf(GROUP_ID, PEER_ID))
            assertFailsWith<IllegalStateException> { handshake.requireOwnerSideJoinOf(PEER_ID) }
        }
    }

    private fun handshake(ownerEncryptionPublicKey: ByteArray? = null, ownerSigningPublicKey: ByteArray? = null) =
        MembershipHandshake(
            sourceId = "source",
            groupId = GROUP_ID,
            peerId = PEER_ID,
            createdAtEpochMilliseconds = 1,
            updatedAtEpochMilliseconds = 1,
            status = MembershipStatus.STAGED,
            ownerEncryptionPublicKey = ownerEncryptionPublicKey,
            ownerSigningPublicKey = ownerSigningPublicKey
        )

    private companion object {
        const val GROUP_ID = "group"
        const val PEER_ID = "peer"
    }
}
