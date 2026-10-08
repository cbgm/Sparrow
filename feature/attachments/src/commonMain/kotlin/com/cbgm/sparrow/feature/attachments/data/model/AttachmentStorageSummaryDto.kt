package com.cbgm.sparrow.feature.attachments.data.model

import com.cbgm.sparrow.core.messagepart.data.model.MessagePartDto

internal data class AttachmentStorageSummaryDto(
    val conversationId: String,
    val displayName: String,
    val isGroup: Boolean,
    val parts: List<MessagePartDto>
)
