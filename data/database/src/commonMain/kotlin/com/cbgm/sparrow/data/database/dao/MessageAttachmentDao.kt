package com.cbgm.sparrow.data.database.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import com.cbgm.sparrow.data.database.entity.AttachmentMessageContextEntity
import com.cbgm.sparrow.data.database.entity.MessageBlobEntity
import com.cbgm.sparrow.data.database.entity.MessagePartEntity
import com.cbgm.sparrow.data.database.model.LocalMessageAttachmentRowDto
import com.cbgm.sparrow.data.database.model.MessageBlobPartRowDto
import kotlinx.coroutines.flow.Flow

@Dao
interface MessageAttachmentDao {
    @Upsert
    suspend fun upsertMessageContext(context: AttachmentMessageContextEntity)

    @Query("SELECT * FROM attachment_message_contexts WHERE messageId = :messageId LIMIT 1")
    suspend fun findMessageContext(messageId: String): AttachmentMessageContextEntity?

    @Query(
        """
        UPDATE attachment_message_contexts
        SET displayName = :displayName, isGroup = :isGroup
        WHERE conversationId = :conversationId
          AND (displayName != :displayName OR isGroup != :isGroup)
        """
    )
    suspend fun updateConversationDisplayName(
        conversationId: String,
        displayName: String,
        isGroup: Boolean
    ): Int

    @Upsert
    suspend fun upsertParts(parts: List<MessagePartEntity>)

    @Upsert
    suspend fun upsertBlobs(blobs: List<MessageBlobEntity>)

    @Transaction
    suspend fun upsertBlobParts(
        parts: List<MessagePartEntity>,
        blobs: List<MessageBlobEntity>
    ) {
        require(parts.size == blobs.size) { "Message part/blob count mismatch" }
        upsertParts(parts)
        upsertBlobs(blobs)
    }

    @Query(
        """
        SELECT
            message_parts.id AS partId,
            message_parts.messageId AS messageId,
            message_parts.position AS position,
            message_parts.type AS type,
            message_blobs.mimeType AS mimeType,
            message_blobs.byteSize AS byteSize,
            message_blobs.fileName AS fileName,
            message_blobs.width AS width,
            message_blobs.height AS height,
            message_blobs.durationMilliseconds AS durationMilliseconds,
            message_blobs.nodeId AS nodeId,
            message_blobs.blobId AS blobId,
            message_blobs.readCapability AS readCapability,
            message_blobs.ciphertextByteSize AS ciphertextByteSize,
            message_blobs.blobExpiresAtEpochMilliseconds AS blobExpiresAtEpochMilliseconds,
            message_blobs.encryptionKey AS encryptionKey,
            message_blobs.nonce AS nonce,
            message_blobs.ciphertextSha256 AS ciphertextSha256,
            message_blobs.deleteCapability AS deleteCapability,
            message_blobs.localFilePath AS localFilePath
        FROM message_parts
        INNER JOIN message_blobs ON message_blobs.partId = message_parts.id
        WHERE message_parts.messageId IN (:messageIds)
        ORDER BY message_parts.messageId ASC, message_parts.position ASC
        """
    )
    fun observeByMessageIds(messageIds: List<String>): Flow<List<MessageBlobPartRowDto>>

    @Query(
        """
        SELECT
            message_parts.id AS partId,
            message_parts.messageId AS messageId,
            message_parts.position AS position,
            message_parts.type AS type,
            message_blobs.mimeType AS mimeType,
            message_blobs.byteSize AS byteSize,
            message_blobs.fileName AS fileName,
            message_blobs.width AS width,
            message_blobs.height AS height,
            message_blobs.durationMilliseconds AS durationMilliseconds,
            message_blobs.nodeId AS nodeId,
            message_blobs.blobId AS blobId,
            message_blobs.readCapability AS readCapability,
            message_blobs.ciphertextByteSize AS ciphertextByteSize,
            message_blobs.blobExpiresAtEpochMilliseconds AS blobExpiresAtEpochMilliseconds,
            message_blobs.encryptionKey AS encryptionKey,
            message_blobs.nonce AS nonce,
            message_blobs.ciphertextSha256 AS ciphertextSha256,
            message_blobs.deleteCapability AS deleteCapability,
            message_blobs.localFilePath AS localFilePath
        FROM message_parts
        INNER JOIN message_blobs ON message_blobs.partId = message_parts.id
        WHERE message_parts.messageId = :messageId
        ORDER BY message_parts.position ASC
        """
    )
    suspend fun findByMessageId(messageId: String): List<MessageBlobPartRowDto>

