package com.cbgm.sparrow.core.ui.component

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.lerp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.cbgm.sparrow.core.ui.theme.SparrowTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sin
import kotlin.time.Duration.Companion.milliseconds

private const val VIEWPORT = 1000f
private const val TOTAL_MILLIS = 2400
private const val PULSE_MILLIS = 2600
private const val PULSE_RING_MILLIS = 1500
private val Center = Offset(500f, 500f)
private val HeadAnchor = Offset(532f, 374f)
private val ShieldCenter = Offset(500f, 600f)

/**
 * The Sparrow mark in the line style of the onboarding graphics. Animated, a sparrow flies in, holds in the air and swings
 * its wings down into the two halves of the shield; the sparrow's head stays on top, its neck running into the shield's
 * top edge. After landing, the ring of the impact repeats every [PULSE_MILLIS] for as long as the mark is shown, with the
 * shield bumping slightly each time. Without animation it shows the finished mark, still; the splash icon
 * (ic_sparrow_splash) is the same mark, filled.
 *
 * All shapes live in a 1000 × 1000 space. Each wing is one outline of eight cubic segments whose points move from the
 * flying wing to the shield half; the eighth segment is the body edge, which fades out instead of drawing a centre line.
 */
@Composable
fun SparrowAnimation(
    modifier: Modifier = Modifier,
    animated: Boolean = false,
    sendAnimationTrigger: Int = 0
) {
    val primary = MaterialTheme.colorScheme.primary
    val background = MaterialTheme.colorScheme.background
    val shapes = remember { SparrowShapes() }

    val time = remember { Animatable(if (animated) 0f else TOTAL_MILLIS.toFloat()) }
    val pulse = remember { Animatable(0f) }
    val sendOffset = remember { Animatable(0f) }
    val sendScale = remember { Animatable(1f) }
    val sendRotation = remember { Animatable(0f) }

    LaunchedEffect(animated) {
        if (animated) {
            time.snapTo(0f)
            time.animateTo(TOTAL_MILLIS.toFloat(), tween(TOTAL_MILLIS, easing = LinearEasing))
            delay(FIRST_PULSE_DELAY_MILLIS.milliseconds)
            while (true) {
                pulse.snapTo(0f)
                pulse.animateTo(PULSE_MILLIS.toFloat(), tween(PULSE_MILLIS, easing = LinearEasing))
            }
        } else {
            time.snapTo(TOTAL_MILLIS.toFloat())
            pulse.snapTo(0f)
        }
    }

    LaunchedEffect(sendAnimationTrigger) {
        if (sendAnimationTrigger == 0) return@LaunchedEffect

        launch {
            sendScale.snapTo(1f)
            sendScale.animateTo(0.94f, tween(90))
            sendScale.animateTo(1.02f, tween(110))
            sendScale.animateTo(1f, tween(140))
        }

        launch {
            sendRotation.snapTo(0f)
            sendRotation.animateTo(-2f, tween(100))
            sendRotation.animateTo(0f, tween(180))
        }

        sendOffset.snapTo(0f)
        sendOffset.animateTo(14f, tween(140))
        sendOffset.animateTo(0f, tween(200))
    }

    Canvas(modifier = modifier.aspectRatio(1f)) {
        val frame = SparrowFrame.at(time.value)
        val pulseRing = pulseRing(pulse.value)
        val unit = min(size.width, size.height) / VIEWPORT

        translate(
            left = (size.width - VIEWPORT * unit) / 2f + sendOffset.value,
            top = (size.height - VIEWPORT * unit) / 2f
        ) {
            scale(unit, pivot = Offset.Zero) {
                scale(sendScale.value, pivot = Center) {
                    rotate(sendRotation.value, pivot = Center) {
                        scale(pulseBump(pulseRing), pivot = ShieldCenter) {
                            drawFlyingMark(frame, shapes, primary, background)
                        }
                        drawRing(frame.ring, shapes, primary)
                        drawRing(pulseRing, shapes, primary)
                    }
                }
            }
        }
    }
}

