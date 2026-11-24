package com.cbgm.sparrow.core.util.time

/** The local calendar day of an instant, counted in days since 1 January 1970. */
expect fun localEpochDay(epochMilliseconds: Long): Long

/** A day between messages: weekday and date this year, date with year before. */
expect fun formatMessageDay(epochMilliseconds: Long): String

/** A day in the day rail: short weekday and day of month. */
expect fun formatMessageDayOfMonth(epochMilliseconds: Long): String

/** A month in the day rail, with its year when it is not this year. */
expect fun formatMessageMonth(epochMilliseconds: Long): String
