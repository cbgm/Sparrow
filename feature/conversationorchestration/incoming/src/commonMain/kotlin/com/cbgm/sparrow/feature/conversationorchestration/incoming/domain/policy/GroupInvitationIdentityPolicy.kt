package com.cbgm.sparrow.feature.conversationorchestration.incoming.domain.policy

/** A signed group offer proves possession of its PROPOSED signing key, not continuity
 * with any previously accepted contact identity. Do not update trusted keys on receipt.
 */
enum class GroupInvitationIdentityDisposition {
    FIRST_CONTACT,
    MATCHES_STORED_IDENTITY,
    REQUIRES_REVIEW
}

object GroupInvitationIdentityPolicy {
    fun evaluate(
        storedEncryptionPublicKey: ByteArray?,
        storedSigningPublicKey: ByteArray?,
        offeredEncryptionPublicKey: ByteArray,
        offeredSigningPublicKey: ByteArray
    ): GroupInvitationIdentityDisposition {
        require(offeredEncryptionPublicKey.size == 32 && offeredSigningPublicKey.size == 32) {
            "A group invitation must supply complete public identity keys"
        }
        if (storedEncryptionPublicKey == null && storedSigningPublicKey == null) {
            return GroupInvitationIdentityDisposition.FIRST_CONTACT
        }
        check(storedEncryptionPublicKey != null && storedSigningPublicKey != null) {
            "A stored contact identity is incomplete"
        }
        return if (storedEncryptionPublicKey.contentEquals(offeredEncryptionPublicKey) &&
            storedSigningPublicKey.contentEquals(offeredSigningPublicKey)
        ) {
            GroupInvitationIdentityDisposition.MATCHES_STORED_IDENTITY
        } else {
            GroupInvitationIdentityDisposition.REQUIRES_REVIEW
        }
    }
}
