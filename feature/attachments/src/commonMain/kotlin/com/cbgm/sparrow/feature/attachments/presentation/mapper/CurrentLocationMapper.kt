package com.cbgm.sparrow.feature.attachments.presentation.mapper

import com.cbgm.sparrow.core.id.IdGenerator
import com.cbgm.sparrow.feature.attachments.domain.model.CurrentLocation
import com.cbgm.sparrow.feature.attachments.domain.model.OutgoingMessageAttachment
import com.cbgm.sparrow.feature.attachments.util.LocationAttachmentPayload

fun CurrentLocation.toOutgoingMessageAttachment(): OutgoingMessageAttachment =
    OutgoingMessageAttachment.Location(
        id = IdGenerator.generate(prefix = "location"),
        bytes = LocationAttachmentPayload.encode(this)
    )
