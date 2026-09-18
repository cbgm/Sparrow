package com.cbgm.sparrow.feature.membership.data.model

data class GroupMembershipPeerDto(
    val id: String,
    val displayName: String?,
    val preferredPhoneNumber: String?,
    val phoneNumbers: List<String>,
    val encryptionPublicKey: ByteArray?,
    val signingPublicKey: ByteArray?,
    val hasMutualIdentity: Boolean
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is GroupMembershipPeerDto) return false
        return id == other.id &&
            displayName == other.displayName &&
            preferredPhoneNumber == other.preferredPhoneNumber &&
            phoneNumbers == other.phoneNumbers &&
            encryptionPublicKey.contentEqualsNullable(other.encryptionPublicKey) &&
            signingPublicKey.contentEqualsNullable(other.signingPublicKey) &&
            hasMutualIdentity == other.hasMutualIdentity
    }

    override fun hashCode(): Int {
        var result = id.hashCode()
        result = 31 * result + (displayName?.hashCode() ?: 0)
        result = 31 * result + (preferredPhoneNumber?.hashCode() ?: 0)
        result = 31 * result + phoneNumbers.hashCode()
        result = 31 * result + (encryptionPublicKey?.contentHashCode() ?: 0)
        result = 31 * result + (signingPublicKey?.contentHashCode() ?: 0)
        result = 31 * result + hasMutualIdentity.hashCode()
        return result
    }
}

private fun ByteArray?.contentEqualsNullable(other: ByteArray?): Boolean =
    when {
        this == null -> other == null
        other == null -> false
        else -> contentEquals(other)
    }