    @Query(
        """
        SELECT
            message_parts.id AS partId,
            message_parts.messageId AS messageId,
            message_parts.position AS position,
            message_parts.type AS type,
            message_blobs.mimeType AS mimeType,
            message_blobs.byteSize AS byteSize,
            message_blobs.fileName AS fileName,
            message_blobs.width AS width,
            message_blobs.height AS height,
            message_blobs.durationMilliseconds AS durationMilliseconds,
            message_blobs.nodeId AS nodeId,
            message_blobs.blobId AS blobId,
            message_blobs.readCapability AS readCapability,
            message_blobs.ciphertextByteSize AS ciphertextByteSize,
            message_blobs.blobExpiresAtEpochMilliseconds AS blobExpiresAtEpochMilliseconds,
            message_blobs.encryptionKey AS encryptionKey,
            message_blobs.nonce AS nonce,
            message_blobs.ciphertextSha256 AS ciphertextSha256,
            message_blobs.deleteCapability AS deleteCapability,
            message_blobs.localFilePath AS localFilePath
        FROM message_parts
        INNER JOIN message_blobs ON message_blobs.partId = message_parts.id
        WHERE message_parts.messageId IN (:messageIds)
        ORDER BY message_parts.messageId ASC, message_parts.position ASC
        """
    )
    suspend fun findByMessageIds(messageIds: List<String>): List<MessageBlobPartRowDto>

    @Query(
        """
        SELECT
            message_parts.id AS partId,
            message_parts.messageId AS messageId,
            message_parts.position AS position,
            message_parts.type AS type,
            message_blobs.mimeType AS mimeType,
            message_blobs.byteSize AS byteSize,
            message_blobs.fileName AS fileName,
            message_blobs.width AS width,
            message_blobs.height AS height,
            message_blobs.durationMilliseconds AS durationMilliseconds,
            message_blobs.nodeId AS nodeId,
            message_blobs.blobId AS blobId,
            message_blobs.readCapability AS readCapability,
            message_blobs.ciphertextByteSize AS ciphertextByteSize,
            message_blobs.blobExpiresAtEpochMilliseconds AS blobExpiresAtEpochMilliseconds,
            message_blobs.encryptionKey AS encryptionKey,
            message_blobs.nonce AS nonce,
            message_blobs.ciphertextSha256 AS ciphertextSha256,
            message_blobs.deleteCapability AS deleteCapability,
            message_blobs.localFilePath AS localFilePath
        FROM message_parts
        INNER JOIN message_blobs ON message_blobs.partId = message_parts.id
        INNER JOIN attachment_message_contexts AS ctx ON ctx.messageId = message_parts.messageId
        WHERE ctx.conversationId = :conversationId
        ORDER BY ctx.createdAtEpochMilliseconds ASC, message_parts.messageId ASC, message_parts.position ASC
        """
    )
    suspend fun findByConversationId(conversationId: String): List<MessageBlobPartRowDto>

