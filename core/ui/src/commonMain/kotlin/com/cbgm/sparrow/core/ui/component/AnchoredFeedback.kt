package com.cbgm.sparrow.core.ui.component

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.tooling.preview.Preview
import com.cbgm.sparrow.core.ui.helper.darker
import com.cbgm.sparrow.core.ui.theme.Dimens
import com.cbgm.sparrow.core.ui.theme.SparrowTheme
import com.cbgm.sparrow.core.ui.theme.spacing
import kotlinx.coroutines.delay
import kotlin.time.Duration.Companion.milliseconds

private const val FEEDBACK_VISIBLE_DURATION_MILLIS = 1000L

@Immutable
data class FeedbackOverlayData(
    val anchor: SparrowOverlayAnchor,
    val text: String,
    val color: Color
)

@Composable
fun SparrowOverlayScope.FeedbackOverlay(
    text: String,
    color: Color,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val density = LocalDensity.current
    val spacing = MaterialTheme.spacing
    val edgeMarginPx = with(density) { spacing.base.roundToPx() }
    val anchorGapPx = with(density) { spacing.micro.roundToPx() }

    LaunchedEffect(anchorBounds, text) {
        delay(FEEDBACK_VISIBLE_DURATION_MILLIS.milliseconds)
        onDismiss()
    }

    Layout(
        modifier = modifier,
        content = {
            FeedbackBubble(
                text = text,
                color = color
            )
        }
    ) { measurables, constraints ->
        val placeable =
            measurables.single().measure(
                constraints.copy(minWidth = 0, minHeight = 0)
            )

        val maxX =
            (constraints.maxWidth - edgeMarginPx - placeable.width)
                .coerceAtLeast(edgeMarginPx)
        val x =
            (anchorBounds.center.x - placeable.width / 2)
                .coerceIn(edgeMarginPx, maxX)

        val below = anchorBounds.bottom + anchorGapPx
        val above = anchorBounds.top - anchorGapPx - placeable.height
        val desiredY =
            if (below + placeable.height <= constraints.maxHeight - edgeMarginPx) {
                below
            } else {
                above
            }
        val maxY =
            (constraints.maxHeight - edgeMarginPx - placeable.height)
                .coerceAtLeast(edgeMarginPx)
        val y = desiredY.coerceIn(edgeMarginPx, maxY)

        layout(constraints.maxWidth, constraints.maxHeight) {
            placeable.placeRelative(x, y)
        }
    }
}

@Composable
private fun FeedbackBubble(
    text: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = MaterialTheme.shapes.extraSmall,
        color = color.darker(0.9f),
        shadowElevation = Dimens.Card.shadowElevation
    ) {
        Text(
            text = text,
            modifier =
                Modifier.padding(
                    horizontal = MaterialTheme.spacing.base,
                    vertical = MaterialTheme.spacing.micro
                ),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Preview
@Composable
private fun FeedbackBubblePreview() {
    SparrowTheme {
        FeedbackBubble(
            text = "Copied",
            color = MaterialTheme.colorScheme.surface
        )
    }
}
