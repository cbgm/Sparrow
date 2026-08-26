package com.cbgm.sparrow.feature.attachments.domain.model

data class MessageFileAttachment(
    val id: String,
    val mimeType: String,
    val byteSize: Long,
    val fileName: String
)
