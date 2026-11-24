package com.cbgm.sparrow.core.util.time

import kotlinx.cinterop.ExperimentalForeignApi
import platform.Foundation.NSCalendar
import platform.Foundation.NSCalendarUnitYear
import platform.Foundation.NSDate
import platform.Foundation.NSDateFormatter
import platform.Foundation.NSTimeZone
import platform.Foundation.localTimeZone

private const val MILLISECONDS_PER_DAY = 86_400_000L

@OptIn(ExperimentalForeignApi::class)
actual fun localEpochDay(epochMilliseconds: Long): Long {
    val offsetMilliseconds = NSTimeZone.localTimeZone.secondsFromGMTForDate(date(epochMilliseconds)) * 1_000L
    return (epochMilliseconds + offsetMilliseconds).floorDiv(MILLISECONDS_PER_DAY)
}

actual fun formatMessageDay(epochMilliseconds: Long): String =
    format(epochMilliseconds, template = "EEEdMMM", templateWithYear = "dMMMy")

actual fun formatMessageDayOfMonth(epochMilliseconds: Long): String =
    format(epochMilliseconds, template = "EEEd", templateWithYear = "EEEd")

actual fun formatMessageMonth(epochMilliseconds: Long): String =
    format(epochMilliseconds, template = "MMM", templateWithYear = "MMMy")

@OptIn(ExperimentalForeignApi::class)
private fun date(epochMilliseconds: Long): NSDate = NSDate.dateWithTimeIntervalSince1970(epochMilliseconds / 1_000.0)

@OptIn(ExperimentalForeignApi::class)
private fun format(
    epochMilliseconds: Long,
    template: String,
    templateWithYear: String
): String {
    val date = date(epochMilliseconds)
    val calendar = NSCalendar.currentCalendar
    val thisYear = calendar.component(NSCalendarUnitYear, fromDate = NSDate()) == calendar.component(NSCalendarUnitYear, fromDate = date)
    val formatter = NSDateFormatter().apply { setLocalizedDateFormatFromTemplate(if (thisYear) template else templateWithYear) }
    return formatter.stringFromDate(date)
}
