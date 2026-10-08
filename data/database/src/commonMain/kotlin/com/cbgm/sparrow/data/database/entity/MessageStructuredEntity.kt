package com.cbgm.sparrow.data.database.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

@Entity(
    tableName = "message_structured",
    foreignKeys = [
        ForeignKey(
            entity = MessagePartEntity::class,
            parentColumns = ["id"],
            childColumns = ["partId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class MessageStructuredEntity(
    @PrimaryKey
    val partId: String,
    val json: String
)
