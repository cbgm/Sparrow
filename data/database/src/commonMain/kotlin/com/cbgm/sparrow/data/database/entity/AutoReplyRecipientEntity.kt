package com.cbgm.sparrow.data.database.entity

import androidx.room.Entity
import androidx.room.ForeignKey

@Entity(
    tableName = "auto_reply_recipients",
    primaryKeys = ["contactId"],
    foreignKeys = [
        ForeignKey(
            entity = ContactEntity::class,
            parentColumns = ["id"],
            childColumns = ["contactId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class AutoReplyRecipientEntity(
    val contactId: String,
    val sentAtEpochMilliseconds: Long
)
