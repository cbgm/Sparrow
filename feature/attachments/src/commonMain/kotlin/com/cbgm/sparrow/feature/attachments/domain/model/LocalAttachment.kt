package com.cbgm.sparrow.feature.attachments.domain.model

data class LocalAttachment(
    val id: String,
    val conversationId: String,
    val type: LocalAttachmentType,
    val mimeType: String,
    val byteSize: Long,
    val fileName: String?,
    val width: Int?,
    val height: Int?,
    val durationMilliseconds: Long?,
    val createdAtEpochMilliseconds: Long
)

enum class LocalAttachmentType {
    IMAGE,
    VIDEO,
    FILE
}
