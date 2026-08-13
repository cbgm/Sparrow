package com.cbgm.securechat.feature.chats.data.group.invitation

import com.cbgm.securechat.feature.contacts.domain.model.ContactVerificationStatus
import com.cbgm.securechat.feature.contacts.domain.model.KeyExchangeStatus
import com.cbgm.securechat.feature.contacts.domain.model.SecureChatIdentity

internal object InvitationIdentityPolicy {
    fun requiresReplacement(
        existing: SecureChatIdentity?,
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
