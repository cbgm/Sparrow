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
}
