package com.cbgm.sparrow.data.database.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index

@Entity(
    tableName = "message_recipient_states",
    primaryKeys = [
        "messageId",
        "contactId"
    ],
    foreignKeys = [
        ForeignKey(
            entity = MessageEntity::class,
            parentColumns = ["id"],
            childColumns = ["messageId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = ContactEntity::class,
            parentColumns = ["id"],
            childColumns = ["contactId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["messageId"]),
        Index(value = ["contactId"]),
        Index(
            value = ["packetId"],
            unique = true
        )
    ]
)
data class MessageRecipientStateEntity(
    val messageId: String,
    val contactId: String,
    val packetId: String?,
    val deliveryStatus: String,
    val lastError: String?,
    val updatedAtEpochMilliseconds: Long
)
