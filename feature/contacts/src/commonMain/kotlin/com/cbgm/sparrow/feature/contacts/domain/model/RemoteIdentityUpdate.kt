package com.cbgm.sparrow.feature.contacts.domain.model

data class RemoteIdentityUpdate(
    val contactId: String,
    val encryptionPublicKey: ByteArray,
    val signingPublicKey: ByteArray,
    val keyExchangeStatus: KeyExchangeStatus,
    val verificationStatus: ContactVerificationStatus,
    val identityChanged: Boolean
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) {
            return true
        }

        if (other !is RemoteIdentityUpdate) {
            return false
        }

        return contactId == other.contactId &&
            encryptionPublicKey.contentEquals(
                other.encryptionPublicKey
            ) &&
            signingPublicKey.contentEquals(
                other.signingPublicKey
            ) &&
            keyExchangeStatus ==
            other.keyExchangeStatus &&
            verificationStatus ==
            other.verificationStatus &&
            identityChanged ==
            other.identityChanged
    }

    override fun hashCode(): Int {
        var result =
            contactId.hashCode()

        result = 31 * result + encryptionPublicKey.contentHashCode()

        result = 31 * result + signingPublicKey.contentHashCode()

        result = 31 * result + keyExchangeStatus.hashCode()

        result = 31 * result + verificationStatus.hashCode()

        result = 31 * result + identityChanged.hashCode()

        return result
    }
}
