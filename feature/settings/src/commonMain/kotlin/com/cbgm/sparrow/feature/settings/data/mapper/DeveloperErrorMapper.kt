package com.cbgm.sparrow.feature.settings.data.mapper

import com.cbgm.sparrow.feature.settings.data.model.DeveloperErrorDto
import com.cbgm.sparrow.feature.settings.domain.model.DeveloperError

internal fun DeveloperErrorDto.toDeveloperError(): DeveloperError =
    DeveloperError(
        id = id,
        timestampEpochMilliseconds = timestampEpochMilliseconds,
        tag = tag,
        message = message,
        exceptionType = exceptionType,
        stackTrace = stackTrace
    )
