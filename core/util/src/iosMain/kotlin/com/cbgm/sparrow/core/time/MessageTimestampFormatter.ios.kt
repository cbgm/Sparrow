package com.cbgm.sparrow.core.time

import kotlinx.cinterop.ExperimentalForeignApi
import platform.Foundation.NSDate
import platform.Foundation.NSDateFormatter

@OptIn(ExperimentalForeignApi::class)
actual fun formatMessageTimestamp(epochMilliseconds: Long): String {
    val formatter =
        NSDateFormatter().apply {
            dateFormat = "dd.MM.yy HH:mm"
        }
    val date = NSDate.dateWithTimeIntervalSince1970(epochMilliseconds / 1_000.0)
    return formatter.stringFromDate(date)
}
