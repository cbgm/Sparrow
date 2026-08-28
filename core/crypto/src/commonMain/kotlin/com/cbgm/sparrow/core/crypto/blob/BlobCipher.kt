package com.cbgm.sparrow.core.crypto.blob

data class EncryptedBlob(
    val key: ByteArray,
    val nonce: ByteArray,
    val ciphertext: ByteArray
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as EncryptedBlob

        if (!key.contentEquals(other.key)) return false
        if (!nonce.contentEquals(other.nonce)) return false
        if (!ciphertext.contentEquals(other.ciphertext)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = key.contentHashCode()
        result = 31 * result + nonce.contentHashCode()
        result = 31 * result + ciphertext.contentHashCode()
        return result
    }
}

interface BlobCipher {
    suspend fun encrypt(
        plaintext: ByteArray,
        associatedData: ByteArray
    ): Result<EncryptedBlob>

    suspend fun decrypt(
        ciphertext: ByteArray,
        key: ByteArray,
        nonce: ByteArray,
        associatedData: ByteArray
    ): Result<ByteArray>
}