private fun DrawScope.drawFlyingMark(
    frame: SparrowFrame,
    shapes: SparrowShapes,
    primary: Color,
    background: Color
) {
    val alpha = frame.visibility
    if (alpha <= 0f) return

    val line = Stroke(width = LINE_WIDTH, cap = StrokeCap.Round, join = StrokeJoin.Round)
    val segments = wingSegments(frame.morph, frame.flap)
    val bodyEdgeAlpha = 1f - (frame.morph * 1.6f).coerceIn(0f, 1f)

    translate(frame.offsetX, frame.offsetY) {
        rotate(frame.rotation, pivot = Center) {
            scale(frame.scale, pivot = Center) {
                drawPath(wingPath(segments, mirrored = false, from = 0, to = 7), primary, alpha, line)
                drawPath(wingPath(segments, mirrored = true, from = 0, to = 7), primary, alpha, line)
                drawPath(wingPath(segments, mirrored = false, from = 7, to = 8), primary, alpha * bodyEdgeAlpha, line)
                drawPath(wingPath(segments, mirrored = true, from = 7, to = 8), primary, alpha * bodyEdgeAlpha, line)
                drawPath(shapes.innerShield, primary, alpha * frame.inner * INNER_ALPHA, Stroke(width = INNER_WIDTH))
                drawLineHead(frame, shapes, primary, background, alpha)
            }
        }
    }
}

/** The head is cut off where it meets the wings (in flight) or the shield's top edge, so the neck runs into them. */
private fun DrawScope.drawLineHead(
    frame: SparrowFrame,
    shapes: SparrowShapes,
    primary: Color,
    background: Color,
    alpha: Float
) {
    val headScale = lerp(0.8f, 1.1f, frame.morph)
    val headAt = Offset(HEAD_X, lerp(402f, 372f, frame.morph))

    head(headAt, headScale) { drawPath(shapes.head, background, alpha) }
    clipPath(neckCut(frame.morph)) {
        head(headAt, headScale) {
            drawPath(shapes.head, primary, alpha, Stroke(width = LINE_WIDTH / headScale, join = StrokeJoin.Round))
            drawPath(shapes.eye, primary, alpha)
            drawPath(shapes.beakLine, primary, alpha * DETAIL_ALPHA, Stroke(DETAIL_WIDTH / headScale, cap = StrokeCap.Round))
        }
    }
}

/** A ring around the shield that widens and fades as [progress] runs from 0 to 1; nothing outside that range. */
private fun DrawScope.drawRing(
    progress: Float,
    shapes: SparrowShapes,
    primary: Color
) {
    if (progress <= 0f || progress >= 1f) return

    scale(1.04f + progress * 0.16f, pivot = ShieldCenter) {
        drawPath(shapes.ring, primary, (1f - progress) * RING_ALPHA, Stroke(width = RING_WIDTH))
    }
}

/** Progress of the repeating ring, [millis] into a pulse; 0 once the ring has faded until the next pulse starts. */
private fun pulseRing(millis: Float): Float = (millis / PULSE_RING_MILLIS).coerceIn(0f, 1f).takeIf { it < 1f } ?: 0f

/** The shield's small bump while a pulse ring leaves it: up quickly, then back. */
private fun pulseBump(ring: Float): Float {
    val bump = if (ring < PULSE_BUMP_RISE) ring / PULSE_BUMP_RISE else (1f - (ring - PULSE_BUMP_RISE) / PULSE_BUMP_FALL).coerceAtLeast(0f)
    return 1f + PULSE_BUMP * bump
}

private inline fun DrawScope.head(
    at: Offset,
    headScale: Float,
    block: DrawScope.() -> Unit
) {
    translate(at.x - HeadAnchor.x, at.y - HeadAnchor.y) {
        scale(headScale, pivot = HeadAnchor) { block() }
    }
}

/** Everything above the line where the head meets the body: straight across the shoulders in flight, the shield's top edge at the end. */
private fun neckCut(morph: Float): Path {
    fun y(shield: Float) = lerp(396f, shield, morph)
    return Path().apply {
        moveTo(0f, 0f)
        lineTo(VIEWPORT, 0f)
        lineTo(VIEWPORT, y(330f))
        lineTo(750f, y(330f))
        cubicTo(685f, y(352f), 587.5f, y(364f), 500f, y(362f))
        cubicTo(412.5f, y(364f), 315f, y(352f), 250f, y(330f))
        lineTo(0f, y(330f))
        close()
    }
}

