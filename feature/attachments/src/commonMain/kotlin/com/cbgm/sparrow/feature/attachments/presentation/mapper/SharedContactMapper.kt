package com.cbgm.sparrow.feature.attachments.presentation.mapper

import com.cbgm.sparrow.core.id.IdGenerator
import com.cbgm.sparrow.feature.attachments.domain.model.OutgoingMessageAttachment
import com.cbgm.sparrow.feature.attachments.domain.model.SharedContact
import com.cbgm.sparrow.feature.attachments.util.ContactAttachmentPayload

fun SharedContact.toOutgoingMessageAttachment(): OutgoingMessageAttachment =
    OutgoingMessageAttachment.Contact(
        id = IdGenerator.generate(prefix = "contact"),
        bytes = ContactAttachmentPayload.encode(this)
    )
