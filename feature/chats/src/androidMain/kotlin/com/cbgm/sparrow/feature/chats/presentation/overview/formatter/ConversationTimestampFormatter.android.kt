package com.cbgm.sparrow.feature.chats.presentation.overview.formatter

import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private val conversationTimeFormatter: DateTimeFormatter =
    DateTimeFormatter.ofPattern("dd.MM.yy HH:mm")

internal actual fun formatConversationTimestamp(epochMilliseconds: Long): String =
    conversationTimeFormatter.format(
        Instant
            .ofEpochMilli(epochMilliseconds)
            .atZone(ZoneId.systemDefault())
    )
