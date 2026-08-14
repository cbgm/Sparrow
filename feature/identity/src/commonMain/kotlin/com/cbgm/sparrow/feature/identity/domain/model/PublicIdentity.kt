package com.cbgm.sparrow.feature.identity.domain.model

data class PublicIdentity(
    /**
     * Public key used for creating shared secrets.
     * X25519
     */
    val encryptionPublicKey: ByteArray,
    /**
     * Public key used for signatures.
     * Ed25519
     */
    val signingPublicKey: ByteArray
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false

        other as PublicIdentity

        if (!encryptionPublicKey.contentEquals(other.encryptionPublicKey)) return false
        if (!signingPublicKey.contentEquals(other.signingPublicKey)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = encryptionPublicKey.contentHashCode()
        result = 31 * result + signingPublicKey.contentHashCode()
        return result
    }
}
