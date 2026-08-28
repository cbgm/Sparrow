package com.cbgm.sparrow.feature.attachments.domain.model

data class AttachmentStorageSummary(
    val conversationId: String,
    val displayName: String,
    val isGroup: Boolean,
    val mediaCount: Int,
    val fileCount: Int,
    val byteSize: Long
)
