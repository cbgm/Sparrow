package com.cbgm.sparrow.data.database.model

import androidx.room.Embedded

data class LocalMessageAttachmentRowDto(
    @Embedded
    val attachment: MessageBlobPartRowDto,
    val conversationId: String,
    val createdAtEpochMilliseconds: Long,
    val displayName: String,
    val isGroup: Boolean
)
