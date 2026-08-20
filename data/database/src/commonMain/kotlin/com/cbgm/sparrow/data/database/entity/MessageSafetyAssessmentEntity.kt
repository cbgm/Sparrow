package com.cbgm.sparrow.data.database.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "message_safety_assessments",
    foreignKeys = [
        ForeignKey(
            entity = MessageEntity::class,
            parentColumns = ["id"],
            childColumns = ["messageId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["analyzerVersion", "risk"])]
)
data class MessageSafetyAssessmentEntity(
    @PrimaryKey
    val messageId: String,
    val analyzerVersion: Int,
    val risk: String,
    val reasons: String
)
