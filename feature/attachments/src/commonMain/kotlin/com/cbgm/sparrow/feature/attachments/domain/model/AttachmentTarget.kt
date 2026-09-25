package com.cbgm.sparrow.feature.attachments.domain.model

import com.cbgm.sparrow.core.protocol.attachment.MessageAttachmentType

data class AttachmentTarget(
    val id: String,
    val type: MessageAttachmentType,
    val source: AttachmentSource = AttachmentSource.Message
) {
    init {
        require(id.isNotBlank()) { "Attachment ID must not be blank" }
    }
}

sealed interface AttachmentSource {
    data object Message : AttachmentSource

    data class GroupPin(
        val groupId: String
    ) : AttachmentSource {
        init {
            require(groupId.isNotBlank()) { "Group ID must not be blank" }
        }
    }
}
