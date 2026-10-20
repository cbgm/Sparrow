package com.cbgm.sparrow.core.util.time

import kotlinx.cinterop.ExperimentalForeignApi
import platform.Foundation.NSDate
import platform.Foundation.NSDateFormatter

@OptIn(ExperimentalForeignApi::class)
actual fun formatDate(epochMilliseconds: Long): String {
    val formatter =
        NSDateFormatter().apply {
            dateFormat = "dd.MM.yy"
        }
    val date = NSDate.dateWithTimeIntervalSince1970(epochMilliseconds / 1_000.0)
    return formatter.stringFromDate(date)
}
