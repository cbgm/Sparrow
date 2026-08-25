package com.cbgm.sparrow.data.database.model

import androidx.room.Embedded
import com.cbgm.sparrow.data.database.entity.MessageAttachmentEntity

data class LocalMessageAttachmentRow(
    @Embedded
    val attachment: MessageAttachmentEntity,
    val conversationId: String,
    val createdAtEpochMilliseconds: Long
)
