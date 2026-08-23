package com.cbgm.sparrow.feature.chats.domain.model.attachment

data class MessageMediaAttachment(
    val id: String,
    val type: MessageMediaType,
    val mimeType: String,
    val width: Int? = null,
    val height: Int? = null,
    val durationMilliseconds: Long? = null
)
