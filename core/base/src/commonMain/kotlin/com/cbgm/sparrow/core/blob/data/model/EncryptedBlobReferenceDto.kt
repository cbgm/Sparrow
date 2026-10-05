package com.cbgm.sparrow.core.blob.data.model

data class EncryptedBlobReferenceDto(
    val nodeId: String,
    val blobId: String,
    val readCapability: String,
    val ciphertextByteSize: Long,
    val expiresAtEpochMilliseconds: Long,
    val encryptionKey: ByteArray,
    val nonce: ByteArray,
    val ciphertextSha256: ByteArray
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is EncryptedBlobReferenceDto) return false

        return nodeId == other.nodeId &&
            blobId == other.blobId &&
            readCapability == other.readCapability &&
            ciphertextByteSize == other.ciphertextByteSize &&
            expiresAtEpochMilliseconds == other.expiresAtEpochMilliseconds &&
            encryptionKey.contentEquals(other.encryptionKey) &&
            nonce.contentEquals(other.nonce) &&
            ciphertextSha256.contentEquals(other.ciphertextSha256)
    }

    override fun hashCode(): Int {
        var result = nodeId.hashCode()
        result = 31 * result + blobId.hashCode()
        result = 31 * result + readCapability.hashCode()
        result = 31 * result + ciphertextByteSize.hashCode()
        result = 31 * result + expiresAtEpochMilliseconds.hashCode()
        result = 31 * result + encryptionKey.contentHashCode()
        result = 31 * result + nonce.contentHashCode()
        result = 31 * result + ciphertextSha256.contentHashCode()
        return result
    }
}