private fun wingSegments(
    morph: Float,
    flap: Float
): List<List<Offset>> {
    val shoulder = FlightCorners.first()
    val corners =
        FlightCorners.mapIndexed { index, corner ->
            corner.rotatedAround(shoulder, degrees = flap * FLAP_DEGREES * if (index == 0 || index == 7) 0f else 1f)
        }
    return List(FlightCorners.size) { index ->
        val start = corners[index]
        val end = corners[(index + 1) % corners.size]
        val flight = listOf(start) + bowedControls(start, end, FlightBows[index]) + end
        flight.mapIndexed { point, flying -> lerp(flying, ShieldSegments[index][point], morph) }
    }
}

private fun wingPath(
    segments: List<List<Offset>>,
    mirrored: Boolean,
    from: Int,
    to: Int
): Path {
    fun Offset.side() = if (mirrored) Offset(VIEWPORT - x, y) else this
    return Path().apply {
        val start = segments[from][0].side()
        moveTo(start.x, start.y)
        segments.subList(from, to).forEach { segment ->
            val first = segment[1].side()
            val second = segment[2].side()
            val end = segment[3].side()
            cubicTo(first.x, first.y, second.x, second.y, end.x, end.y)
        }
    }
}

private fun bowedControls(
    start: Offset,
    end: Offset,
    bow: Float
): List<Offset> {
    val dx = end.x - start.x
    val dy = end.y - start.y
    val normal = Offset(dy * bow, -dx * bow)
    return listOf(Offset(start.x + dx / 3f, start.y + dy / 3f) + normal, Offset(start.x + dx * 2f / 3f, start.y + dy * 2f / 3f) + normal)
}

private fun Offset.rotatedAround(
    center: Offset,
    degrees: Float
): Offset {
    val radians = degrees * PI.toFloat() / 180f
    val dx = x - center.x
    val dy = y - center.y
    return Offset(center.x + dx * cos(radians) - dy * sin(radians), center.y + dx * sin(radians) + dy * cos(radians))
}

/** Where everything stands [millis] into the animation. */
private class SparrowFrame(
    val visibility: Float,
    val offsetX: Float,
    val offsetY: Float,
    val rotation: Float,
    val scale: Float,
    val morph: Float,
    val flap: Float,
    val inner: Float,
    val ring: Float
) {
    companion object {
        fun at(millis: Float): SparrowFrame {
            val entrance = easeOut(progress(millis, 0f, 750f))
            val morph = easeInOut(progress(millis, 950f, 600f))
            val impactTime = progress(millis, 1500f, 280f)
            val impact = if (impactTime < 0.3f) impactTime / 0.3f else 1f - (impactTime - 0.3f) / 0.7f
            val flapStrength = 1f - progress(millis, 750f, 200f) * 0.8f
            return SparrowFrame(
                visibility = progress(millis, 0f, 160f),
                offsetX = lerp(-460f, 0f, entrance),
                offsetY = lerp(-260f, 0f, entrance),
                rotation = lerp(-14f, 0f, entrance),
                scale = lerp(0.55f, 1f, entrance) * (1f - impact * 0.03f),
                morph = morph,
                flap = sin(millis / 1000f * 2f * PI.toFloat() * FLAP_HERTZ) * flapStrength,
                inner = ((morph - 0.7f) / 0.3f).coerceIn(0f, 1f),
                ring = progress(millis, 1520f, 700f)
            )
        }

        private fun progress(
            millis: Float,
            start: Float,
            duration: Float
        ): Float = ((millis - start) / duration).coerceIn(0f, 1f)

        private fun easeOut(t: Float): Float = 1f - (1f - t).pow(3)

        private fun easeInOut(t: Float): Float = if (t < 0.5f) 4f * t * t * t else 1f - (-2f * t + 2f).pow(3) / 2f
    }
}

private class SparrowShapes {
    val head = path(HEAD)
    val eye = path(EYE)
    val beakLine = path(BEAK_LINE)
    val innerShield = path(INNER_SHIELD)
    val ring = path(RING)

