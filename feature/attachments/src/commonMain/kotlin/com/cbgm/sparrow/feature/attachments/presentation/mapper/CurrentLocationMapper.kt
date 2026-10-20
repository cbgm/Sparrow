package com.cbgm.sparrow.feature.attachments.presentation.mapper

import com.cbgm.sparrow.core.base.messagepart.domain.model.Location
import com.cbgm.sparrow.core.util.id.IdGenerator
import com.cbgm.sparrow.feature.attachments.domain.model.CurrentLocation

fun CurrentLocation.toMessagePart(): Location =
    Location(
        id = IdGenerator.generate(prefix = "location"),
        latitude = latitude,
        longitude = longitude
    )
