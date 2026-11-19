package com.cbgm.sparrow.feature.onboarding.presentation.pages.graphic

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp

/**
 * A message leaving "you" wrapped in three layers (rings), passing three relays to "contact"; each relay peels one
 * layer off. Loops while shown.
 */
@Composable
fun OnionGraphic(
    youLabel: String,
    relayLabel: String,
    contactLabel: String,
    modifier: Modifier = Modifier
) {
    val time by rememberInfiniteTransition(label = "onion").animateFloat(
        initialValue = 0f,
        targetValue = CYCLE_MILLISECONDS.toFloat(),
        animationSpec = infiniteRepeatable(tween(CYCLE_MILLISECONDS, easing = LinearEasing), RepeatMode.Restart),
        label = "onionTime"
    )
    val measurer = rememberTextMeasurer()
    val labelStyle = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.outline)
    val labels = listOf(youLabel, relayLabel, relayLabel, relayLabel, contactLabel)
    val path = MaterialTheme.colorScheme.outline
    val relay = MaterialTheme.colorScheme.outline
    val end = MaterialTheme.colorScheme.onBackground
    val packet = MaterialTheme.colorScheme.primary
    Canvas(modifier = modifier) {
        val inset = LABEL_GAP.dp.toPx() + measurer.measure(labels.first(), labelStyle).size.height
        val points = NODES.map { (x, y) -> Offset(x * size.width, inset + y * (size.height - 2 * inset)) }
        val route = Path().apply { points.forEachIndexed { index, point -> if (index == 0) moveTo(point.x, point.y) else lineTo(point.x, point.y) } }
        drawPath(route, path, style = Stroke(width = 1.2.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(4.dp.toPx(), 5.dp.toPx()))))
        points.forEachIndexed { index, point ->
            val isEnd = index == 0 || index == points.lastIndex
            if (isEnd) drawCircle(end, radius = 5.dp.toPx(), center = point) else drawCircle(relay, 4.dp.toPx(), point, style = Stroke(1.3.dp.toPx()))
            val layout = measurer.measure(labels[index], labelStyle)
            val below = point.y > size.height / 2
            val top = if (below) point.y + LABEL_GAP.dp.toPx() else point.y - LABEL_GAP.dp.toPx() - layout.size.height
            drawText(layout, topLeft = Offset(point.x - layout.size.width / 2f, top))
        }
        val hop = minOf(HOPS - 1, (time / HOP_MILLISECONDS).toInt())
        val inHop = time - hop * HOP_MILLISECONDS
        val moved = easeInOut((inHop / MOVE_MILLISECONDS).coerceIn(0f, 1f))
        val at = points[hop] + (points[hop + 1] - points[hop]) * moved
        val peeled = (time / HOP_MILLISECONDS).toInt() + if (inHop >= MOVE_MILLISECONDS) 1 else 0
        LAYER_RADII.forEachIndexed { layer, radius ->
            if (peeled <= layer) drawCircle(packet.copy(alpha = 1f - layer * LAYER_FADE), radius.dp.toPx(), at, style = Stroke(1.4.dp.toPx()))
        }
        drawCircle(packet, 3.5.dp.toPx(), at)
    }
}

private fun easeInOut(value: Float): Float =
    if (value < 0.5f) 2f * value * value else 1f - (-2f * value + 2f) * (-2f * value + 2f) / 2f

/** You, three relays, your contact; x as a fraction of the width, y of the height between the labels. */
private val NODES = listOf(0.09f to 1f, 0.32f to 0.08f, 0.58f to 0.92f, 0.79f to 0f, 0.94f to 0.74f)
private const val LABEL_GAP = 14f
private val LAYER_RADII = listOf(18f, 13f, 8f)
private const val LAYER_FADE = 0.15f
private const val HOPS = 4
private const val MOVE_MILLISECONDS = 1_100f
private const val HOP_MILLISECONDS = 1_450f
private const val CYCLE_MILLISECONDS = 6_700
