package com.cbgm.sparrow.feature.attachments.data.mapper

import com.cbgm.sparrow.core.messagepart.data.model.FileDto
import com.cbgm.sparrow.core.messagepart.data.model.ImageDto
import com.cbgm.sparrow.core.messagepart.data.model.MessagePartDto
import com.cbgm.sparrow.core.messagepart.data.model.VideoDto
import com.cbgm.sparrow.core.messagepart.data.model.VoiceDto
import com.cbgm.sparrow.feature.attachments.data.model.AttachmentStorageSummaryDto
import com.cbgm.sparrow.feature.attachments.domain.model.AttachmentStorageSummary

internal fun AttachmentStorageSummaryDto.toAttachmentStorageSummary(): AttachmentStorageSummary {
    val storedParts = parts.filter { part ->
        part is ImageDto || part is VideoDto || part is FileDto || part is VoiceDto
    }
    return AttachmentStorageSummary(
        conversationId = conversationId,
        displayName = displayName,
        isGroup = isGroup,
        mediaCount = storedParts.count { part -> part !is FileDto },
        fileCount = storedParts.count { part -> part is FileDto },
        byteSize = storedParts.sumOf(MessagePartDto::byteSize)
    )
}

private val MessagePartDto.byteSize: Long
    get() =
        when (this) {
            is ImageDto -> byteSize
            is VideoDto -> byteSize
            is FileDto -> byteSize
            is VoiceDto -> byteSize
            else -> 0L
        }
