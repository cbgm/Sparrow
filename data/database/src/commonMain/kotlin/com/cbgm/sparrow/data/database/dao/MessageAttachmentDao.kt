package com.cbgm.sparrow.data.database.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import com.cbgm.sparrow.data.database.entity.AttachmentMessageContextEntity
import com.cbgm.sparrow.data.database.entity.MessageBlobEntity
import com.cbgm.sparrow.data.database.entity.MessagePartEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface MessageAttachmentDao {
    @Upsert
    suspend fun upsertMessageContext(context: AttachmentMessageContextEntity)

    @Query("SELECT * FROM attachment_message_contexts WHERE messageId = :messageId LIMIT 1")
    suspend fun findMessageContext(messageId: String): AttachmentMessageContextEntity?

    @Query("SELECT * FROM attachment_message_contexts WHERE messageId IN (:messageIds)")
    suspend fun findMessageContexts(messageIds: List<String>): List<AttachmentMessageContextEntity>

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

    @Transaction
    suspend fun upsertMessageParts(
        parts: List<MessagePartEntity>,
        blobs: List<MessageBlobEntity>
    ) {
        upsertParts(parts)
        upsertBlobs(blobs)
    }

    @Query(
        """
        SELECT message_parts.*
        FROM message_parts
        LEFT JOIN message_blobs ON message_blobs.partId = message_parts.id
        WHERE message_parts.messageId IN (:messageIds)
          AND message_parts.type != 'TEXT'
        ORDER BY message_parts.messageId, message_parts.position
        """
    )
    fun observeMessagePartsByMessageIds(messageIds: List<String>): Flow<List<MessagePartEntity>>

    @Query("SELECT * FROM message_parts WHERE messageId = :messageId AND type != 'TEXT' ORDER BY position")
    suspend fun findMessagePartsByMessageId(messageId: String): List<MessagePartEntity>

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
        SELECT message_parts.*
        FROM message_parts
        INNER JOIN message_blobs ON message_blobs.partId = message_parts.id
        WHERE message_parts.messageId IN (:messageIds)
        ORDER BY message_parts.messageId ASC, message_parts.position ASC
        """
    )
    fun observeBlobPartsByMessageIds(messageIds: List<String>): Flow<List<MessagePartEntity>>

    @Query(
        """
        SELECT message_parts.*
        FROM message_parts
        INNER JOIN message_blobs ON message_blobs.partId = message_parts.id
        WHERE message_parts.messageId = :messageId
        ORDER BY message_parts.position ASC
        """
    )
    suspend fun findBlobPartsByMessageId(messageId: String): List<MessagePartEntity>

    @Query(
        """
        SELECT message_parts.*
        FROM message_parts
        INNER JOIN message_blobs ON message_blobs.partId = message_parts.id
        WHERE message_parts.messageId IN (:messageIds)
        ORDER BY message_parts.messageId ASC, message_parts.position ASC
        """
    )
    suspend fun findBlobPartsByMessageIds(messageIds: List<String>): List<MessagePartEntity>

    @Query(
        """
        SELECT message_parts.*
        FROM message_parts
        INNER JOIN message_blobs ON message_blobs.partId = message_parts.id
        INNER JOIN attachment_message_contexts AS ctx ON ctx.messageId = message_parts.messageId
        WHERE ctx.conversationId = :conversationId
        ORDER BY ctx.createdAtEpochMilliseconds ASC, message_parts.messageId ASC, message_parts.position ASC
        """
    )
    suspend fun findBlobPartsByConversationId(conversationId: String): List<MessagePartEntity>

    @Query(
        """
        SELECT message_parts.*
        FROM message_parts
        INNER JOIN message_blobs ON message_blobs.partId = message_parts.id
        INNER JOIN attachment_message_contexts AS ctx ON ctx.messageId = message_parts.messageId
        WHERE message_blobs.localFilePath IS NOT NULL
        ORDER BY ctx.createdAtEpochMilliseconds DESC, message_parts.position ASC
        """
    )
    fun observeAllLocalParts(): Flow<List<MessagePartEntity>>

    @Query(
        """
        SELECT message_parts.*
        FROM message_parts
        INNER JOIN message_blobs ON message_blobs.partId = message_parts.id
        INNER JOIN attachment_message_contexts AS ctx ON ctx.messageId = message_parts.messageId
        WHERE ctx.conversationId = :conversationId
          AND message_blobs.localFilePath IS NOT NULL
        ORDER BY ctx.createdAtEpochMilliseconds DESC, message_parts.position ASC
        """
    )
    fun observeLocalPartsByConversationId(conversationId: String): Flow<List<MessagePartEntity>>

    @Query(
        """
        SELECT message_parts.*
        FROM message_parts
        INNER JOIN message_blobs ON message_blobs.partId = message_parts.id
        WHERE message_parts.id IN (:partIds)
          AND message_blobs.localFilePath IS NOT NULL
        """
    )
    suspend fun findLocalPartsByIds(partIds: List<String>): List<MessagePartEntity>

    @Query("SELECT * FROM message_parts WHERE id = :partId LIMIT 1")
    suspend fun findPartById(partId: String): MessagePartEntity?

    @Query("SELECT * FROM message_blobs WHERE partId IN (:partIds)")
    suspend fun findBlobsByPartIds(partIds: List<String>): List<MessageBlobEntity>

    @Query("SELECT * FROM message_blobs WHERE partId = :partId LIMIT 1")
    suspend fun findBlobByPartId(partId: String): MessageBlobEntity?

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

    @Query("UPDATE message_blobs SET localFilePath = :localFilePath WHERE partId = :partId")
    suspend fun updateLocalFilePath(partId: String, localFilePath: String): Int

    @Query(
        """
        UPDATE message_blobs
        SET nodeId = :nodeId,
            blobId = :blobId,
            readCapability = :readCapability,
            ciphertextByteSize = :ciphertextByteSize,
            blobExpiresAtEpochMilliseconds = :blobExpiresAtEpochMilliseconds,
            encryptionKey = :encryptionKey,
            nonce = :nonce,
            ciphertextSha256 = :ciphertextSha256,
            deleteCapability = :deleteCapability
        WHERE partId = :partId
        """
    )
    suspend fun updateRemoteBlobReference(
        partId: String,
        nodeId: String,
        blobId: String,
        readCapability: String,
        ciphertextByteSize: Long,
        blobExpiresAtEpochMilliseconds: Long,
        encryptionKey: ByteArray,
        nonce: ByteArray,
        ciphertextSha256: ByteArray,
        deleteCapability: String
    ): Int

    @Query(
        """
        DELETE FROM message_parts
        WHERE messageId IN (:messageIds)
          AND type IN ('IMAGE', 'VIDEO', 'FILE', 'VOICE', 'LOCATION', 'CONTACT', 'POLL')
        """
    )
    suspend fun deleteByMessageIds(messageIds: List<String>)
}