    @Query(
        """
        SELECT
            message_parts.id AS partId,
            message_parts.messageId AS messageId,
            message_parts.position AS position,
            message_parts.type AS type,
            message_blobs.mimeType AS mimeType,
            message_blobs.byteSize AS byteSize,
            message_blobs.fileName AS fileName,
            message_blobs.width AS width,
            message_blobs.height AS height,
            message_blobs.durationMilliseconds AS durationMilliseconds,
            message_blobs.nodeId AS nodeId,
            message_blobs.blobId AS blobId,
            message_blobs.readCapability AS readCapability,
            message_blobs.ciphertextByteSize AS ciphertextByteSize,
            message_blobs.blobExpiresAtEpochMilliseconds AS blobExpiresAtEpochMilliseconds,
            message_blobs.encryptionKey AS encryptionKey,
            message_blobs.nonce AS nonce,
            message_blobs.ciphertextSha256 AS ciphertextSha256,
            message_blobs.deleteCapability AS deleteCapability,
            message_blobs.localFilePath AS localFilePath,
            ctx.conversationId AS conversationId,
            ctx.createdAtEpochMilliseconds AS createdAtEpochMilliseconds,
            ctx.displayName AS displayName,
            ctx.isGroup AS isGroup
        FROM message_parts
        INNER JOIN message_blobs ON message_blobs.partId = message_parts.id
        INNER JOIN attachment_message_contexts AS ctx ON ctx.messageId = message_parts.messageId
        WHERE message_blobs.localFilePath IS NOT NULL
        ORDER BY ctx.createdAtEpochMilliseconds DESC, message_parts.position ASC
        """
    )
    fun observeAllLocal(): Flow<List<LocalMessageAttachmentRowDto>>

    @Query(
        """
        SELECT
            message_parts.id AS partId,
            message_parts.messageId AS messageId,
            message_parts.position AS position,
            message_parts.type AS type,
            message_blobs.mimeType AS mimeType,
            message_blobs.byteSize AS byteSize,
            message_blobs.fileName AS fileName,
            message_blobs.width AS width,
            message_blobs.height AS height,
            message_blobs.durationMilliseconds AS durationMilliseconds,
            message_blobs.nodeId AS nodeId,
            message_blobs.blobId AS blobId,
            message_blobs.readCapability AS readCapability,
            message_blobs.ciphertextByteSize AS ciphertextByteSize,
            message_blobs.blobExpiresAtEpochMilliseconds AS blobExpiresAtEpochMilliseconds,
            message_blobs.encryptionKey AS encryptionKey,
            message_blobs.nonce AS nonce,
            message_blobs.ciphertextSha256 AS ciphertextSha256,
            message_blobs.deleteCapability AS deleteCapability,
            message_blobs.localFilePath AS localFilePath,
            ctx.conversationId AS conversationId,
            ctx.createdAtEpochMilliseconds AS createdAtEpochMilliseconds,
            ctx.displayName AS displayName,
            ctx.isGroup AS isGroup
        FROM message_parts
        INNER JOIN message_blobs ON message_blobs.partId = message_parts.id
        INNER JOIN attachment_message_contexts AS ctx ON ctx.messageId = message_parts.messageId
        WHERE ctx.conversationId = :conversationId
          AND message_blobs.localFilePath IS NOT NULL
        ORDER BY ctx.createdAtEpochMilliseconds DESC, message_parts.position ASC
        """
    )
    fun observeLocalByConversationId(conversationId: String): Flow<List<LocalMessageAttachmentRowDto>>

    @Query(
        """
        SELECT
            message_parts.id AS partId,
            message_parts.messageId AS messageId,
            message_parts.position AS position,
            message_parts.type AS type,
            message_blobs.mimeType AS mimeType,
            message_blobs.byteSize AS byteSize,
            message_blobs.fileName AS fileName,
            message_blobs.width AS width,
            message_blobs.height AS height,
            message_blobs.durationMilliseconds AS durationMilliseconds,
            message_blobs.nodeId AS nodeId,
            message_blobs.blobId AS blobId,
            message_blobs.readCapability AS readCapability,
            message_blobs.ciphertextByteSize AS ciphertextByteSize,
            message_blobs.blobExpiresAtEpochMilliseconds AS blobExpiresAtEpochMilliseconds,
            message_blobs.encryptionKey AS encryptionKey,
            message_blobs.nonce AS nonce,
            message_blobs.ciphertextSha256 AS ciphertextSha256,
            message_blobs.deleteCapability AS deleteCapability,
            message_blobs.localFilePath AS localFilePath,
            ctx.conversationId AS conversationId,
            ctx.createdAtEpochMilliseconds AS createdAtEpochMilliseconds,
            ctx.displayName AS displayName,
            ctx.isGroup AS isGroup
        FROM message_parts
        INNER JOIN message_blobs ON message_blobs.partId = message_parts.id
        INNER JOIN attachment_message_contexts AS ctx ON ctx.messageId = message_parts.messageId
        WHERE message_parts.id IN (:partIds)
          AND message_blobs.localFilePath IS NOT NULL
        """
    )
    suspend fun findLocalRowsByIds(partIds: List<String>): List<LocalMessageAttachmentRowDto>

