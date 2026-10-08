package com.cbgm.sparrow.feature.polls.presentation.component

import androidx.compose.runtime.Composable
import com.cbgm.sparrow.resources.Res
import com.cbgm.sparrow.resources.feature_polls_days
import com.cbgm.sparrow.resources.feature_polls_hours
import com.cbgm.sparrow.resources.feature_polls_minutes
import org.jetbrains.compose.resources.pluralStringResource

@Composable
fun pollDurationLabel(totalMinutes: Long): String {
    val days = totalMinutes / MINUTES_PER_DAY
    val hours = (totalMinutes % MINUTES_PER_DAY) / MINUTES_PER_HOUR
    val minutes = totalMinutes % MINUTES_PER_HOUR

    val parts = mutableListOf<String>()
    if (days > 0L) {
        parts += pluralStringResource(Res.plurals.feature_polls_days, days.toInt(), days)
    }
    if (hours > 0L) {
        parts += pluralStringResource(Res.plurals.feature_polls_hours, hours.toInt(), hours)
    }
    if (minutes > 0L || parts.isEmpty()) {
        parts += pluralStringResource(Res.plurals.feature_polls_minutes, minutes.toInt(), minutes)
    }
    return parts.joinToString(" ")
}

private const val MINUTES_PER_HOUR = 60L
private const val MINUTES_PER_DAY = 24L * MINUTES_PER_HOUR
