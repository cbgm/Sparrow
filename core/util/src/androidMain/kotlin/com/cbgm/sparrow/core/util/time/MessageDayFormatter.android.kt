package com.cbgm.sparrow.core.util.time

import android.text.format.DateFormat
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

actual fun localEpochDay(epochMilliseconds: Long): Long = zoned(epochMilliseconds).toLocalDate().toEpochDay()

actual fun formatMessageDay(epochMilliseconds: Long): String =
    format(epochMilliseconds, skeleton = "EEEdMMM", skeletonWithYear = "dMMMy")

actual fun formatMessageDayOfMonth(epochMilliseconds: Long): String =
    format(epochMilliseconds, skeleton = "EEEd", skeletonWithYear = "EEEd")

actual fun formatMessageMonth(epochMilliseconds: Long): String =
    format(epochMilliseconds, skeleton = "MMM", skeletonWithYear = "MMMy")

private fun zoned(epochMilliseconds: Long): ZonedDateTime =
    Instant
        .ofEpochMilli(epochMilliseconds)
        .atZone(ZoneId.systemDefault())

private fun format(
    epochMilliseconds: Long,
    skeleton: String,
    skeletonWithYear: String
): String {
    val date = zoned(epochMilliseconds)
    val locale = Locale.getDefault()
    val pattern =
        DateFormat.getBestDateTimePattern(
            locale,
            if (date.year == LocalDate.now().year) skeleton else skeletonWithYear
        )
    return DateTimeFormatter.ofPattern(pattern, locale).format(date)
}
