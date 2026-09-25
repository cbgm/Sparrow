package com.cbgm.sparrow.data.database.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index

@Entity(
    tableName = "message_reactions",
    primaryKeys = ["messageId", "reactorId", "emoji"],
    foreignKeys = [
        ForeignKey(
            entity = MessageEntity::class,
            parentColumns = ["id"],
            childColumns = ["messageId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("messageId"), Index("conversationId")]
)
data class MessageReactionEntity(
    val messageId: String,
    val conversationId: String,
    val reactorId: String,
    val emoji: String
) {
    companion object {
        const val LOCAL_REACTOR_ID = "__local__"
    }
}
