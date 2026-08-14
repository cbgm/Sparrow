package com.cbgm.sparrow.data.database.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "messages",
    foreignKeys = [
        ForeignKey(
            entity = ConversationEntity::class,
            parentColumns = ["id"],
            childColumns = ["conversationId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["conversationId"]),

        Index(
            value = [
                "conversationId",
                "createdAtEpochMilliseconds"
            ]
        ),

        Index(
            value = ["packetId"],
            unique = true
        )
    ]
)
data class MessageEntity(
    @PrimaryKey
    val id: String,
    val conversationId: String,
    val packetId: String?,
    val text: String,
    val transportPayload: String?,
    val transportMode: String,
    val contentStatus: String,
    val deliveryStatus: String,
    /** Contact that authored an incoming group/direct message. Null for local messages. */
    val senderContactId: String?,
    /**
     * True after this device has queued a ReadReceiptPacket for this
     * incoming message.
     *
     * Outgoing messages always keep this false.
     */
    val readReceiptSent: Boolean = false,
    val isMine: Boolean,
    val createdAtEpochMilliseconds: Long
)
