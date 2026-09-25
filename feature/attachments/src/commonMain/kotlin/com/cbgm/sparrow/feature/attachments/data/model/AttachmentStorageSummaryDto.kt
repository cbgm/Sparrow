package com.cbgm.sparrow.feature.attachments.data.model

import com.cbgm.sparrow.data.database.model.LocalMessageAttachmentRowDto

internal data class AttachmentStorageSummaryDto(
    val conversationId: String,
    val displayName: String,
    val isGroup: Boolean,
    val rows: List<LocalMessageAttachmentRowDto>
)
