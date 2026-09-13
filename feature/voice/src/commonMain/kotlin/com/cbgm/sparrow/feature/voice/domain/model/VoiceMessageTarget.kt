package com.cbgm.sparrow.feature.voice.domain.model

import com.cbgm.sparrow.core.protocol.attachment.MessageAttachmentType
import com.cbgm.sparrow.feature.attachments.domain.model.AttachmentSource
import com.cbgm.sparrow.feature.attachments.domain.model.AttachmentTarget

data class VoiceMessageTarget(
    val attachmentId: String,
    val durationMilliseconds: Long,
    val source: AttachmentSource = AttachmentSource.Message
) {
    init {
        require(attachmentId.isNotBlank()) { "Voice attachment ID must not be blank" }
        require(durationMilliseconds >= 0L) { "Voice duration must not be negative" }
    }

    val stableKey: String
        get() =
            when (val attachmentSource = source) {
                AttachmentSource.Message -> "voice:message:$attachmentId"
                is AttachmentSource.GroupPin -> "voice:group-pin:${attachmentSource.groupId}:$attachmentId"
            }

    val attachmentTarget: AttachmentTarget
        get() =
            AttachmentTarget(
                id = attachmentId,
                type = MessageAttachmentType.VOICE,
                source = source
            )
}
