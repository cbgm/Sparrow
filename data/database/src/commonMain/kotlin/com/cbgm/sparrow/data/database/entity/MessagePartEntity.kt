package com.cbgm.sparrow.data.database.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "message_parts",
    foreignKeys = [
        ForeignKey(
            entity = MessageEntity::class,
            parentColumns = ["id"],
            childColumns = ["messageId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["messageId"]),
        Index(value = ["messageId", "position"], unique = true)
    ]
)
data class MessagePartEntity(
    @PrimaryKey
    val id: String,
    val messageId: String,
    val position: Int,
    val type: String,
    val payload: String? = null
)
