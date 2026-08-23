package com.cbgm.sparrow.data.database.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "message_attachments",
    foreignKeys = [
        ForeignKey(
            entity = MessageEntity::class,
            parentColumns = ["id"],
            childColumns = ["messageId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["messageId"]),
        Index(value = ["messageId", "position"], unique = true),
        Index(value = ["blobId"], unique = true)
    ]
)
data class MessageAttachmentEntity(
    @PrimaryKey
    val id: String,
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
    /** Only present for blobs uploaded by this device. Never sent to peers. */
    val deleteCapability: String?,
    /** File name inside Sparrow's private message-attachment cache. */
    val localFileName: String?
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as MessageAttachmentEntity

        if (position != other.position) return false
        if (byteSize != other.byteSize) return false
        if (width != other.width) return false
        if (height != other.height) return false
        if (durationMilliseconds != other.durationMilliseconds) return false
        if (ciphertextByteSize != other.ciphertextByteSize) return false
        if (blobExpiresAtEpochMilliseconds != other.blobExpiresAtEpochMilliseconds) return false
        if (id != other.id) return false
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
        if (localFileName != other.localFileName) return false

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
        result = 31 * result + id.hashCode()
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
        result = 31 * result + (localFileName?.hashCode() ?: 0)
        return result
    }
}
