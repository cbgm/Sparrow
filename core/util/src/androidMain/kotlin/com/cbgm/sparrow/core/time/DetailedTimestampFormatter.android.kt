package com.cbgm.sparrow.core.time

import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private val detailedTimestampFormatter: DateTimeFormatter =
    DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm:ss.SSS")

actual fun formatDetailedTimestamp(epochMilliseconds: Long): String =
    detailedTimestampFormatter.format(
        Instant
            .ofEpochMilli(epochMilliseconds)
            .atZone(ZoneId.systemDefault())
    )
