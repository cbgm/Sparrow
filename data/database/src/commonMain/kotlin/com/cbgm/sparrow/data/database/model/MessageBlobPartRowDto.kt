package com.cbgm.sparrow.data.database.model

data class MessageBlobPartRowDto(
    val partId: String,
    val messageId: String,
    val position: Int,
    val type: String,
    val mimeType: String,
    val byteSize: Long,
    val fileName: String?,
    val width: Int?,
    val height: Int?,
    val durationMilliseconds: Long?,
    val nodeId: String,
    val blobId: String,
    val readCapability: String,
    val ciphertextByteSize: Long,
    val blobExpiresAtEpochMilliseconds: Long,
    val encryptionKey: ByteArray,
    val nonce: ByteArray,
    val ciphertextSha256: ByteArray,
    val deleteCapability: String?,
    val localFilePath: String?
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as MessageBlobPartRowDto

        if (position != other.position) return false
        if (byteSize != other.byteSize) return false
        if (width != other.width) return false
        if (height != other.height) return false
        if (durationMilliseconds != other.durationMilliseconds) return false
        if (ciphertextByteSize != other.ciphertextByteSize) return false
        if (blobExpiresAtEpochMilliseconds != other.blobExpiresAtEpochMilliseconds) return false
        if (partId != other.partId) return false
        if (messageId != other.messageId) return false
        if (type != other.type) return false
        if (mimeType != other.mimeType) return false
        if (fileName != other.fileName) return false
        if (nodeId != other.nodeId) return false
        if (blobId != other.blobId) return false
        if (readCapability != other.readCapability) return false
        if (!encryptionKey.contentEquals(other.encryptionKey)) return false
        if (!nonce.contentEquals(other.nonce)) return false
        if (!ciphertextSha256.contentEquals(other.ciphertextSha256)) return false
        if (deleteCapability != other.deleteCapability) return false
        if (localFilePath != other.localFilePath) return false

        return true
    }

    override fun hashCode(): Int {
        var result = position
        result = 31 * result + byteSize.hashCode()
        result = 31 * result + (width ?: 0)
        result = 31 * result + (height ?: 0)
        result = 31 * result + (durationMilliseconds?.hashCode() ?: 0)
        result = 31 * result + ciphertextByteSize.hashCode()
        result = 31 * result + blobExpiresAtEpochMilliseconds.hashCode()
        result = 31 * result + partId.hashCode()
        result = 31 * result + messageId.hashCode()
        result = 31 * result + type.hashCode()
        result = 31 * result + mimeType.hashCode()
        result = 31 * result + (fileName?.hashCode() ?: 0)
        result = 31 * result + nodeId.hashCode()
        result = 31 * result + blobId.hashCode()
        result = 31 * result + readCapability.hashCode()
        result = 31 * result + encryptionKey.contentHashCode()
        result = 31 * result + nonce.contentHashCode()
        result = 31 * result + ciphertextSha256.contentHashCode()
        result = 31 * result + (deleteCapability?.hashCode() ?: 0)
        result = 31 * result + (localFilePath?.hashCode() ?: 0)
        return result
    }
}
