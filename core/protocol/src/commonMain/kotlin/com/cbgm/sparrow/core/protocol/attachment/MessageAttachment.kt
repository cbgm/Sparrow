package com.cbgm.sparrow.core.protocol.attachment

import com.cbgm.sparrow.core.protocol.serializer.ByteArrayAsBase64Serializer
import kotlinx.serialization.Serializable

@Serializable
enum class MessageAttachmentType {
    IMAGE,
    VIDEO,
    FILE,
    LOCATION
}

const val LOCATION_MIME_TYPE = "application/vnd.sparrow.location"

@Serializable
data class MessageAttachment(
    val attachmentId: String,
    val type: MessageAttachmentType,
    val mimeType: String,
    val byteSize: Long,
    val blob: EncryptedBlobReference,
    val fileName: String? = null,
    val width: Int? = null,
    val height: Int? = null,
    val durationMilliseconds: Long? = null
) {
    init {
        require(attachmentId.isNotBlank()) { "Attachment ID must not be blank" }
        require(mimeType.isNotBlank()) { "Attachment MIME type must not be blank" }
        require(byteSize > 0L) { "Attachment byte size must be positive" }
        require(fileName == null || fileName.isNotBlank()) { "Attachment file name must not be blank" }
        require(width == null || width > 0) { "Attachment width must be positive" }
        require(height == null || height > 0) { "Attachment height must be positive" }
        require(durationMilliseconds == null || durationMilliseconds >= 0L) {
            "Attachment duration must not be negative"
        }
        when (type) {
            MessageAttachmentType.IMAGE -> {
                require(mimeType.startsWith("image/")) { "Image attachment must use an image MIME type" }
                require(width != null && height != null) {
                    "Image attachments require width and height"
                }
            }

            MessageAttachmentType.VIDEO -> {
                require(mimeType.startsWith("video/")) { "Video attachment must use a video MIME type" }
                require((width == null) == (height == null)) {
                    "Video attachment dimensions must be both present or both absent"
                }
            }

            MessageAttachmentType.FILE -> Unit

            MessageAttachmentType.LOCATION -> {
                require(mimeType == LOCATION_MIME_TYPE) {
                    "Location attachment must use the Sparrow location MIME type"
                }
                require(fileName == null) { "Location attachment must not have a file name" }
                require(width == null && height == null && durationMilliseconds == null) {
                    "Location attachments must not contain media metadata"
                }
            }
        }
    }
}

@Serializable
data class EncryptedBlobReference(
    val nodeId: String,
    val blobId: String,
    val readCapability: String,
    val ciphertextByteSize: Long,
    val expiresAtEpochMilliseconds: Long,
    @Serializable(with = ByteArrayAsBase64Serializer::class)
    val encryptionKey: ByteArray,
    @Serializable(with = ByteArrayAsBase64Serializer::class)
    val nonce: ByteArray,
    @Serializable(with = ByteArrayAsBase64Serializer::class)
    val ciphertextSha256: ByteArray
) {
    init {
        require(nodeId.isNotBlank()) { "Blob node ID must not be blank" }
        require(blobId.isNotBlank()) { "Blob ID must not be blank" }
        require(readCapability.isNotBlank()) { "Blob read capability must not be blank" }
        require(ciphertextByteSize in 1..MAX_BLOB_CIPHERTEXT_BYTES) {
            "Blob ciphertext size must be between 1 and $MAX_BLOB_CIPHERTEXT_BYTES bytes"
        }
        require(expiresAtEpochMilliseconds > 0L) { "Blob expiry must be positive" }
        require(encryptionKey.size == BLOB_KEY_BYTES) { "Blob encryption key must be $BLOB_KEY_BYTES bytes" }
        require(nonce.size == BLOB_NONCE_BYTES) { "Blob nonce must be $BLOB_NONCE_BYTES bytes" }
        require(ciphertextSha256.size == SHA_256_BYTES) { "Blob SHA-256 digest must be $SHA_256_BYTES bytes" }
    }

    companion object {
        const val BLOB_KEY_BYTES = 32
        const val BLOB_NONCE_BYTES = 24
        const val SHA_256_BYTES = 32
        const val MAX_BLOB_CIPHERTEXT_BYTES = 128L * 1024L * 1024L
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as EncryptedBlobReference

        if (ciphertextByteSize != other.ciphertextByteSize) return false
        if (expiresAtEpochMilliseconds != other.expiresAtEpochMilliseconds) return false
        if (nodeId != other.nodeId) return false
        if (blobId != other.blobId) return false
        if (readCapability != other.readCapability) return false
        if (!encryptionKey.contentEquals(other.encryptionKey)) return false
        if (!nonce.contentEquals(other.nonce)) return false
        if (!ciphertextSha256.contentEquals(other.ciphertextSha256)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = ciphertextByteSize.hashCode()
        result = 31 * result + expiresAtEpochMilliseconds.hashCode()
        result = 31 * result + nodeId.hashCode()
        result = 31 * result + blobId.hashCode()
        result = 31 * result + readCapability.hashCode()
        result = 31 * result + encryptionKey.contentHashCode()
        result = 31 * result + nonce.contentHashCode()
        result = 31 * result + ciphertextSha256.contentHashCode()
        return result
    }
}