    @Query("UPDATE message_blobs SET localFilePath = NULL WHERE partId IN (:partIds)")
    suspend fun clearLocalFilePaths(partIds: List<String>): Int

    @Query(
        """
        UPDATE message_blobs
        SET localFilePath = NULL
        WHERE partId IN (
            SELECT message_parts.id
            FROM message_parts
            INNER JOIN attachment_message_contexts AS ctx ON ctx.messageId = message_parts.messageId
            WHERE ctx.conversationId = :conversationId
        )
        """
    )
    suspend fun clearLocalFilePathsForConversation(conversationId: String): Int

    @Query(
        """
        SELECT
            message_parts.id AS partId,
            message_parts.messageId AS messageId,
            message_parts.position AS position,
            message_parts.type AS type,
            message_blobs.mimeType AS mimeType,
            message_blobs.byteSize AS byteSize,
            message_blobs.fileName AS fileName,
            message_blobs.width AS width,
            message_blobs.height AS height,
            message_blobs.durationMilliseconds AS durationMilliseconds,
            message_blobs.nodeId AS nodeId,
            message_blobs.blobId AS blobId,
            message_blobs.readCapability AS readCapability,
            message_blobs.ciphertextByteSize AS ciphertextByteSize,
            message_blobs.blobExpiresAtEpochMilliseconds AS blobExpiresAtEpochMilliseconds,
            message_blobs.encryptionKey AS encryptionKey,
            message_blobs.nonce AS nonce,
            message_blobs.ciphertextSha256 AS ciphertextSha256,
            message_blobs.deleteCapability AS deleteCapability,
            message_blobs.localFilePath AS localFilePath
        FROM message_parts
        INNER JOIN message_blobs ON message_blobs.partId = message_parts.id
        WHERE message_parts.id = :partId
        LIMIT 1
        """
    )
    fun observeById(partId: String): Flow<MessageBlobPartRowDto?>

    @Query(
        """
        SELECT
            message_parts.id AS partId,
            message_parts.messageId AS messageId,
            message_parts.position AS position,
            message_parts.type AS type,
            message_blobs.mimeType AS mimeType,
            message_blobs.byteSize AS byteSize,
            message_blobs.fileName AS fileName,
            message_blobs.width AS width,
            message_blobs.height AS height,
            message_blobs.durationMilliseconds AS durationMilliseconds,
            message_blobs.nodeId AS nodeId,
            message_blobs.blobId AS blobId,
            message_blobs.readCapability AS readCapability,
            message_blobs.ciphertextByteSize AS ciphertextByteSize,
            message_blobs.blobExpiresAtEpochMilliseconds AS blobExpiresAtEpochMilliseconds,
            message_blobs.encryptionKey AS encryptionKey,
            message_blobs.nonce AS nonce,
            message_blobs.ciphertextSha256 AS ciphertextSha256,
            message_blobs.deleteCapability AS deleteCapability,
            message_blobs.localFilePath AS localFilePath
        FROM message_parts
        INNER JOIN message_blobs ON message_blobs.partId = message_parts.id
        WHERE message_parts.id = :partId
        LIMIT 1
        """
    )
    suspend fun findById(partId: String): MessageBlobPartRowDto?

    @Query("UPDATE message_blobs SET localFilePath = :localFilePath WHERE partId = :partId")
    suspend fun updateLocalFilePath(partId: String, localFilePath: String): Int

    @Query(
        """
        DELETE FROM message_parts
        WHERE messageId IN (:messageIds)
          AND type IN ('IMAGE', 'VIDEO', 'FILE', 'VOICE', 'LOCATION', 'CONTACT')
        """
    )
    suspend fun deleteByMessageIds(messageIds: List<String>)
}
