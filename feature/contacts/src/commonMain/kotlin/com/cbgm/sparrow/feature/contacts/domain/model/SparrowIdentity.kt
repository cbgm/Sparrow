package com.cbgm.sparrow.feature.contacts.domain.model

/**
 * Another person's public Sparrow identity.
 *
 * A contact may exist without this identity.
 *
 * When present, both public keys are required.
 */
data class SparrowIdentity(
    val encryptionPublicKey: ByteArray,
    val signingPublicKey: ByteArray,
    val verificationStatus: ContactVerificationStatus,
    val verifiedByContact: Boolean = false,
    val locallyImported: Boolean = false,
    val keyExchangeStatus: KeyExchangeStatus,
    val updatedAtEpochMilliseconds: Long
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) {
            return true
        }

        if (other !is SparrowIdentity) {
            return false
        }

        return encryptionPublicKey.contentEquals(
            other.encryptionPublicKey
        ) &&
            signingPublicKey.contentEquals(
                other.signingPublicKey
            ) &&
            verificationStatus ==
            other.verificationStatus &&
            verifiedByContact == other.verifiedByContact &&
            locallyImported == other.locallyImported &&
            keyExchangeStatus ==
            other.keyExchangeStatus &&
            updatedAtEpochMilliseconds ==
            other.updatedAtEpochMilliseconds
    }

    override fun hashCode(): Int {
        var result = encryptionPublicKey.contentHashCode()

        result = 31 * result + signingPublicKey.contentHashCode()

        result = 31 * result + verificationStatus.hashCode()

        result = 31 * result + verifiedByContact.hashCode()

        result = 31 * result + locallyImported.hashCode()

        result = 31 * result + keyExchangeStatus.hashCode()

        result = 31 * result + updatedAtEpochMilliseconds.hashCode()

        return result
    }
}
