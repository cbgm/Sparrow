package com.cbgm.sparrow.data.database.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.cbgm.sparrow.data.database.entity.MessageSearchEmbeddingEntity
import com.cbgm.sparrow.data.database.model.MessageSearchSourceDto
import com.cbgm.sparrow.data.database.model.StoredMessageEmbeddingDto
import com.cbgm.sparrow.data.database.model.StoredMessageSearchMatchDto

@Dao
interface MessageSearchDao {
    @Query(
        """
        SELECT
            messages.id AS messageId,
            messages.conversationId AS conversationId,
            conversations.type AS conversationType,
            conversations.contactId AS contactId,
            CASE
                WHEN messages.isMine = 0
                    THEN COALESCE(sender_contacts.displayName, conversation_contacts.displayName)
                ELSE NULL
            END AS senderName,
            conversations.title AS conversationTitle,
            conversation_contacts.displayName AS contactName,
            message_text.text AS text,
            messages.createdAtEpochMilliseconds AS createdAtEpochMilliseconds
        FROM messages
        INNER JOIN message_parts
            ON message_parts.messageId = messages.id
            AND message_parts.type = 'TEXT'
        INNER JOIN message_text ON message_text.partId = message_parts.id
        INNER JOIN conversations ON conversations.id = messages.conversationId
        LEFT JOIN contacts AS conversation_contacts
            ON conversation_contacts.id = conversations.contactId
        LEFT JOIN contacts AS sender_contacts
            ON sender_contacts.id = messages.senderContactId
        LEFT JOIN message_search_embeddings
            ON message_search_embeddings.messageId = messages.id
            AND message_search_embeddings.modelVersion = :modelVersion
        WHERE messages.contentStatus = 'READABLE'
          AND TRIM(message_text.text) != ''
          AND message_search_embeddings.messageId IS NULL
        ORDER BY messages.createdAtEpochMilliseconds ASC
        LIMIT :limit
        """
    )
    suspend fun getMessagesMissingEmbedding(
        modelVersion: Int,
        limit: Int
    ): List<MessageSearchSourceDto>

    @Query(
        """
        SELECT COUNT(*)
        FROM messages
        INNER JOIN message_parts
            ON message_parts.messageId = messages.id
            AND message_parts.type = 'TEXT'
        INNER JOIN message_text ON message_text.partId = message_parts.id
        WHERE messages.contentStatus = 'READABLE'
          AND TRIM(message_text.text) != ''
        """
    )
    suspend fun getSearchableMessageCount(): Int

    @Query(
        """
        SELECT COUNT(*)
        FROM message_search_embeddings
        WHERE modelVersion = :modelVersion
        """
    )
    suspend fun getIndexedMessageCount(modelVersion: Int): Int

    @Upsert
    suspend fun upsertEmbedding(embedding: MessageSearchEmbeddingEntity)

    @Query("DELETE FROM message_search_embeddings")
    suspend fun deleteAllEmbeddings()

    @Query("DELETE FROM message_search_embeddings WHERE modelVersion != :modelVersion")
    suspend fun deleteEmbeddingsForOtherModels(modelVersion: Int)

    @Query(
        """
        SELECT
            messages.id AS messageId,
            messages.conversationId AS conversationId,
            conversations.type AS conversationType,
            conversations.contactId AS contactId,
            conversations.title AS conversationTitle,
            conversation_contacts.displayName AS contactName,
            message_text.text AS text,
            messages.createdAtEpochMilliseconds AS createdAtEpochMilliseconds
        FROM messages
        INNER JOIN message_parts
            ON message_parts.messageId = messages.id
            AND message_parts.type = 'TEXT'
        INNER JOIN message_text ON message_text.partId = message_parts.id
        INNER JOIN conversations ON conversations.id = messages.conversationId
        LEFT JOIN contacts AS conversation_contacts
            ON conversation_contacts.id = conversations.contactId
        WHERE messages.contentStatus = 'READABLE'
          AND TRIM(message_text.text) != ''
          AND INSTR(LOWER(message_text.text), LOWER(:query)) > 0
        ORDER BY messages.createdAtEpochMilliseconds DESC
        LIMIT :limit
        """
    )
    suspend fun searchExactMessages(
        query: String,
        limit: Int
    ): List<StoredMessageSearchMatchDto>

    @Query(
        """
        SELECT
            messages.id AS messageId,
            messages.conversationId AS conversationId,
            conversations.type AS conversationType,
            conversations.contactId AS contactId,
            CASE
                WHEN messages.isMine = 0
                    THEN COALESCE(sender_contacts.displayName, conversation_contacts.displayName)
                ELSE NULL
            END AS senderName,
            conversations.title AS conversationTitle,
            conversation_contacts.displayName AS contactName,
            message_text.text AS text,
            messages.createdAtEpochMilliseconds AS createdAtEpochMilliseconds,
            message_search_embeddings.embedding AS embedding
        FROM message_search_embeddings
        INNER JOIN messages ON messages.id = message_search_embeddings.messageId
        INNER JOIN message_parts
            ON message_parts.messageId = messages.id
            AND message_parts.type = 'TEXT'
        INNER JOIN message_text ON message_text.partId = message_parts.id
        INNER JOIN conversations ON conversations.id = messages.conversationId
        LEFT JOIN contacts AS conversation_contacts
            ON conversation_contacts.id = conversations.contactId
        LEFT JOIN contacts AS sender_contacts
            ON sender_contacts.id = messages.senderContactId
        WHERE message_search_embeddings.modelVersion = :modelVersion
          AND messages.contentStatus = 'READABLE'
          AND TRIM(message_text.text) != ''
        """
    )
    suspend fun getIndexedMessages(modelVersion: Int): List<StoredMessageEmbeddingDto>
}
