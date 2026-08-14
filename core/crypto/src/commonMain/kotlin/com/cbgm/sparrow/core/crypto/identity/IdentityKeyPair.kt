package com.cbgm.sparrow.core.crypto.identity

@OptIn(ExperimentalUnsignedTypes::class)
data class IdentityKeyPair(
    val encryptionPublicKey: UByteArray,
    val encryptionPrivateKey: UByteArray,
    val signingPublicKey: UByteArray,
    val signingPrivateKey: UByteArray
) {
    init {
        require(encryptionPublicKey.isNotEmpty()) {
            "Encryption public key must not be empty"
        }

        require(encryptionPrivateKey.isNotEmpty()) {
            "Encryption private key must not be empty"
        }

        require(signingPublicKey.isNotEmpty()) {
            "Signing public key must not be empty"
        }

        require(signingPrivateKey.isNotEmpty()) {
            "Signing private key must not be empty"
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) {
            return true
        }

        if (other !is IdentityKeyPair) {
            return false
        }

        return encryptionPublicKey.contentEquals(other.encryptionPublicKey) &&
            encryptionPrivateKey.contentEquals(other.encryptionPrivateKey) &&
            signingPublicKey.contentEquals(other.signingPublicKey) &&
            signingPrivateKey.contentEquals(other.signingPrivateKey)
    }

    override fun hashCode(): Int {
        var result = encryptionPublicKey.contentHashCode()

        result = 31 * result + encryptionPrivateKey.contentHashCode()

        result = 31 * result + signingPublicKey.contentHashCode()

        result = 31 * result + signingPrivateKey.contentHashCode()

        return result
    }
}
