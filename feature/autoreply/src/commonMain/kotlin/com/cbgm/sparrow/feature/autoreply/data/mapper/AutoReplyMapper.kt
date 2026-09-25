package com.cbgm.sparrow.feature.autoreply.data.mapper

import com.cbgm.sparrow.data.database.entity.AutoReplyEntity
import com.cbgm.sparrow.feature.autoreply.domain.model.AutoReply

internal fun AutoReplyEntity.toDomain(): AutoReply =
    AutoReply(
        id = id,
        name = name,
        text = text,
        isActive = isActive,
        activationSessionId = activationSessionId,
        createdAtEpochMilliseconds = createdAtEpochMilliseconds,
        updatedAtEpochMilliseconds = updatedAtEpochMilliseconds
    )

internal fun AutoReply.toEntity(): AutoReplyEntity =
    AutoReplyEntity(
        id = id,
        name = name,
        text = text,
        isActive = isActive,
        activationSessionId = activationSessionId,
        createdAtEpochMilliseconds = createdAtEpochMilliseconds,
        updatedAtEpochMilliseconds = updatedAtEpochMilliseconds
    )
