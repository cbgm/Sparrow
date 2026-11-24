package com.cbgm.sparrow.feature.chats.presentation.common.history.component

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.layout
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import com.cbgm.sparrow.core.ui.theme.Alpha
import com.cbgm.sparrow.core.ui.theme.Dimens
import com.cbgm.sparrow.core.ui.theme.SparrowTheme
import com.cbgm.sparrow.core.ui.theme.spacing
import com.cbgm.sparrow.core.util.time.SystemClock
import com.cbgm.sparrow.core.util.time.formatMessageDay
import com.cbgm.sparrow.core.util.time.formatMessageDayOfMonth
import com.cbgm.sparrow.core.util.time.formatMessageMonth
import com.cbgm.sparrow.core.util.time.localEpochDay
import com.cbgm.sparrow.feature.chats.presentation.common.history.model.MessageDayUi
import com.cbgm.sparrow.resources.Res
import com.cbgm.sparrow.resources.feature_chats_day_message_count
import org.jetbrains.compose.resources.pluralStringResource
import kotlin.math.abs
import kotlin.math.roundToInt

internal const val MILLISECONDS_PER_DAY = 86_400_000L

/** Where a press on a day separator stands: which day the rail points at and where the finger is. */
@Stable
internal class DayRailState {
    var isOpen by mutableStateOf(false)
        private set
    var selectedEpochDay by mutableStateOf<Long?>(null)
        private set

    /** The finger, in root coordinates, once it has moved; null while it rests or is lifted. */
    var pointerY by mutableStateOf<Float?>(null)
        private set
    var railBounds by mutableStateOf(Rect.Zero)
    val dayBounds = mutableStateMapOf<Long, Rect>()

    private var startY = 0f
    private var touchSlop = 0f

    fun open(epochDay: Long, y: Float, slop: Float) {
        isOpen = true
        selectedEpochDay = epochDay
        pointerY = null
        startY = y
        touchSlop = slop
    }

    fun dragTo(y: Float) {
        if (pointerY == null && abs(y - startY) < touchSlop) return
        pointerY = y
        dayNearest(y)?.let { selectedEpochDay = it }
    }

    /** Lifting after a slide picks the day; lifting without one leaves the rail open to tap a day. */
    fun release(): Long? =
        selectedEpochDay
            .takeIf { pointerY != null }
            ?.also { close() }

    fun close() {
        isOpen = false
        pointerY = null
    }

    private fun dayNearest(y: Float): Long? =
        dayBounds
            .filterValues { it.bottom > railBounds.top && it.top < railBounds.bottom }
            .minByOrNull { (_, bounds) -> abs(bounds.center.y - y) }
            ?.key
}

@Composable
internal fun rememberDayRailState(): DayRailState = remember { DayRailState() }

private sealed interface DayRailEntry {
    data class Month(
        val label: String
    ) : DayRailEntry

    data class Day(
        val day: MessageDayUi
    ) : DayRailEntry
}

/** Every day of the chat, oldest at the top like the chat itself, centred over the dimmed messages; it grows both ways until it fills the height, then scrolls. */
@Composable
internal fun DayRail(
    state: DayRailState,
    days: List<MessageDayUi>,
    today: Long,
    contentPadding: PaddingValues,
    onDayPicked: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val haptics = LocalHapticFeedback.current
    LaunchedEffect(state.selectedEpochDay) {
        if (state.isOpen) haptics.performHapticFeedback(HapticFeedbackType.SegmentFrequentTick)
    }

    AnimatedVisibility(
        visible = state.isOpen,
        enter = fadeIn(),
        exit = fadeOut(),
        modifier = modifier.fillMaxSize()
    ) {
        val entries = remember(days) { days.toRailEntries() }
        val railListState =
            rememberLazyListState(
                initialFirstVisibleItemIndex =
                    (entries.indexOfFirst { it is DayRailEntry.Day && it.day.epochDay == state.selectedEpochDay } - RAIL_LEAD)
                        .coerceAtLeast(0)
            )
        RailAutoScroll(state = state, railListState = railListState)
        var top by remember { mutableFloatStateOf(0f) }

        Box(
            modifier =
                Modifier
                    .fillMaxSize()
                    .onGloballyPositioned { top = it.positionInRoot().y }
                    .background(MaterialTheme.colorScheme.background.copy(alpha = Alpha.MessageList.dayRailScrim))
                    .clickable(interactionSource = null, indication = null, onClick = state::close)
        ) {
            LazyColumn(
                state = railListState,
                reverseLayout = true,
                modifier =
                    Modifier
                        .align(Alignment.CenterEnd)
                        .padding(
                            top = contentPadding.calculateTopPadding() + MaterialTheme.spacing.base,
                            end = MaterialTheme.spacing.messageList.horizontalPadding,
                            bottom = contentPadding.calculateBottomPadding() + MaterialTheme.spacing.base
                        ).width(Dimens.MessageList.dayRailWidth)
                        .background(MaterialTheme.colorScheme.surfaceContainer, MaterialTheme.shapes.large)
                        .onGloballyPositioned { state.railBounds = it.boundsInRoot() },
                contentPadding = PaddingValues(MaterialTheme.spacing.base)
            ) {
                items(
                    items = entries,
                    key = { entry ->
                        when (entry) {
                            is DayRailEntry.Month -> "month-${entry.label}"
                            is DayRailEntry.Day -> entry.day.epochDay
                        }
                    }
                ) { entry ->
                    when (entry) {
                        is DayRailEntry.Month -> RailMonth(label = entry.label)
                        is DayRailEntry.Day ->
                            RailDay(
                                day = entry.day,
                                today = today,
                                isSelected = entry.day.epochDay == state.selectedEpochDay,
                                state = state,
                                onClick = {
                                    state.close()
                                    onDayPicked(entry.day.epochDay)
                                }
                            )
                    }
                }
            }
            val selected = days.firstOrNull { it.epochDay == state.selectedEpochDay }
            val selectedBounds = state.selectedEpochDay?.let(state.dayBounds::get)
            if (selected != null && selectedBounds != null) {
                SelectedDayLabel(
                    day = selected,
                    today = today,
                    modifier =
                        Modifier
                            .align(Alignment.TopEnd)
                            .padding(
                                end = Dimens.MessageList.dayRailWidth + MaterialTheme.spacing.messageList.horizontalPadding +
                                    MaterialTheme.spacing.base
                            ).layout { measurable, constraints ->
                                val placeable = measurable.measure(constraints)
                                layout(placeable.width, placeable.height) {
                                    placeable.place(0, (selectedBounds.center.y - top - placeable.height / 2f).roundToInt())
                                }
                            }
                )
            }
        }
    }
}

