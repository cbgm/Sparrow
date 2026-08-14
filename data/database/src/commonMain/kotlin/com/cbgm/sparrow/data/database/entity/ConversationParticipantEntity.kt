package com.cbgm.sparrow.data.database.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index

@Entity(
    tableName = "conversation_participants",
    primaryKeys = [
        "conversationId",
        "contactId"
    ],
    foreignKeys = [
        ForeignKey(
            entity = ConversationEntity::class,
            parentColumns = ["id"],
            childColumns = ["conversationId"],
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
        Index(value = ["conversationId"]),
        Index(value = ["contactId"])
    ]
)
data class ConversationParticipantEntity(
    val conversationId: String,
    val contactId: String,
    val role: String,
    val joinedAtEpochMilliseconds: Long
)
