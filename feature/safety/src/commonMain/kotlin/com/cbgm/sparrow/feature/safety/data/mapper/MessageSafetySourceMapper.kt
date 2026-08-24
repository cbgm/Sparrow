package com.cbgm.sparrow.feature.safety.data.mapper

import com.cbgm.sparrow.data.database.model.MessageSafetySource
import com.cbgm.sparrow.feature.safety.domain.model.MessageSafetyCandidate

internal fun MessageSafetySource.toSafetyCandidate(): MessageSafetyCandidate =
    MessageSafetyCandidate(
        messageId = messageId,
        text = text
    )
