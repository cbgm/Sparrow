package com.cbgm.sparrow.feature.membership.domain.model

/** Membership-owned authorization for one received group welcome; no database entities cross modules. */
data class GroupIncomingWelcomeAuthorization(
    val isFirstWelcome: Boolean,
    val sourceInvitationId: String?,
    val previousSigningKeysByContactId: Map<String, ByteArray>,
    val priorAdminEncryptionPublicKey: ByteArray?,
    val priorAdminSigningPublicKey: ByteArray?
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is GroupIncomingWelcomeAuthorization) return false
        return isFirstWelcome == other.isFirstWelcome &&
            sourceInvitationId == other.sourceInvitationId &&
            previousSigningKeysByContactId.keys == other.previousSigningKeysByContactId.keys &&
            previousSigningKeysByContactId.all { (contactId, key) ->
                key.contentEquals(other.previousSigningKeysByContactId.getValue(contactId))
            } &&
            priorAdminEncryptionPublicKey.sameOptionalBytes(other.priorAdminEncryptionPublicKey) &&
            priorAdminSigningPublicKey.sameOptionalBytes(other.priorAdminSigningPublicKey)
    }

    override fun hashCode(): Int {
        var result = isFirstWelcome.hashCode()
        result = 31 * result + (sourceInvitationId?.hashCode() ?: 0)
        result = 31 * result + previousSigningKeysByContactId.entries.sumOf { (contactId, key) ->
            contactId.hashCode() xor key.contentHashCode()
        }
        result = 31 * result + (priorAdminEncryptionPublicKey?.contentHashCode() ?: 0)
        result = 31 * result + (priorAdminSigningPublicKey?.contentHashCode() ?: 0)
        return result
    }
}

private fun ByteArray?.sameOptionalBytes(other: ByteArray?): Boolean =
    if (this == null) other == null else other != null && contentEquals(other)
