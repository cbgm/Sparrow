package com.cbgm.sparrow.data.database.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.cbgm.sparrow.data.database.entity.MessageReactionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface MessageReactionDao {
    @Query("SELECT * FROM message_reactions WHERE conversationId = :conversationId")
    fun observeByConversationId(conversationId: String): Flow<List<MessageReactionEntity>>

    @Query(
        """
        SELECT message_reactions.*
        FROM message_reactions
        INNER JOIN messages ON messages.id = message_reactions.messageId
        WHERE messages.conversationId = :conversationId
          AND message_reactions.messageId IN (
              SELECT id
              FROM messages
              WHERE conversationId = :conversationId
              ORDER BY createdAtEpochMilliseconds DESC, id DESC
              LIMIT :messageLimit
          )
        """
    )
    fun observeRecentByConversationId(
        conversationId: String,
        messageLimit: Int
    ): Flow<List<MessageReactionEntity>>

    @Query(
        """
        SELECT message_reactions.*
        FROM message_reactions
        INNER JOIN messages ON messages.id = message_reactions.messageId
        WHERE messages.conversationId = :conversationId
          AND (
              messages.createdAtEpochMilliseconds > :fromTimestamp
              OR (
                  messages.createdAtEpochMilliseconds = :fromTimestamp
                  AND messages.id >= :fromMessageId
              )
          )
        """
    )
    fun observeFromMessageCursor(
        conversationId: String,
        fromTimestamp: Long,
        fromMessageId: String
    ): Flow<List<MessageReactionEntity>>

    @Query("SELECT * FROM message_reactions WHERE messageId = :messageId AND reactorId = :reactorId AND emoji = :emoji LIMIT 1")
    suspend fun find(messageId: String, reactorId: String, emoji: String): MessageReactionEntity?

    @Upsert
    suspend fun upsert(reaction: MessageReactionEntity)

    @Query("DELETE FROM message_reactions WHERE messageId = :messageId AND reactorId = :reactorId AND emoji = :emoji")
    suspend fun delete(messageId: String, reactorId: String, emoji: String)
}
