package com.cbgm.sparrow.data.database.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

@Entity(
    tableName = "message_text",
    foreignKeys = [
        ForeignKey(
            entity = MessagePartEntity::class,
            parentColumns = ["id"],
            childColumns = ["partId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class MessageTextEntity(
    @PrimaryKey
    val partId: String,
    val text: String
)
