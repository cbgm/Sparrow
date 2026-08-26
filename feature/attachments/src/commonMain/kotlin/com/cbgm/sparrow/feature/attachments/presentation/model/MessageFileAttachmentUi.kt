package com.cbgm.sparrow.feature.attachments.presentation.model

data class MessageFileAttachmentUi(
    val id: String,
    val fileName: String,
    val mimeType: String,
    val sizeText: String
)
