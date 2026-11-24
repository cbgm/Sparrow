package com.cbgm.sparrow.core.util.time

import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private val messageTimestampFormatter: DateTimeFormatter =
    DateTimeFormatter.ofPattern("dd.MM.yy HH:mm")

private val messageTimeFormatter: DateTimeFormatter =
    DateTimeFormatter.ofPattern("HH:mm")

actual fun formatMessageTimestamp(epochMilliseconds: Long): String =
    messageTimestampFormatter.format(
        Instant
            .ofEpochMilli(epochMilliseconds)
            .atZone(ZoneId.systemDefault())
    )

actual fun formatMessageTime(epochMilliseconds: Long): String =
    messageTimeFormatter.format(
        Instant
            .ofEpochMilli(epochMilliseconds)
            .atZone(ZoneId.systemDefault())
    )
