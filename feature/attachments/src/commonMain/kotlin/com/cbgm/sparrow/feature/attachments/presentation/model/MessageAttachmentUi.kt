package com.cbgm.sparrow.feature.attachments.presentation.model

import com.cbgm.sparrow.core.protocol.attachment.MessageAttachmentType
import com.cbgm.sparrow.feature.attachments.domain.model.AttachmentSource
import com.cbgm.sparrow.feature.attachments.domain.model.AttachmentTarget

sealed interface MessageAttachmentUi {
    val id: String
    val source: AttachmentSource

    val target: AttachmentTarget

    data class ImageVideoAttachmentUi(
        override val id: String,
        val type: MessageAttachmentType,
        val mimeType: String,
        val byteSize: Long,
        val fileName: String? = null,
        val width: Int? = null,
        val height: Int? = null,
        val durationMilliseconds: Long? = null,
        override val source: AttachmentSource = AttachmentSource.Message
    ) : MessageAttachmentUi {
        override val target: AttachmentTarget
            get() = AttachmentTarget(id = id, type = type, source = source)
    }

    data class FileAttachmentUi(
        override val id: String,
        val mimeType: String,
        val byteSize: Long,
        val fileName: String,
        override val source: AttachmentSource = AttachmentSource.Message
    ) : MessageAttachmentUi {
        override val target: AttachmentTarget
            get() = AttachmentTarget(id = id, type = MessageAttachmentType.FILE, source = source)
    }

    data class LocationAttachmentUi(
        override val id: String,
        override val source: AttachmentSource = AttachmentSource.Message
    ) : MessageAttachmentUi {
        override val target: AttachmentTarget
            get() = AttachmentTarget(id = id, type = MessageAttachmentType.LOCATION, source = source)
    }

    data class ContactAttachmentUi(
        override val id: String,
        override val source: AttachmentSource = AttachmentSource.Message
    ) : MessageAttachmentUi {
        override val target: AttachmentTarget
            get() = AttachmentTarget(id = id, type = MessageAttachmentType.CONTACT, source = source)
    }
}
