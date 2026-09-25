package com.cbgm.sparrow.data.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "auto_replies")
data class AutoReplyEntity(
    @PrimaryKey
    val id: String,
    val name: String,
    val text: String,
    val isActive: Boolean,
    val activationSessionId: String?,
    val createdAtEpochMilliseconds: Long,
    val updatedAtEpochMilliseconds: Long
)
