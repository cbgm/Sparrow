package com.cbgm.sparrow.data.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "conversations",
    foreignKeys = [
        ForeignKey(
            entity = ContactEntity::class,
            parentColumns = ["id"],
            childColumns = ["contactId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(
            value = ["contactId"],
            unique = true
        ),
        Index(value = ["type"]),
        Index(value = ["updatedAtEpochMilliseconds"])
    ]
)
data class ConversationEntity(
    @PrimaryKey
    val id: String,
    /**
     * Kept for direct-conversation lookup and migration compatibility.
     * Group conversations leave this null and use conversation_participants.
     */
    val contactId: String?,
    val type: String,
    val title: String?,
    val createdAtEpochMilliseconds: Long,
    val updatedAtEpochMilliseconds: Long,
    @ColumnInfo(defaultValue = "0")
    val unseenLocalMessageCount: Int = 0
)
