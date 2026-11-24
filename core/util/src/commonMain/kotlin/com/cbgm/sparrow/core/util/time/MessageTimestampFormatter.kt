package com.cbgm.sparrow.core.util.time

expect fun formatMessageTimestamp(epochMilliseconds: Long): String

/** Only the time of day, for a message inside its chat. */
expect fun formatMessageTime(epochMilliseconds: Long): String
