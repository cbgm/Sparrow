package com.cbgm.sparrow.feature.identity.domain.model

/** Sensitive, short-lived decoded backup. Never log, persist unencrypted, or display private keys. */
data class IdentityBackup(
    val publicIdentity: PublicIdentity,
    val encryptionPrivateKey: ByteArray,
    val signingPrivateKey: ByteArray
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as IdentityBackup

        if (publicIdentity != other.publicIdentity) return false
        if (!encryptionPrivateKey.contentEquals(other.encryptionPrivateKey)) return false
        if (!signingPrivateKey.contentEquals(other.signingPrivateKey)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = publicIdentity.hashCode()
        result = 31 * result + encryptionPrivateKey.contentHashCode()
        result = 31 * result + signingPrivateKey.contentHashCode()
        return result
    }
}
