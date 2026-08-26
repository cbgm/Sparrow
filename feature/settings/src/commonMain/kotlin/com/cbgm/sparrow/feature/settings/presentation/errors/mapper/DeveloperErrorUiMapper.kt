package com.cbgm.sparrow.feature.settings.presentation.errors.mapper

import com.cbgm.sparrow.core.time.formatDetailedTimestamp
import com.cbgm.sparrow.feature.settings.domain.model.DeveloperError
import com.cbgm.sparrow.feature.settings.presentation.errors.model.DeveloperErrorUi

internal fun DeveloperError.toUiModel(): DeveloperErrorUi =
    DeveloperErrorUi(
        id = id,
        timestamp = formatDetailedTimestamp(timestampEpochMilliseconds),
        tag = tag,
        message = message,
        exceptionType = exceptionType,
        stackTrace = stackTrace
    )
