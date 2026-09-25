package com.cbgm.sparrow.feature.identity.domain.model

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class RemotePeerIdentityEncryptionTest {
    private fun identity(
        exchange: KeyExchangeStatus = KeyExchangeStatus.MUTUAL,
        encryptionKey: ByteArray = ByteArray(32) { 1 },
        signingKey: ByteArray = ByteArray(32) { 2 }
    ) = RemotePeerIdentity(
        peerId = "peer",
        encryptionPublicKey = encryptionKey,
        signingPublicKey = signingKey,
        verificationStatus = ContactVerificationStatus.UNVERIFIED,
        keyExchangeStatus = exchange,
        verifiedByContact = false,
        locallyImported = false,
        updatedAtEpochMilliseconds = 1L
    )

    @Test fun mutualKeysAllowEncryptedDeliveryWithoutCopyingVerification() {
        assertTrue(identity().hasDirectMessageEncryptionKeys())
    }

    @Test fun oneWayOrAbsentKeysCannotPrepareDirectContent() {
        assertFalse(identity(exchange = KeyExchangeStatus.ONE_WAY).hasDirectMessageEncryptionKeys())
        assertFalse(identity(encryptionKey = byteArrayOf()).hasDirectMessageEncryptionKeys())
        assertFalse(identity(signingKey = byteArrayOf()).hasDirectMessageEncryptionKeys())
        assertFalse(identity(encryptionKey = ByteArray(31)).hasDirectMessageEncryptionKeys())
        assertFalse(identity(signingKey = ByteArray(33)).hasDirectMessageEncryptionKeys())
        val absent: RemotePeerIdentity? = null
        assertFalse(absent.hasDirectMessageEncryptionKeys())
    }
}
