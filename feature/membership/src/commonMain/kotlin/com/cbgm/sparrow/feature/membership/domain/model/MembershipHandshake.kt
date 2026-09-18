package com.cbgm.sparrow.feature.membership.domain.model

data class MembershipHandshake(
    val sourceId: String,
    val groupId: String,
    val peerId: String,
    val createdAtEpochMilliseconds: Long,
    val updatedAtEpochMilliseconds: Long,
    val ownerEncryptionPublicKey: ByteArray? = null,
    val ownerSigningPublicKey: ByteArray? = null
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is MembershipHandshake) return false
        return sourceId == other.sourceId &&
            groupId == other.groupId &&
            peerId == other.peerId &&
            createdAtEpochMilliseconds == other.createdAtEpochMilliseconds &&
            updatedAtEpochMilliseconds == other.updatedAtEpochMilliseconds &&
            ownerEncryptionPublicKey.contentEqualsNullable(other.ownerEncryptionPublicKey) &&
            ownerSigningPublicKey.contentEqualsNullable(other.ownerSigningPublicKey)
    }

    override fun hashCode(): Int {
        var result = sourceId.hashCode()
        result = 31 * result + groupId.hashCode()
        result = 31 * result + peerId.hashCode()
        result = 31 * result + createdAtEpochMilliseconds.hashCode()
        result = 31 * result + updatedAtEpochMilliseconds.hashCode()
        result = 31 * result + (ownerEncryptionPublicKey?.contentHashCode() ?: 0)
        result = 31 * result + (ownerSigningPublicKey?.contentHashCode() ?: 0)
        return result
    }
}

data class StartedMembershipHandshake(
    val sourceId: String,
    val groupId: String,
    val peerId: String,
    val createdAtEpochMilliseconds: Long,
    val expiresAtEpochMilliseconds: Long
)

data class IncomingMembershipOffer(
    val sourceId: String,
    val groupId: String,
    val peerId: String,
    val title: String,
    val createdAtEpochMilliseconds: Long,
    val expiresAtEpochMilliseconds: Long,
    val ownerEncryptionPublicKey: ByteArray,
    val ownerSigningPublicKey: ByteArray
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is IncomingMembershipOffer) return false
        return sourceId == other.sourceId &&
            groupId == other.groupId &&
            peerId == other.peerId &&
            title == other.title &&
            createdAtEpochMilliseconds == other.createdAtEpochMilliseconds &&
            expiresAtEpochMilliseconds == other.expiresAtEpochMilliseconds &&
            ownerEncryptionPublicKey.contentEquals(other.ownerEncryptionPublicKey) &&
            ownerSigningPublicKey.contentEquals(other.ownerSigningPublicKey)
    }

    override fun hashCode(): Int {
        var result = sourceId.hashCode()
        result = 31 * result + groupId.hashCode()
        result = 31 * result + peerId.hashCode()
        result = 31 * result + title.hashCode()
        result = 31 * result + createdAtEpochMilliseconds.hashCode()
        result = 31 * result + expiresAtEpochMilliseconds.hashCode()
        result = 31 * result + ownerEncryptionPublicKey.contentHashCode()
        result = 31 * result + ownerSigningPublicKey.contentHashCode()
        return result
    }
}

data class MembershipSigningProof(
    val sourceId: String,
    val groupId: String,
    val peerId: String,
    val signingPublicKey: ByteArray
) {
    override fun equals(other: Any?): Boolean =
        other is MembershipSigningProof &&
            sourceId == other.sourceId &&
            groupId == other.groupId &&
            peerId == other.peerId &&
            signingPublicKey.contentEquals(other.signingPublicKey)

    override fun hashCode(): Int {
        var result = sourceId.hashCode()
        result = 31 * result + groupId.hashCode()
        result = 31 * result + peerId.hashCode()
        result = 31 * result + signingPublicKey.contentHashCode()
        return result
    }
}

data class MembershipJoinRequest(
    val sourceId: String,
    val groupId: String,
    val peerId: String,
    val memberEncryptionPublicKey: ByteArray,
    val memberSigningPublicKey: ByteArray,
    val alreadyAccepted: Boolean
) {
    override fun equals(other: Any?): Boolean =
        other is MembershipJoinRequest &&
            sourceId == other.sourceId &&
            groupId == other.groupId &&
            peerId == other.peerId &&
            alreadyAccepted == other.alreadyAccepted &&
            memberEncryptionPublicKey.contentEquals(other.memberEncryptionPublicKey) &&
            memberSigningPublicKey.contentEquals(other.memberSigningPublicKey)

    override fun hashCode(): Int {
        var result = sourceId.hashCode()
        result = 31 * result + groupId.hashCode()
        result = 31 * result + peerId.hashCode()
        result = 31 * result + memberEncryptionPublicKey.contentHashCode()
        result = 31 * result + memberSigningPublicKey.contentHashCode()
        result = 31 * result + alreadyAccepted.hashCode()
        return result
    }
}

enum class MembershipDeclineDisposition {
    PENDING_HANDSHAKE,
    ACTIVE_MEMBER
}

data class MembershipDeclineResult(
    val sourceId: String,
    val groupId: String,
    val peerId: String,
    val signingPublicKey: ByteArray,
    val disposition: MembershipDeclineDisposition
) {
    override fun equals(other: Any?): Boolean =
        other is MembershipDeclineResult &&
            sourceId == other.sourceId &&
            groupId == other.groupId &&
            peerId == other.peerId &&
            disposition == other.disposition &&
            signingPublicKey.contentEquals(other.signingPublicKey)

    override fun hashCode(): Int {
        var result = sourceId.hashCode()
        result = 31 * result + groupId.hashCode()
        result = 31 * result + peerId.hashCode()
        result = 31 * result + signingPublicKey.contentHashCode()
        result = 31 * result + disposition.hashCode()
        return result
    }
}

private fun ByteArray?.contentEqualsNullable(other: ByteArray?): Boolean =
    when {
        this == null -> other == null
        other == null -> false
        else -> contentEquals(other)
    }
