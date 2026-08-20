package com.cbgm.sparrow.data.database.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.cbgm.sparrow.data.database.entity.MessageSearchEmbeddingEntity
import com.cbgm.sparrow.data.database.model.MessageSearchSource
import com.cbgm.sparrow.data.database.model.StoredMessageEmbedding
import com.cbgm.sparrow.data.database.model.StoredMessageSearchMatch

@Dao
interface MessageSearchDao {
    @Query(
        """
        SELECT
            messages.id AS messageId,
            messages.conversationId AS conversationId,
            messages.text AS text,
            messages.createdAtEpochMilliseconds AS createdAtEpochMilliseconds
        FROM messages
        LEFT JOIN message_search_embeddings
            ON message_search_embeddings.messageId = messages.id
            AND message_search_embeddings.modelVersion = :modelVersion
        WHERE messages.contentStatus = 'READABLE'
          AND TRIM(messages.text) != ''
          AND message_search_embeddings.messageId IS NULL
        ORDER BY messages.createdAtEpochMilliseconds ASC
        LIMIT :limit
        """
    )
    suspend fun getMessagesMissingEmbedding(
        modelVersion: Int,
        limit: Int
    ): List<MessageSearchSource>

    @Query(
        """
        SELECT COUNT(*)
        FROM messages
        WHERE contentStatus = 'READABLE'
          AND TRIM(text) != ''
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
            conversations.title AS conversationTitle,
            contacts.displayName AS contactName,
            messages.text AS text,
            messages.createdAtEpochMilliseconds AS createdAtEpochMilliseconds
        FROM messages
        INNER JOIN conversations ON conversations.id = messages.conversationId
        LEFT JOIN contacts ON contacts.id = conversations.contactId
        WHERE messages.contentStatus = 'READABLE'
          AND TRIM(messages.text) != ''
          AND INSTR(LOWER(messages.text), LOWER(:query)) > 0
        ORDER BY messages.createdAtEpochMilliseconds DESC
        LIMIT :limit
        """
    )
    suspend fun searchExactMessages(
        query: String,
        limit: Int
    ): List<StoredMessageSearchMatch>

    @Query(
        """
        SELECT
            messages.id AS messageId,
            messages.conversationId AS conversationId,
            conversations.title AS conversationTitle,
            contacts.displayName AS contactName,
            messages.text AS text,
            messages.createdAtEpochMilliseconds AS createdAtEpochMilliseconds,
            message_search_embeddings.embedding AS embedding
        FROM message_search_embeddings
        INNER JOIN messages ON messages.id = message_search_embeddings.messageId
        INNER JOIN conversations ON conversations.id = messages.conversationId
        LEFT JOIN contacts ON contacts.id = conversations.contactId
        WHERE message_search_embeddings.modelVersion = :modelVersion
          AND messages.contentStatus = 'READABLE'
          AND TRIM(messages.text) != ''
        """
    )
    suspend fun getIndexedMessages(modelVersion: Int): List<StoredMessageEmbedding>
}
