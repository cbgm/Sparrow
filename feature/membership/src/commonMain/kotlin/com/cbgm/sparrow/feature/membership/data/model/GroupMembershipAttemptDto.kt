package com.cbgm.sparrow.feature.membership.data.model

data class GroupMembershipAttemptDto(
    val membershipId: String,
    val sourceInvitationId: String,
    val groupId: String,
    val contactId: String,
    val perspective: GroupMembershipPerspective,
    val status: GroupMembershipStatus,
    val challenge: ByteArray,
    val ownerEncryptionPublicKey: ByteArray? = null,
    val ownerSigningPublicKey: ByteArray? = null,
    val createdAtEpochMilliseconds: Long,
    val updatedAtEpochMilliseconds: Long
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is GroupMembershipAttemptDto) return false

        return membershipId == other.membershipId &&
            sourceInvitationId == other.sourceInvitationId &&
            groupId == other.groupId &&
            contactId == other.contactId &&
            perspective == other.perspective &&
            status == other.status &&
            challenge.contentEquals(other.challenge) &&
            ownerEncryptionPublicKey.contentEqualsNullable(other.ownerEncryptionPublicKey) &&
            ownerSigningPublicKey.contentEqualsNullable(other.ownerSigningPublicKey) &&
            createdAtEpochMilliseconds == other.createdAtEpochMilliseconds &&
            updatedAtEpochMilliseconds == other.updatedAtEpochMilliseconds
    }

    override fun hashCode(): Int {
        var result = membershipId.hashCode()
        result = 31 * result + sourceInvitationId.hashCode()
        result = 31 * result + groupId.hashCode()
        result = 31 * result + contactId.hashCode()
        result = 31 * result + perspective.hashCode()
        result = 31 * result + status.hashCode()
        result = 31 * result + challenge.contentHashCode()
        result = 31 * result + (ownerEncryptionPublicKey?.contentHashCode() ?: 0)
        result = 31 * result + (ownerSigningPublicKey?.contentHashCode() ?: 0)
        result = 31 * result + createdAtEpochMilliseconds.hashCode()
        result = 31 * result + updatedAtEpochMilliseconds.hashCode()
        return result
    }
}

private fun ByteArray?.contentEqualsNullable(other: ByteArray?): Boolean =
    when {
        this == null -> other == null
        other == null -> false
        else -> contentEquals(other)
    }
