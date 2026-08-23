package com.cbgm.sparrow.data.database.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.cbgm.sparrow.data.database.entity.MessageAttachmentEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface MessageAttachmentDao {
    @Upsert
    suspend fun upsertAll(attachments: List<MessageAttachmentEntity>)

    @Query(
        """
        SELECT message_attachments.*
        FROM message_attachments
        INNER JOIN messages ON messages.id = message_attachments.messageId
        WHERE messages.conversationId = :conversationId
          AND message_attachments.messageId IN (
              SELECT id
              FROM messages
              WHERE conversationId = :conversationId
              ORDER BY createdAtEpochMilliseconds DESC, id DESC
              LIMIT :messageLimit
          )
        ORDER BY messages.createdAtEpochMilliseconds ASC, message_attachments.position ASC
        """
    )
    fun observeRecentByConversation(
        conversationId: String,
        messageLimit: Int
    ): Flow<List<MessageAttachmentEntity>>

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
        """
        SELECT message_attachments.*
        FROM message_attachments
        INNER JOIN messages ON messages.id = message_attachments.messageId
        WHERE messages.conversationId = :conversationId
        ORDER BY messages.createdAtEpochMilliseconds ASC, message_attachments.position ASC
        """
    )
    suspend fun findByConversationId(conversationId: String): List<MessageAttachmentEntity>

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

    @Query("DELETE FROM message_attachments WHERE messageId IN (:messageIds)")
    suspend fun deleteByMessageIds(messageIds: List<String>)
}
