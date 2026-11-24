package com.cbgm.sparrow.core.ui.component

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import com.cbgm.sparrow.core.ui.theme.Dimens

/** Where a message is on its way; the dots show it instead of words. */
enum class DeliveryDotsState {
    /** Waiting for a route or the next try: hollow, the first one pulsing slowly. */
    QUEUED,

    /** Going out right now: the first dot pulses. */
    SENDING,
    SENT,
    DELIVERED,
    READ,

    /** Red, with the path broken between the dots. */
    FAILED
}

/** Three stops on a path: sent, delivered, read. The stops reached so far are cyan. */
@Composable
fun SparrowDeliveryDots(
    state: DeliveryDotsState,
    description: String,
    modifier: Modifier = Modifier
) {
    val on = MaterialTheme.colorScheme.primary
    val off = MaterialTheme.colorScheme.outline
    val failed = MaterialTheme.colorScheme.error
    val pulse = rememberPulse(state)
    Canvas(
        modifier = modifier
            .size(width = Dimens.MessageBubble.deliveryDotsWidth, height = Dimens.MessageBubble.deliveryDotsHeight)
            .semantics { contentDescription = description }
    ) {
        val radius = Dimens.MessageBubble.deliveryDotRadius.toPx()
        val stroke = Dimens.MessageBubble.deliveryLineWidth.toPx()
        val centerY = size.height / 2f
        val step = (size.width - radius * 2f) / 2f
        val stops = List(3) { index -> Offset(radius + step * index, centerY) }
        when (state) {
            DeliveryDotsState.FAILED -> drawBroken(stops, radius, stroke, failed)
            DeliveryDotsState.QUEUED -> drawHollow(stops, radius, stroke, off, pulse.value)
            else -> drawReached(stops, radius, stroke, state, on, off, pulse.value)
        }
    }
}

/** A gap before every stop but the first: the path broke. */
private fun DrawScope.drawBroken(
    stops: List<Offset>,
    radius: Float,
    stroke: Float,
    color: Color
) {
    val gap = Offset(radius * 2f, 0f)
    stops.zipWithNext { from, to -> drawLine(color, from + gap, to - gap, stroke) }
    stops.forEach { center -> drawCircle(color, radius, center) }
}

private fun DrawScope.drawHollow(
    stops: List<Offset>,
    radius: Float,
    stroke: Float,
    color: Color,
    pulse: Float
) {
    drawLine(color.copy(alpha = PATH_ALPHA), stops.first(), stops.last(), stroke)
    stops.forEachIndexed { index, center ->
        drawCircle(color.copy(alpha = pulse.takeIf { index == 0 } ?: 1f), radius, center, style = Stroke(stroke))
    }
}

private fun DrawScope.drawReached(
    stops: List<Offset>,
    radius: Float,
    stroke: Float,
    state: DeliveryDotsState,
    on: Color,
    off: Color,
    pulse: Float
) {
    val reached = state.reached()
    drawLine(off.copy(alpha = PATH_ALPHA), stops.first(), stops.last(), stroke)
    drawLine(on, stops.first(), stops[reached - 1], stroke)
    stops.forEachIndexed { index, center ->
        drawCircle(on.takeIf { index < reached } ?: off.copy(alpha = OFF_ALPHA), radius, center)
    }
    // Sending: a ring grows from the first stop and fades.
    if (state == DeliveryDotsState.SENDING) {
        drawCircle(on.copy(alpha = 1f - pulse), radius * (1f + pulse), stops.first(), style = Stroke(stroke))
    }
}

private fun DeliveryDotsState.reached(): Int =
    when (this) {
        DeliveryDotsState.SENDING, DeliveryDotsState.SENT -> 1
        DeliveryDotsState.DELIVERED -> 2
        DeliveryDotsState.READ -> 3
        DeliveryDotsState.QUEUED, DeliveryDotsState.FAILED -> 0
    }

/** 0 to 1 and back while sending or queued (slower); a still value otherwise, so no animation runs for every message. */
@Composable
private fun rememberPulse(state: DeliveryDotsState): State<Float> {
    val millis =
        when (state) {
            DeliveryDotsState.SENDING -> SENDING_PULSE_MILLISECONDS
            DeliveryDotsState.QUEUED -> QUEUED_PULSE_MILLISECONDS
            else -> return remember { mutableFloatStateOf(1f) }
        }
    return rememberInfiniteTransition(label = "deliveryPulse").animateFloat(
        initialValue = PULSE_LOW,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(millis, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "deliveryPulseValue"
    )
}

private const val PATH_ALPHA = 0.35f
private const val OFF_ALPHA = 0.55f
private const val PULSE_LOW = 0.25f
private const val SENDING_PULSE_MILLISECONDS = 700
private const val QUEUED_PULSE_MILLISECONDS = 1_400
