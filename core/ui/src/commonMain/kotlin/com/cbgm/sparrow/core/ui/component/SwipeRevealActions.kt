package com.cbgm.sparrow.core.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import com.cbgm.sparrow.core.ui.theme.Dimens
import com.cbgm.sparrow.core.ui.theme.SparrowTheme
import com.cbgm.sparrow.core.ui.theme.spacing
import kotlin.math.roundToInt

private val defaultSwipeActionWidth = Dimens.SwipeRevealActions.actionWidth
private const val DEFAULT_REVEAL_THRESHOLD_FRACTION = 0.5f

/** What a swipe action means: it only picks the icon colour, the background is always the quiet note dark. */
enum class SwipeActionTone {
    NEUTRAL,
    PRIMARY,
    DESTRUCTIVE
}

data class SwipeRevealAction(
    val tone: SwipeActionTone = SwipeActionTone.NEUTRAL,
    val onClick: () -> Unit,
    val content: @Composable () -> Unit
)

@Composable
fun SparrowSwipeRevealItem(
    actions: List<SwipeRevealAction>,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    actionWidth: Dp = defaultSwipeActionWidth,
    revealThresholdFraction: Float = DEFAULT_REVEAL_THRESHOLD_FRACTION,
    content: @Composable () -> Unit
) {
    require(revealThresholdFraction in 0f..1f) {
        "revealThresholdFraction must be between 0 and 1"
    }

    if (actions.isEmpty()) {
        Box(modifier = modifier.fillMaxWidth()) {
            content()
        }
        return
    }

    val density = LocalDensity.current
    val revealWidth = actionWidth * actions.size
    val revealWidthPx = with(density) { revealWidth.toPx() }
    var offset by remember { mutableFloatStateOf(0f) }

    LaunchedEffect(enabled, revealWidthPx) {
        offset =
            if (enabled) {
                offset.coerceIn(-revealWidthPx, 0f)
            } else {
                0f
            }
    }

    Box(modifier = modifier.fillMaxWidth().clipToBounds()) {
        Row(
            modifier = Modifier.matchParentSize(),
            horizontalArrangement = Arrangement.End
        ) {
            actions.forEach { action ->
                SwipeRevealActionItem(
                    action = action,
                    width = actionWidth,
                    enabled = enabled,
                    onClick = {
                        offset = 0f
                        action.onClick()
                    }
                )
            }
        }

        Box(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .offset { IntOffset(offset.roundToInt(), 0) }
                    .background(MaterialTheme.colorScheme.background)
                    .draggable(
                        enabled = enabled,
                        orientation = Orientation.Horizontal,
                        state =
                            rememberDraggableState { delta ->
                                offset = (offset + delta).coerceIn(-revealWidthPx, 0f)
                            },
                        onDragStopped = {
                            offset =
                                if (offset <= -revealWidthPx * revealThresholdFraction) {
                                    -revealWidthPx
                                } else {
                                    0f
                                }
                        }
                    )
        ) {
            content()
        }
    }
}

@Composable
private fun SwipeRevealActionItem(
    action: SwipeRevealAction,
    width: Dp,
    enabled: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier =
            Modifier
                .width(width)
                .fillMaxHeight()
                .background(MaterialTheme.colorScheme.surfaceContainerLow)
                .clickable(
                    enabled = enabled,
                    onClick = onClick
                ),
        contentAlignment = Alignment.Center
    ) {
        val tint =
            when (action.tone) {
                SwipeActionTone.NEUTRAL -> MaterialTheme.colorScheme.onSurfaceVariant
                SwipeActionTone.PRIMARY -> MaterialTheme.colorScheme.primary
                SwipeActionTone.DESTRUCTIVE -> MaterialTheme.colorScheme.error
            }
        CompositionLocalProvider(LocalContentColor provides tint) {
            action.content()
        }
    }
}

@Preview
@Composable
private fun SparrowSwipeRevealItemPreview() {
    SparrowTheme {
        SparrowSwipeRevealItem(
            actions =
                listOf(
                    SwipeRevealAction(
                        tone = SwipeActionTone.NEUTRAL,
                        onClick = {}
                    ) {
                        Text("A")
                    },
                    SwipeRevealAction(
                        tone = SwipeActionTone.NEUTRAL,
                        onClick = {}
                    ) {
                        Text("B")
                    },
                    SwipeRevealAction(
                        tone = SwipeActionTone.DESTRUCTIVE,
                        onClick = {}
                    ) {
                        Text("C")
                    }
                )
        ) {
            Box(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.background)
                        .padding(MaterialTheme.spacing.medium)
            ) {
                Text("Swipe me")
            }
        }
    }
}
