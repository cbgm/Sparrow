package com.cbgm.sparrow.feature.identity.domain.model

/** Durable approval of a particular signed invitation proposal, not proof that the
 * old owner controls the new keys. Never release outgoing plaintext or old ciphertext. */
data class ApprovedIdentityReconnection(
    val peerId: String,
    val approvalId: String,
    val originalInviteChallenge: ByteArray? = null,
    val originalInviteCreatedAtEpochMilliseconds: Long? = null,
    val originalInviteExpiresAtEpochMilliseconds: Long? = null,
    val originalInviteAutoSharesIdentity: Boolean = false,
    val originalInviterEncryptionPublicKey: ByteArray? = null,
    val originalInviterSigningPublicKey: ByteArray? = null,
    val approvedAtEpochMilliseconds: Long = 0L
) {
    fun sameApproval(other: ApprovedIdentityReconnection?): Boolean =
        other?.peerId == peerId && other.approvalId == approvalId

    /**
     * The original signed invitation, accepted again after the user approved the new keys. Fails when a part is
     * missing, a key has the wrong size or the invitation expired.
     */
    fun requireOriginalOffer(nowEpochMilliseconds: Long): IdentityExchangeOffer {
        val challenge = requireNotNull(originalInviteChallenge)
        val createdAt = requireNotNull(originalInviteCreatedAtEpochMilliseconds)
        val expiresAt = requireNotNull(originalInviteExpiresAtEpochMilliseconds)
        val encryption = requireNotNull(originalInviterEncryptionPublicKey)
        val signing = requireNotNull(originalInviterSigningPublicKey)
        check(challenge.size == KEY_SIZE && encryption.size == KEY_SIZE && signing.size == KEY_SIZE)
        check(createdAt < expiresAt && expiresAt > nowEpochMilliseconds) {
            "Approved original identity invitation expired"
        }
        return IdentityExchangeOffer(
            exchangeId = approvalId,
            createdAtEpochMilliseconds = createdAt,
            expiresAtEpochMilliseconds = expiresAt,
            inviteChallenge = challenge,
            encryptionPublicKey = encryption,
            signingPublicKey = signing,
            autoSharesIdentity = originalInviteAutoSharesIdentity
        )
    }

    /**
     * True when [requireOriginalOffer] will succeed: recovery then accepts the original invitation instead of
     * asking for a new authorization. False for a changed-key acceptance, an expired or damaged original
     * invitation or an old approval without challenge.
     */
    fun hasOriginalInvitation(nowEpochMilliseconds: Long): Boolean {
        val createdAt = originalInviteCreatedAtEpochMilliseconds ?: return false
        val expiresAt = originalInviteExpiresAtEpochMilliseconds ?: return false
        return originalInviteChallenge?.size == KEY_SIZE &&
            originalInviterEncryptionPublicKey?.size == KEY_SIZE &&
            originalInviterSigningPublicKey?.size == KEY_SIZE &&
            createdAt < expiresAt &&
            expiresAt > nowEpochMilliseconds
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as ApprovedIdentityReconnection

        if (originalInviteCreatedAtEpochMilliseconds != other.originalInviteCreatedAtEpochMilliseconds) return false
        if (originalInviteExpiresAtEpochMilliseconds != other.originalInviteExpiresAtEpochMilliseconds) return false
        if (originalInviteAutoSharesIdentity != other.originalInviteAutoSharesIdentity) return false
        if (approvedAtEpochMilliseconds != other.approvedAtEpochMilliseconds) return false
        if (peerId != other.peerId) return false
        if (approvalId != other.approvalId) return false
        if (!originalInviteChallenge.contentEquals(other.originalInviteChallenge)) return false
        if (!originalInviterEncryptionPublicKey.contentEquals(other.originalInviterEncryptionPublicKey)) return false
        if (!originalInviterSigningPublicKey.contentEquals(other.originalInviterSigningPublicKey)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = originalInviteCreatedAtEpochMilliseconds?.hashCode() ?: 0
        result = 31 * result + (originalInviteExpiresAtEpochMilliseconds?.hashCode() ?: 0)
        result = 31 * result + originalInviteAutoSharesIdentity.hashCode()
        result = 31 * result + approvedAtEpochMilliseconds.hashCode()
        result = 31 * result + peerId.hashCode()
        result = 31 * result + approvalId.hashCode()
        result = 31 * result + (originalInviteChallenge?.contentHashCode() ?: 0)
        result = 31 * result + (originalInviterEncryptionPublicKey?.contentHashCode() ?: 0)
        result = 31 * result + (originalInviterSigningPublicKey?.contentHashCode() ?: 0)
        return result
    }

    private companion object {
        const val KEY_SIZE = 32
    }
}
