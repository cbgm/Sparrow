package com.cbgm.sparrow.data.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/** Attachments-owned, persisted snapshot supplied by the conversation owner. */
@Entity(
    tableName = "attachment_message_contexts",
    foreignKeys = [
        ForeignKey(
            entity = MessageEntity::class,
            parentColumns = ["id"],
            childColumns = ["messageId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["conversationId"])]
)
data class AttachmentMessageContextEntity(
    @PrimaryKey val messageId: String,
    val conversationId: String,
    @ColumnInfo(defaultValue = "0") val createdAtEpochMilliseconds: Long,
    val displayName: String,
    val isGroup: Boolean,
    val isMine: Boolean,
    val senderContactId: String?
)
