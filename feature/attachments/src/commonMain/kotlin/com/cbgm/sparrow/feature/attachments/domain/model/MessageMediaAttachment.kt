package com.cbgm.sparrow.feature.attachments.domain.model

import com.cbgm.sparrow.feature.media.domain.model.MediaContentType

data class MessageMediaAttachment(
    val id: String,
    val type: MediaContentType,
    val mimeType: String,
    val width: Int? = null,
    val height: Int? = null,
    val durationMilliseconds: Long? = null
)
