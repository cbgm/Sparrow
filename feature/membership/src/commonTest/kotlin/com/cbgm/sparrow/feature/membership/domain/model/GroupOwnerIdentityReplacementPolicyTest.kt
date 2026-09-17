package com.cbgm.sparrow.feature.membership.domain.model

import com.cbgm.sparrow.feature.contacts.domain.model.SparrowIdentity
import com.cbgm.sparrow.feature.identity.domain.model.ContactVerificationStatus
import com.cbgm.sparrow.feature.identity.domain.model.KeyExchangeStatus
import kotlin.test.Test
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class GroupOwnerIdentityReplacementPolicyTest {
    @Test
    fun missingIdentityDoesNotRequireReplacement() {
        assertFalse(
            GroupOwnerIdentityReplacementPolicy.requiresReplacement(
                existing = null,
                encryptionPublicKey = encryptionKey,
                signingPublicKey = signingKey
            )
        )
    }

    @Test
    fun matchingIdentityDoesNotRequireReplacement() {
        assertFalse(
            GroupOwnerIdentityReplacementPolicy.requiresReplacement(
                existing = identity(),
                encryptionPublicKey = encryptionKey.copyOf(),
                signingPublicKey = signingKey.copyOf()
            )
        )
    }

    @Test
    fun changedOneWayUnverifiedIdentityCanBeReplaced() {
        assertTrue(
            GroupOwnerIdentityReplacementPolicy.requiresReplacement(
                existing = identity(),
                encryptionPublicKey = changedEncryptionKey,
                signingPublicKey = changedSigningKey
            )
        )
    }

    @Test
    fun changedMutualIdentityIsRejected() {
        assertFailsWith<IllegalStateException> {
            GroupOwnerIdentityReplacementPolicy.requiresReplacement(
                existing = identity(keyExchangeStatus = KeyExchangeStatus.MUTUAL),
                encryptionPublicKey = changedEncryptionKey,
                signingPublicKey = signingKey
            )
        }
    }

    @Test
    fun changedVerifiedIdentityIsRejected() {
        assertFailsWith<IllegalStateException> {
            GroupOwnerIdentityReplacementPolicy.requiresReplacement(
                existing =
                    identity(
                        verificationStatus = ContactVerificationStatus.VERIFIED
                    ),
                encryptionPublicKey = encryptionKey,
                signingPublicKey = changedSigningKey
            )
        }
    }

    private fun identity(
        verificationStatus: ContactVerificationStatus = ContactVerificationStatus.UNVERIFIED,
        keyExchangeStatus: KeyExchangeStatus = KeyExchangeStatus.ONE_WAY
    ): SparrowIdentity =
        SparrowIdentity(
            encryptionPublicKey = encryptionKey.copyOf(),
            signingPublicKey = signingKey.copyOf(),
            verificationStatus = verificationStatus,
            keyExchangeStatus = keyExchangeStatus,
            updatedAtEpochMilliseconds = 1L
        )

    private companion object {
        val encryptionKey = byteArrayOf(1, 2, 3)
        val signingKey = byteArrayOf(4, 5, 6)
        val changedEncryptionKey = byteArrayOf(7, 8, 9)
        val changedSigningKey = byteArrayOf(10, 11, 12)
    }
}
