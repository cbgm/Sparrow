package com.cbgm.sparrow.feature.attachments.presentation.model

import com.cbgm.sparrow.core.protocol.attachment.MessageAttachmentType
import com.cbgm.sparrow.feature.attachments.domain.model.AttachmentSource
import com.cbgm.sparrow.feature.attachments.domain.model.AttachmentTarget
import com.cbgm.sparrow.feature.media.presentation.model.MediaItemUi
import com.cbgm.sparrow.feature.media.presentation.model.MediaTypeUi

sealed interface MessageAttachmentUi {
    val id: String
    val source: AttachmentSource

    val target: AttachmentTarget

    data class ImageVideoAttachmentUi(
        override val id: String,
        val media: MediaItemUi,
        val byteSize: Long,
        val fileName: String? = null,
        override val source: AttachmentSource = AttachmentSource.Message
    ) : MessageAttachmentUi {
        val mimeType: String get() = media.mimeType
        val width: Int? get() = media.width
        val height: Int? get() = media.height
        val durationMilliseconds: Long? get() = media.durationMilliseconds

        override val target: AttachmentTarget
            get() =
                AttachmentTarget(
                    id = id,
                    type =
                        when (media.type) {
                            MediaTypeUi.IMAGE -> MessageAttachmentType.IMAGE
                            MediaTypeUi.VIDEO -> MessageAttachmentType.VIDEO
                        },
                    source = source
                )
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
