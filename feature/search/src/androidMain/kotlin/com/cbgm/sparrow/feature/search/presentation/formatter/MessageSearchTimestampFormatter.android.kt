package com.cbgm.sparrow.feature.search.presentation.formatter

import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private val messageSearchTimestampFormatter: DateTimeFormatter =
    DateTimeFormatter.ofPattern("dd.MM.yy HH:mm")

internal actual fun formatMessageSearchTimestamp(epochMilliseconds: Long): String =
    messageSearchTimestampFormatter.format(
        Instant
            .ofEpochMilli(epochMilliseconds)
            .atZone(ZoneId.systemDefault())
    )
