package com.cbgm.sparrow.core.crypto.model

data class PublicIdentityKeySet(
    val signingPublicKey: ByteArray,
    val encryptionPublicKey: ByteArray
) {
    init {
        require(signingPublicKey.isNotEmpty()) {
            "Signing public key must not be empty"
        }

        require(encryptionPublicKey.isNotEmpty()) {
            "Encryption public key must not be empty"
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true

        if (other !is PublicIdentityKeySet) return false

        return signingPublicKey.contentEquals(other.signingPublicKey) &&
            encryptionPublicKey.contentEquals(other.encryptionPublicKey)
    }

    override fun hashCode(): Int {
        var result = signingPublicKey.contentHashCode()

        result = 31 * result + encryptionPublicKey.contentHashCode()

        return result
    }
}
