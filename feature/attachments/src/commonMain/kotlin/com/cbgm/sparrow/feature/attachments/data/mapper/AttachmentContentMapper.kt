package com.cbgm.sparrow.feature.attachments.data.mapper

import com.cbgm.sparrow.core.protocol.attachment.MessageAttachmentType
import com.cbgm.sparrow.feature.attachments.data.model.AttachmentContentPayloadDto
import com.cbgm.sparrow.feature.attachments.data.model.AttachmentTargetDto
import com.cbgm.sparrow.feature.attachments.domain.model.AttachmentContent
import com.cbgm.sparrow.feature.attachments.domain.model.AttachmentSource
import com.cbgm.sparrow.feature.attachments.domain.model.AttachmentTarget
import com.cbgm.sparrow.feature.attachments.util.ContactAttachmentPayload
import com.cbgm.sparrow.feature.attachments.util.LocationAttachmentPayload

internal fun AttachmentTarget.toDto(): AttachmentTargetDto =
    AttachmentTargetDto(
        id = id,
        type = type,
        groupId = (source as? AttachmentSource.GroupPin)?.groupId
    )

internal fun AttachmentContentPayloadDto.toDomain(target: AttachmentTarget): AttachmentContent =
    when (this) {
        is AttachmentContentPayloadDto.LocalFile ->
            AttachmentContent.LocalFile(target = target, localFilePath = localFilePath)

        is AttachmentContentPayloadDto.Payload ->
            when (target.type) {
                MessageAttachmentType.LOCATION -> AttachmentContent.Location(
                    target = target,
                    location = requireNotNull(LocationAttachmentPayload.decode(bytes)) {
                        "Location attachment payload is invalid"
                    }
                )

                MessageAttachmentType.CONTACT -> AttachmentContent.Contact(
                    target = target,
                    contact = requireNotNull(ContactAttachmentPayload.decode(bytes)) {
                        "Contact attachment payload is invalid"
                    }
                )

                MessageAttachmentType.IMAGE,
                MessageAttachmentType.VIDEO,
                MessageAttachmentType.FILE,
                MessageAttachmentType.VOICE ->
                    error("Unexpected binary payload for attachment type ${target.type}")
            }
    }
