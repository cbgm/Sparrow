package com.cbgm.sparrow.feature.attachments.presentation.mapper

import com.cbgm.sparrow.core.id.IdGenerator
import com.cbgm.sparrow.core.protocol.attachment.CONTACT_MIME_TYPE
import com.cbgm.sparrow.core.protocol.attachment.MessageAttachmentType
import com.cbgm.sparrow.feature.attachments.domain.model.OutgoingMessageAttachment
import com.cbgm.sparrow.feature.attachments.domain.model.SharedContact
import com.cbgm.sparrow.feature.attachments.util.ContactAttachmentPayload

fun SharedContact.toOutgoingMessageAttachment(): OutgoingMessageAttachment =
    OutgoingMessageAttachment(
        id = IdGenerator.generate(prefix = "contact"),
        type = MessageAttachmentType.CONTACT,
        bytes = ContactAttachmentPayload.encode(this),
        mimeType = CONTACT_MIME_TYPE
    )
