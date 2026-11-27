package com.cbgm.sparrow.data.database.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/** An incoming edit, delete, reaction or poll operation whose message has not arrived yet. */
@Entity(
    tableName = "parked_message_operations",
    indices = [Index("targetMessageId"), Index("parkedAtEpochMilliseconds")]
)
data class ParkedMessageOperationEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val targetMessageId: String,
    val conversationId: String,
    val conversation: String,
    val senderContactId: String,
    val operation: String,
    val sentAtEpochMilliseconds: Long,
    val parkedAtEpochMilliseconds: Long
)
