package com.cbgm.sparrow.feature.chats.data.group.invitation

import com.cbgm.sparrow.feature.contacts.domain.model.ContactVerificationStatus
import com.cbgm.sparrow.feature.contacts.domain.model.KeyExchangeStatus
import com.cbgm.sparrow.feature.contacts.domain.model.SparrowIdentity

internal object InvitationIdentityPolicy {
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
                "Contact encryption identity conflicts with the invitation handshake"
            }
            check(signingKeyMatches) {
                "Contact signing identity conflicts with the invitation handshake"
            }
        }

        return true
    }
}
