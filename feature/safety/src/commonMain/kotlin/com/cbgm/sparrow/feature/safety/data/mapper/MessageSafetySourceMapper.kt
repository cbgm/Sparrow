package com.cbgm.sparrow.feature.safety.data.mapper

import com.cbgm.sparrow.data.database.model.MessageSafetySourceDto
import com.cbgm.sparrow.feature.safety.domain.model.MessageSafetyCandidate

internal fun MessageSafetySourceDto.toMessageSafetyCandidate(): MessageSafetyCandidate =
    MessageSafetyCandidate(
        messageId = messageId,
        text = text
    )
