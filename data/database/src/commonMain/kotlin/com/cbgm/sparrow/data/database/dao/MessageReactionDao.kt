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

    @Query("SELECT * FROM message_reactions WHERE messageId = :messageId AND reactorId = :reactorId AND emoji = :emoji LIMIT 1")
    suspend fun find(messageId: String, reactorId: String, emoji: String): MessageReactionEntity?

    @Upsert
    suspend fun upsert(reaction: MessageReactionEntity)

    @Query("DELETE FROM message_reactions WHERE messageId = :messageId AND reactorId = :reactorId AND emoji = :emoji")
    suspend fun delete(messageId: String, reactorId: String, emoji: String)
}
