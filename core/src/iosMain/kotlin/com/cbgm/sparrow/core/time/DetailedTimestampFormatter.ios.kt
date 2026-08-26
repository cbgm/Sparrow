package com.cbgm.sparrow.core.time

import kotlinx.cinterop.ExperimentalForeignApi
import platform.Foundation.NSDate
import platform.Foundation.NSDateFormatter

@OptIn(ExperimentalForeignApi::class)
actual fun formatDetailedTimestamp(epochMilliseconds: Long): String {
    val formatter =
        NSDateFormatter().apply {
            dateFormat = "dd.MM.yyyy HH:mm:ss.SSS"
        }
    val date = NSDate.dateWithTimeIntervalSince1970(epochMilliseconds / 1_000.0)
    return formatter.stringFromDate(date)
}
