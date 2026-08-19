package com.cbgm.sparrow.data.database.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "message_search_embeddings",
    foreignKeys = [
        ForeignKey(
            entity = MessageEntity::class,
            parentColumns = ["id"],
            childColumns = ["messageId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["modelVersion"])]
)
data class MessageSearchEmbeddingEntity(
    @PrimaryKey
    val messageId: String,
    val modelVersion: Int,
    val embedding: ByteArray
)
