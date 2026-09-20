package com.cbgm.sparrow.feature.chats.domain.model.direct

import com.cbgm.sparrow.feature.identity.domain.model.ContactVerificationStatus
import com.cbgm.sparrow.feature.identity.domain.model.KeyExchangeStatus
import com.cbgm.sparrow.feature.identity.domain.model.RemotePeerIdentity
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class IdentitySnapshotEqualityTest {
    @Test
    fun `the same stored identity with distinct key arrays does not emit again`() {
        val a = identity()
        val b = identity()
        assertTrue(a !== b)
        assertTrue(a.hasSameIdentityContent(b))
    }

    @Test
    fun `a remote key change must emit`() {
        assertFalse(identity().hasSameIdentityContent(identity(signing = byteArrayOf(6))))
        assertFalse(identity().hasSameIdentityContent(identity(encryption = byteArrayOf(7))))
    }

    @Test
    fun `a trust change must emit`() {
        assertFalse(identity().hasSameIdentityContent(identity(verified = true)))
        assertFalse(identity().hasSameIdentityContent(identity(exchange = KeyExchangeStatus.MUTUAL)))
    }

    @Test
    fun `a missing identity is distinct from a known one`() {
        val missing: RemotePeerIdentity? = null
        assertTrue(missing.hasSameIdentityContent(null))
        assertFalse(missing.hasSameIdentityContent(identity()))
        assertFalse(identity().hasSameIdentityContent(missing))
    }

    private fun identity(
        encryption: ByteArray = byteArrayOf(1),
        signing: ByteArray = byteArrayOf(2),
        verified: Boolean = false,
        exchange: KeyExchangeStatus = KeyExchangeStatus.ONE_WAY
    ) = RemotePeerIdentity(
        peerId = "contact",
        encryptionPublicKey = encryption,
        signingPublicKey = signing,
        verificationStatus = if (verified) ContactVerificationStatus.VERIFIED else ContactVerificationStatus.UNVERIFIED,
        keyExchangeStatus = exchange,
        verifiedByContact = false,
        locallyImported = true,
        updatedAtEpochMilliseconds = 1L
    )
}