/** Keeps the rail moving while the finger rests above or below it. */
@Composable
private fun RailAutoScroll(
    state: DayRailState,
    railListState: LazyListState
) {
    val density = LocalDensity.current
    val edgeZone = with(density) { Dimens.MessageList.dayRailEdgeZone.toPx() }
    val step = with(density) { Dimens.MessageList.dayRailScrollStep.toPx() }

    LaunchedEffect(railListState) {
        while (true) {
            withFrameNanos { }
            val y = state.pointerY ?: continue
            val delta =
                when {
                    y < state.railBounds.top + edgeZone -> step
                    y > state.railBounds.bottom - edgeZone -> -step
                    else -> 0f
                }
            if (delta != 0f && railListState.scrollBy(delta) != 0f) state.dragTo(y)
        }
    }
}

@Composable
private fun RailMonth(label: String) {
    Text(
        text = label.uppercase(),
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(top = MaterialTheme.spacing.small, end = MaterialTheme.spacing.base, bottom = MaterialTheme.spacing.micro),
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.outline,
        textAlign = TextAlign.End
    )
}

@Composable
private fun RailDay(
    day: MessageDayUi,
    today: Long,
    isSelected: Boolean,
    state: DayRailState,
    onClick: () -> Unit
) {
    DisposableEffect(day.epochDay) {
        onDispose { state.dayBounds.remove(day.epochDay) }
    }
    Text(
        text = dayLabel(epochDay = day.epochDay, writtenAt = day.firstMessageAt, today = today, format = ::formatMessageDayOfMonth),
        modifier =
            Modifier
                .fillMaxWidth()
                .onGloballyPositioned { state.dayBounds[day.epochDay] = it.boundsInRoot() }
                .background(
                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainer,
                    shape = MaterialTheme.shapes.large
                ).clickable(onClick = onClick)
                .padding(horizontal = MaterialTheme.spacing.base, vertical = MaterialTheme.spacing.micro),
        style = MaterialTheme.typography.labelLarge,
        fontWeight = if (isSelected) FontWeight.Medium else FontWeight.Normal,
        color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.End
    )
}

@Composable
private fun SelectedDayLabel(
    day: MessageDayUi,
    today: Long,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier, horizontalAlignment = Alignment.End) {
        Text(
            text = dayLabel(epochDay = day.epochDay, writtenAt = day.firstMessageAt, today = today, format = ::formatMessageDay),
            modifier =
                Modifier
                    .background(MaterialTheme.colorScheme.primary, MaterialTheme.shapes.extraLarge)
                    .padding(horizontal = MaterialTheme.spacing.medium, vertical = MaterialTheme.spacing.base),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onPrimary
        )
        Text(
            text = pluralStringResource(Res.plurals.feature_chats_day_message_count, day.messageCount, day.messageCount),
            modifier = Modifier.padding(top = MaterialTheme.spacing.micro, end = MaterialTheme.spacing.base),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/** Newest first, each month's name after its oldest day, so it sits above its days in the reversed rail. */
private fun List<MessageDayUi>.toRailEntries(): List<DayRailEntry> =
    flatMapIndexed { index, day ->
        val month = formatMessageMonth(day.firstMessageAt)
        val nextMonth = getOrNull(index + 1)?.let { formatMessageMonth(it.firstMessageAt) }
        listOfNotNull(DayRailEntry.Day(day), DayRailEntry.Month(month).takeIf { month != nextMonth })
    }

private const val RAIL_LEAD = 4

@Preview(heightDp = 640)
@Composable
private fun DayRailPreview() {
    SparrowTheme {
        val now = SystemClock.nowEpochMilliseconds()
        val days =
            PREVIEW_DAYS_AGO.mapIndexed { index, daysAgo ->
                val writtenAt = now - daysAgo * MILLISECONDS_PER_DAY
                MessageDayUi(
                    epochDay = localEpochDay(writtenAt),
                    firstMessageId = "day-$index",
                    firstMessageAt = writtenAt,
                    messageCount = 3 + index % 5
                )
            }
        val state = rememberDayRailState().apply { open(days[6].epochDay, 0f, 0f) }
        DayRail(
            state = state,
            days = days,
            today = localEpochDay(now),
            contentPadding = PaddingValues(),
            onDayPicked = {}
        )
    }
}

/** Days with messages for previews: this week, the last months and one in an earlier year. */
internal val PREVIEW_DAYS_AGO = listOf(0, 1, 2, 4, 7, 12, 20, 33, 47, 61, 90, 400)
