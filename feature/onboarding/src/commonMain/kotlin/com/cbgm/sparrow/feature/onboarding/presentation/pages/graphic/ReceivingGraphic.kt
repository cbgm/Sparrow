package com.cbgm.sparrow.feature.onboarding.presentation.pages.graphic

import androidx.compose.animation.animateColorAsState
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
import kotlin.math.PI
import kotlin.math.sin

/**
 * You and a relay. Always connected: a steady line with a pulse running from the relay to you. Only while open: the
 * line is dotted and dim, and a message waits at the relay.
 */
@Composable
fun ReceivingGraphic(
    alwaysConnected: Boolean,
    youLabel: String,
    relayLabel: String,
    modifier: Modifier = Modifier
) {
    val beat by rememberInfiniteTransition(label = "receiving").animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(BEAT_MILLISECONDS, easing = LinearEasing), RepeatMode.Restart),
        label = "receivingBeat"
    )
    val lineColor by animateColorAsState(
        if (alwaysConnected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
        label = "receivingLine"
    )
    val measurer = rememberTextMeasurer()
    val labelStyle = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.outline)
    val you = MaterialTheme.colorScheme.onBackground
    val relay = MaterialTheme.colorScheme.outline
    val message = MaterialTheme.colorScheme.primary
    Canvas(modifier = modifier) {
        val start = Offset(size.width * END_INSET, size.height * NODE_HEIGHT)
        val end = Offset(size.width * (1f - END_INSET), size.height * NODE_HEIGHT)
        val bend = size.height * BEND
        val first = Offset(start.x + (end.x - start.x) / 3f, start.y - bend)
        val second = Offset(start.x + (end.x - start.x) * 2f / 3f, start.y + bend)
        val line = Path().apply {
            moveTo(start.x, start.y)
            cubicTo(first.x, first.y, second.x, second.y, end.x, end.y)
        }
        val dots = if (alwaysConnected) null else PathEffect.dashPathEffect(floatArrayOf(3.dp.toPx(), 6.dp.toPx()))
        drawPath(line, lineColor, style = Stroke(1.4.dp.toPx(), pathEffect = dots))
        drawCircle(you, 6.dp.toPx(), start)
        drawCircle(relay, 5.dp.toPx(), end, style = Stroke(1.4.dp.toPx()))
        listOf(youLabel to start, relayLabel to end).forEach { (label, point) ->
            val layout = measurer.measure(label, labelStyle)
            drawText(layout, topLeft = Offset(point.x - layout.size.width / 2f, point.y + LABEL_GAP.dp.toPx()))
        }
        if (alwaysConnected) {
            val pulse = bezier(end, second, first, start, beat)
            drawCircle(message.copy(alpha = sin(PI * beat).toFloat()), 3.5.dp.toPx(), pulse)
        } else {
            drawCircle(message, 3.5.dp.toPx(), end)
            drawCircle(message.copy(alpha = (1f - beat) * WAIT_RING_ALPHA), (5f + beat * WAIT_RING_GROWTH).dp.toPx(), end, style = Stroke(1.dp.toPx()))
        }
    }
}

private fun bezier(
    start: Offset,
    first: Offset,
    second: Offset,
    end: Offset,
    fraction: Float
): Offset {
    val rest = 1f - fraction
    return start * (rest * rest * rest) + first * (3f * rest * rest * fraction) + second * (3f * rest * fraction * fraction) +
        end * (fraction * fraction * fraction)
}

private const val END_INSET = 0.15f
private const val NODE_HEIGHT = 0.4f
private const val BEND = 0.35f
private const val LABEL_GAP = 12f
private const val WAIT_RING_ALPHA = 0.8f
private const val WAIT_RING_GROWTH = 12f
private const val BEAT_MILLISECONDS = 2_200
