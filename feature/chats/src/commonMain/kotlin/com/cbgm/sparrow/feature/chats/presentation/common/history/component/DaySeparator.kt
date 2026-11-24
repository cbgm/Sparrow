package com.cbgm.sparrow.feature.chats.presentation.common.history.component

import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.tooling.preview.Preview
import com.cbgm.sparrow.core.ui.theme.Dimens
import com.cbgm.sparrow.core.ui.theme.SparrowTheme
import com.cbgm.sparrow.core.ui.theme.divider
import com.cbgm.sparrow.core.ui.theme.spacing
import com.cbgm.sparrow.core.util.time.SystemClock
import com.cbgm.sparrow.core.util.time.formatMessageDay
import com.cbgm.sparrow.core.util.time.localEpochDay
import com.cbgm.sparrow.resources.Res
import com.cbgm.sparrow.resources.feature_chats_day_today
import com.cbgm.sparrow.resources.feature_chats_day_yesterday
import org.jetbrains.compose.resources.stringResource

/** The date above the first message of a day. Holding it opens the day rail; sliding picks a day. */
@Composable
internal fun DaySeparator(
    writtenAt: Long,
    today: Long,
    railState: DayRailState,
    onDayPicked: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val epochDay = remember(writtenAt) { localEpochDay(writtenAt) }
    var top by remember { mutableFloatStateOf(0f) }

    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .padding(top = MaterialTheme.spacing.base)
                .onGloballyPositioned { top = it.positionInRoot().y }
                .pointerInput(epochDay) {
                    detectDragGesturesAfterLongPress(
                        onDragStart = { offset -> railState.open(epochDay, top + offset.y, viewConfiguration.touchSlop) },
                        onDrag = { change, _ ->
                            change.consume()
                            railState.dragTo(top + change.position.y)
                        },
                        onDragEnd = { railState.release()?.let(onDayPicked) },
                        onDragCancel = railState::close
                    )
                },
        horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.small),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Hairline(modifier = Modifier.weight(1f))
        Text(
            text = dayLabel(epochDay = epochDay, writtenAt = writtenAt, today = today, format = ::formatMessageDay),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.outline
        )
        Hairline(modifier = Modifier.weight(1f))
    }
}

@Composable
private fun Hairline(modifier: Modifier = Modifier) {
    HorizontalDivider(
        modifier = modifier,
        thickness = Dimens.Base.dividerThickness,
        color = MaterialTheme.colorScheme.divider
    )
}

/** Today and yesterday by name, any other day as [format] writes it. */
@Composable
internal fun dayLabel(
    epochDay: Long,
    writtenAt: Long,
    today: Long,
    format: (Long) -> String
): String =
    when (epochDay) {
        today -> stringResource(Res.string.feature_chats_day_today)
        today - 1 -> stringResource(Res.string.feature_chats_day_yesterday)
        else -> format(writtenAt)
    }

@Preview
@Composable
private fun DaySeparatorPreview() {
    SparrowTheme {
        val now = SystemClock.nowEpochMilliseconds()
        DaySeparator(
            writtenAt = now - 9 * MILLISECONDS_PER_DAY,
            today = localEpochDay(now),
            railState = rememberDayRailState(),
            onDayPicked = {}
        )
    }
}
