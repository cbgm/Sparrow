package com.cbgm.sparrow.core.time

import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private val messageTimestampFormatter: DateTimeFormatter =
    DateTimeFormatter.ofPattern("dd.MM.yy HH:mm")

actual fun formatMessageTimestamp(epochMilliseconds: Long): String =
    messageTimestampFormatter.format(
        Instant
            .ofEpochMilli(epochMilliseconds)
            .atZone(ZoneId.systemDefault())
    )
