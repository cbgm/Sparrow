package com.cbgm.sparrow.feature.chats.presentation.common.history.model

import com.cbgm.sparrow.feature.attachments.domain.model.AttachmentSource
import com.cbgm.sparrow.feature.linkpreview.presentation.model.TextContentPart
import com.cbgm.sparrow.feature.linkpreview.presentation.model.toTextContentParts

sealed interface MessagePartUi {
    data class ImageVideo(
        val id: String,
        val type: ImageVideoTypeUi,
        val mimeType: String,
        val byteSize: Long,
        val fileName: String? = null,
        val width: Int? = null,
        val height: Int? = null,
        val durationMilliseconds: Long? = null,
        val attachmentSource: AttachmentSource = AttachmentSource.Message
    ) : MessagePartUi

    data class File(
        val id: String,
        val mimeType: String,
        val byteSize: Long,
        val fileName: String,
        val attachmentSource: AttachmentSource = AttachmentSource.Message
    ) : MessagePartUi

    data class Location(
        val id: String,
        val attachmentSource: AttachmentSource = AttachmentSource.Message
    ) : MessagePartUi

    data class Contact(
        val id: String,
        val attachmentSource: AttachmentSource = AttachmentSource.Message
    ) : MessagePartUi

    data class Text(
        val text: String,
        val isContentFailed: Boolean,
        val contentParts: List<TextContentPart> = text.toTextContentParts()
    ) : MessagePartUi

    data class Voice(
        val id: String = "",
        val mimeType: String = "audio/wav",
        val byteSize: Long = 0L,
        val durationMilliseconds: Long,
        val attachmentSource: AttachmentSource = AttachmentSource.Message
    ) : MessagePartUi
}

enum class ImageVideoTypeUi {
    IMAGE,
    VIDEO
}
