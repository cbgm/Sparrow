package com.cbgm.sparrow.feature.attachments.presentation.mapper

import com.cbgm.sparrow.core.id.IdGenerator
import com.cbgm.sparrow.core.protocol.attachment.LOCATION_MIME_TYPE
import com.cbgm.sparrow.core.protocol.attachment.MessageAttachmentType
import com.cbgm.sparrow.feature.attachments.domain.model.CurrentLocation
import com.cbgm.sparrow.feature.attachments.domain.model.OutgoingMessageAttachment
import com.cbgm.sparrow.feature.attachments.util.LocationAttachmentPayload

fun CurrentLocation.toOutgoingMessageAttachment(): OutgoingMessageAttachment =
    OutgoingMessageAttachment(
        id = IdGenerator.generate(prefix = "location"),
        type = MessageAttachmentType.LOCATION,
        bytes = LocationAttachmentPayload.encode(this),
        mimeType = LOCATION_MIME_TYPE
    )
