package com.cbgm.sparrow.core.protocol.identity

data class LocalSigningKeyPair(
    val publicKey: ByteArray,
    val privateKey: ByteArray
) {
    init {
        require(publicKey.isNotEmpty()) {
            "Signing public key must not be empty"
        }

        require(privateKey.isNotEmpty()) {
            "Signing private key must not be empty"
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true

        if (other !is LocalSigningKeyPair) return false

        return publicKey.contentEquals(other.publicKey) && privateKey.contentEquals(other.privateKey)
    }

    override fun hashCode(): Int {
        var result = publicKey.contentHashCode()

        result = 31 * result + privateKey.contentHashCode()

        return result
    }
}
