package com.cbgm.sparrow.feature.attachments.domain.model

import com.cbgm.sparrow.feature.media.domain.model.MediaContentType

sealed interface LocalAttachment {
    val id: String
    val conversationId: String
    val mimeType: String
    val byteSize: Long
    val createdAtEpochMilliseconds: Long

    data class Media(
        override val id: String,
        override val conversationId: String,
        override val mimeType: String,
        override val byteSize: Long,
        val mediaType: MediaContentType,
        val fileName: String?,
        val width: Int?,
        val height: Int?,
        val durationMilliseconds: Long?,
        override val createdAtEpochMilliseconds: Long
    ) : LocalAttachment

    data class File(
        override val id: String,
        override val conversationId: String,
        override val mimeType: String,
        override val byteSize: Long,
        val fileName: String?,
        override val createdAtEpochMilliseconds: Long
    ) : LocalAttachment

    data class Voice(
        override val id: String,
        override val conversationId: String,
        override val mimeType: String,
        override val byteSize: Long,
        val durationMilliseconds: Long,
        override val createdAtEpochMilliseconds: Long
    ) : LocalAttachment
}
