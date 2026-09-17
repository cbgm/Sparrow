package com.cbgm.sparrow.feature.membership.domain.model

import com.cbgm.sparrow.feature.contacts.domain.model.SparrowIdentity
import com.cbgm.sparrow.feature.identity.domain.model.ContactVerificationStatus
import com.cbgm.sparrow.feature.identity.domain.model.KeyExchangeStatus

internal object GroupOwnerIdentityReplacementPolicy {
    fun requiresReplacement(
        existing: SparrowIdentity?,
        encryptionPublicKey: ByteArray,
        signingPublicKey: ByteArray
    ): Boolean {
        existing ?: return false

        val encryptionKeyMatches =
            existing.encryptionPublicKey.contentEquals(encryptionPublicKey)
        val signingKeyMatches =
            existing.signingPublicKey.contentEquals(signingPublicKey)

        if (encryptionKeyMatches && signingKeyMatches) {
            return false
        }

        val identityIsPinned =
            existing.keyExchangeStatus == KeyExchangeStatus.MUTUAL ||
                existing.verificationStatus == ContactVerificationStatus.VERIFIED

        if (identityIsPinned) {
            check(encryptionKeyMatches) {
                "Contact encryption identity conflicts with the remote identity handshake"
            }
            check(signingKeyMatches) {
                "Contact signing identity conflicts with the remote identity handshake"
            }
        }

        return true
    }
}
