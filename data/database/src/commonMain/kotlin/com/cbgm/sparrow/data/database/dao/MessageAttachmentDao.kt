package com.cbgm.sparrow.data.database.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.cbgm.sparrow.data.database.entity.AttachmentMessageContextEntity
import com.cbgm.sparrow.data.database.entity.MessageAttachmentEntity
import com.cbgm.sparrow.data.database.model.LocalMessageAttachmentRowDto
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
    suspend fun updateConversationDisplayName(conversationId: String, displayName: String, isGroup: Boolean): Int

    @Upsert
    suspend fun upsertAll(attachments: List<MessageAttachmentEntity>)

    /** Messages owner supplies the visible page IDs; only Attachments rows are queried here. */
    @Query(
        """SELECT * FROM message_attachments
              WHERE messageId IN (:messageIds)
              ORDER BY messageId ASC, position ASC"""
    )
    fun observeByMessageIds(messageIds: List<String>): Flow<List<MessageAttachmentEntity>>

    @Query(
        """
        SELECT *
        FROM message_attachments
        WHERE messageId = :messageId
        ORDER BY position ASC
        """
    )
    suspend fun findByMessageId(messageId: String): List<MessageAttachmentEntity>

    @Query(
        """
        SELECT *
        FROM message_attachments
        WHERE messageId IN (:messageIds)
        ORDER BY messageId ASC, position ASC
        """
    )
    suspend fun findByMessageIds(messageIds: List<String>): List<MessageAttachmentEntity>

    @Query(
        """SELECT message_attachments.* FROM message_attachments
              INNER JOIN attachment_message_contexts AS ctx
              ON ctx.messageId = message_attachments.messageId
              WHERE ctx.conversationId = :conversationId
              ORDER BY ctx.createdAtEpochMilliseconds ASC, ctx.messageId ASC, message_attachments.position ASC"""
    )
    suspend fun findByConversationId(conversationId: String): List<MessageAttachmentEntity>

    @Query(
        """SELECT message_attachments.*,
                     ctx.conversationId AS conversationId,
                     ctx.createdAtEpochMilliseconds AS createdAtEpochMilliseconds,
                     ctx.displayName AS displayName,
                     ctx.isGroup AS isGroup
              FROM message_attachments
              INNER JOIN attachment_message_contexts AS ctx
              ON ctx.messageId = message_attachments.messageId
              WHERE message_attachments.localFileName IS NOT NULL
              ORDER BY ctx.createdAtEpochMilliseconds DESC, message_attachments.position ASC"""
    )
    fun observeAllLocal(): Flow<List<LocalMessageAttachmentRowDto>>

    @Query(
        """SELECT message_attachments.*,
                     ctx.conversationId AS conversationId,
                     ctx.createdAtEpochMilliseconds AS createdAtEpochMilliseconds,
                     ctx.displayName AS displayName,
                     ctx.isGroup AS isGroup
              FROM message_attachments
              INNER JOIN attachment_message_contexts AS ctx
              ON ctx.messageId = message_attachments.messageId
              WHERE ctx.conversationId = :conversationId AND message_attachments.localFileName IS NOT NULL
              ORDER BY ctx.createdAtEpochMilliseconds DESC, message_attachments.position ASC"""
    )
    fun observeLocalByConversationId(conversationId: String): Flow<List<LocalMessageAttachmentRowDto>>

    @Query(
        """SELECT message_attachments.*,
                     ctx.conversationId AS conversationId,
                     ctx.createdAtEpochMilliseconds AS createdAtEpochMilliseconds,
                     ctx.displayName AS displayName,
                     ctx.isGroup AS isGroup
              FROM message_attachments
              INNER JOIN attachment_message_contexts AS ctx
              ON ctx.messageId = message_attachments.messageId
              WHERE message_attachments.id IN (:attachmentIds)
                AND message_attachments.localFileName IS NOT NULL"""
    )
    suspend fun findLocalRowsByIds(attachmentIds: List<String>): List<LocalMessageAttachmentRowDto>

    @Query(
        """
        UPDATE message_attachments
        SET localFileName = NULL
        WHERE id IN (:attachmentIds)
        """
    )
    suspend fun clearLocalFileNames(attachmentIds: List<String>): Int

    @Query(
        """
        UPDATE message_attachments
        SET localFileName = NULL
        WHERE messageId IN (
            SELECT messageId FROM attachment_message_contexts
            WHERE conversationId = :conversationId
        )
        """
    )
    suspend fun clearLocalFileNamesForConversation(conversationId: String): Int

    @Query("SELECT * FROM message_attachments WHERE id = :attachmentId LIMIT 1")
    fun observeById(attachmentId: String): Flow<MessageAttachmentEntity?>

    @Query("SELECT * FROM message_attachments WHERE id = :attachmentId LIMIT 1")
    suspend fun findById(attachmentId: String): MessageAttachmentEntity?

    @Query(
        """
        UPDATE message_attachments
        SET localFileName = :localFileName
        WHERE id = :attachmentId
        """
    )
    suspend fun updateLocalFileName(
        attachmentId: String,
        localFileName: String
    ): Int

    @Query(
        """
        UPDATE message_attachments
        SET payloadBytes = :payloadBytes
        WHERE id = :attachmentId
        """
    )
    suspend fun updatePayloadBytes(
        attachmentId: String,
        payloadBytes: ByteArray
    ): Int

    @Query(
        """
        UPDATE message_attachments
        SET transcript = :transcript
        WHERE id = :attachmentId
        """
    )
    suspend fun updateTranscript(
        attachmentId: String,
        transcript: String
    ): Int

    @Query("DELETE FROM message_attachments WHERE messageId IN (:messageIds)")
    suspend fun deleteByMessageIds(messageIds: List<String>)
}
