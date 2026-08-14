package com.cbgm.sparrow.core.protocol.identity

data class LocalPublicIdentity(
    val encryptionPublicKey: ByteArray,
    val signingPublicKey: ByteArray
) {
    init {
        require(encryptionPublicKey.isNotEmpty()) {
            "Encryption public key must not be empty"
        }

        require(signingPublicKey.isNotEmpty()) {
            "Signing public key must not be empty"
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true

        if (other !is LocalPublicIdentity) return false

        return encryptionPublicKey.contentEquals(other.encryptionPublicKey) &&
            signingPublicKey.contentEquals(
                other.signingPublicKey
            )
    }

    override fun hashCode(): Int {
        var result = encryptionPublicKey.contentHashCode()

        result = 31 * result + signingPublicKey.contentHashCode()

        return result
    }
}