    private fun path(data: String): Path = PathParser().parsePathString(data).toPath()
}

private fun lerp(
    start: Float,
    end: Float,
    fraction: Float
): Float = start + (end - start) * fraction

private const val LINE_WIDTH = 9f
private const val INNER_WIDTH = 4f
private const val INNER_ALPHA = 0.35f
private const val DETAIL_WIDTH = 6f
private const val DETAIL_ALPHA = 0.6f
private const val RING_WIDTH = 3f
private const val RING_ALPHA = 0.5f
private const val FLAP_DEGREES = 16f
private const val FLAP_HERTZ = 2.6f
private const val HEAD_X = 494f
private const val FIRST_PULSE_DELAY_MILLIS = 1720L
private const val PULSE_BUMP = 0.018f
private const val PULSE_BUMP_RISE = 0.12f
private const val PULSE_BUMP_FALL = 0.4f

/** Flying left wing, clockwise: shoulder, three feather tips with the notches between them, wing root. */
private val FlightCorners =
    listOf(
        Offset(490f, 380f),
        Offset(86f, 196f),
        Offset(176f, 260f),
        Offset(118f, 282f),
        Offset(214f, 302f),
        Offset(176f, 336f),
        Offset(276f, 334f),
        Offset(478f, 442f)
    )

private val FlightBows = listOf(-0.13f, 0.03f, -0.02f, 0.03f, -0.02f, 0.03f, -0.12f, 0f)

/** Left shield half as the same eight segments: top edge, side down to the point, and a body edge that collapses. */
private val ShieldSegments =
    listOf(
        listOf(Offset(500f, 358f), Offset(412.5f, 360f), Offset(315f, 348f), Offset(250f, 326f)),
        listOf(Offset(250f, 326f), Offset(247.5f, 358f), Offset(246.2f, 390f), Offset(247.5f, 420f)),
        listOf(Offset(247.5f, 420f), Offset(248.8f, 450f), Offset(251.2f, 476f), Offset(257.5f, 500f)),
        listOf(Offset(257.5f, 500f), Offset(263.8f, 526f), Offset(270f, 554f), Offset(280f, 580f)),
        listOf(Offset(280f, 580f), Offset(290f, 606f), Offset(302.5f, 634f), Offset(320f, 660f)),
        listOf(Offset(320f, 660f), Offset(337.5f, 686f), Offset(356.2f, 712f), Offset(377.5f, 736f)),
        listOf(Offset(377.5f, 736f), Offset(410f, 774f), Offset(452.5f, 816f), Offset(500f, 846f)),
        List(4) { Offset(500f, 846f) }
    )

/** Sparrow head with a short cone beak; the neck ends wide and rounded so the head sits on the shield. */
private const val HEAD =
    "M414 262 L452 246 L440 222 C474 190 530 176 578 186 C612 192 636 210 648 232 L714 264 L652 290 " +
        "C640 304 618 312 600 314 C592 340 580 362 566 374 C546 380 518 380 500 374 C474 358 454 336 440 306 " +
        "C428 292 418 278 414 262 Z"

/** Eye with a flat lid sloping down towards the beak, which gives the stern look. */
private const val EYE = "M548 230 L620 244 L606 258 C588 262 566 258 554 246 Z"
private const val BEAK_LINE = "M652 262 L700 264"
private const val INNER_SHIELD =
    "M500 400 C462.5 400 362.5 392 310 378 C305 470 315 556 355 640 C390 712 440 762 500 798 " +
        "C560 762 610 712 645 640 C685 556 695 470 690 378 C637.5 392 537.5 400 500 400 Z"
private const val RING =
    "M500 358 C412.5 360 315 348 250 324 C242.5 440 250 560 307.5 660 C355 742 425 800 500 846 " +
        "C575 800 645 742 692.5 660 C750 560 757.5 440 750 324 C685 348 587.5 360 500 358 Z"

@Preview
@Composable
private fun SparrowAnimationPreview() {
    SparrowTheme {
        SparrowAnimation(modifier = Modifier.size(210.dp))
    }
}
