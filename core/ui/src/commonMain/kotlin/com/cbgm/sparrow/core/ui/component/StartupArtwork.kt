package com.cbgm.sparrow.core.ui.component

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.graphicsLayer

object SparrowBrandColors {
    val Black = Color(0xFF020817)
    val Background = Color(0xFF071A2E)
    val Surface = Color(0xFF102A46)
    val Accent = Color(0xFF35E6FF)
    val OnDark = Color(0xFFF7FBFF)
}

@Composable
fun SparrowAnimation(
    modifier: Modifier = Modifier,
    animated: Boolean = false
) {
    var started by remember { mutableStateOf(!animated) }

    LaunchedEffect(animated) {
        if (animated) {
            started = true
        }
    }

    val shieldProgress by animateFloatAsState(
        targetValue = if (started) 1f else 0.78f,
        animationSpec = tween(
            durationMillis = 420,
            easing = FastOutSlowInEasing
        ),
        label = "sparrowShieldReveal"
    )
    val headProgress by animateFloatAsState(
        targetValue = if (started) 1f else 0f,
        animationSpec = tween(
            durationMillis = 520,
            delayMillis = 150,
            easing = FastOutSlowInEasing
        ),
        label = "sparrowHeadReveal"
    )

    val infiniteTransition = rememberInfiniteTransition(label = "sparrowIdle")
    val pulse by infiniteTransition.animateFloat(
        initialValue = 0.992f,
        targetValue = 1.012f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = 1_800,
                easing = FastOutSlowInEasing
            ),
            repeatMode = RepeatMode.Reverse
        ),
        label = "sparrowPulse"
    )
    val blink by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = keyframes {
                durationMillis = 3_200
                1f at 0
                1f at 2_400
                0.12f at 2_480
                1f at 2_580
                1f at 3_200
            }
        ),
        label = "sparrowBlink"
    )

    SparrowMarkCanvas(
        modifier = modifier
            .aspectRatio(1f)
            .graphicsLayer {
                val idleScale = if (animated) pulse else 1f
                scaleX = idleScale
                scaleY = idleScale
            },
        headProgress = headProgress,
        shieldProgress = shieldProgress,
        eyeScaleY = if (animated) blink else 1f
    )
}

@Composable
private fun SparrowMarkCanvas(
    headProgress: Float,
    shieldProgress: Float,
    eyeScaleY: Float,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val unit = size.minDimension
        val centerX = size.width / 2f
        val headWidth = unit * 0.36f
        val headHeight = unit * 0.34f
        val headTop = unit * 0.16f + (1f - headProgress) * unit * 0.13f

        drawOval(
            color = SparrowBrandColors.Accent,
            topLeft = Offset(
                x = centerX - headWidth / 2f,
                y = headTop
            ),
            size = Size(headWidth, headHeight)
        )

        val eyeY = headTop + headHeight * 0.47f
        val eyeOffsetX = headWidth * 0.20f
        val eyeRadius = unit * 0.018f
        val eyeHeight = eyeRadius * eyeScaleY

        listOf(centerX - eyeOffsetX, centerX + eyeOffsetX).forEach { eyeX ->
            drawOval(
                color = SparrowBrandColors.Background,
                topLeft = Offset(eyeX - eyeRadius, eyeY - eyeHeight),
                size = Size(eyeRadius * 2f, eyeHeight * 2f)
            )
        }

        val beakCenterY = headTop + headHeight * 0.60f
        val beakHalfWidth = unit * 0.038f
        val beak = Path().apply {
            moveTo(centerX, beakCenterY - beakHalfWidth * 0.55f)
            lineTo(centerX + beakHalfWidth, beakCenterY)
            lineTo(centerX, beakCenterY + beakHalfWidth * 0.65f)
            lineTo(centerX - beakHalfWidth, beakCenterY)
            close()
        }
        drawPath(
            path = beak,
            color = SparrowBrandColors.Background
        )

        val shieldScale = 0.78f + 0.22f * shieldProgress
        val shield = createShieldPath(
            width = size.width,
            height = size.height
        )

        scale(
            scale = shieldScale,
            pivot = Offset(centerX, size.height * 0.50f)
        ) {
            drawPath(
                path = shield,
                color = SparrowBrandColors.Surface,
                alpha = shieldProgress
            )
            drawPath(
                path = shield,
                color = SparrowBrandColors.Accent,
                alpha = shieldProgress,
                style = Stroke(width = unit * 0.022f)
            )
        }
    }
}

private fun createShieldPath(
    width: Float,
    height: Float
): Path {
    val cx = width / 2f
    return Path().apply {
        moveTo(cx, height * 0.405f)
        cubicTo(
            width * 0.39f,
            height * 0.455f,
            width * 0.29f,
            height * 0.47f,
            width * 0.20f,
            height * 0.485f
        )
        cubicTo(
            width * 0.205f,
            height * 0.69f,
            width * 0.31f,
            height * 0.84f,
            cx,
            height * 0.94f
        )
        cubicTo(
            width * 0.69f,
            height * 0.84f,
            width * 0.795f,
            height * 0.69f,
            width * 0.80f,
            height * 0.485f
        )
        cubicTo(
            width * 0.71f,
            height * 0.47f,
            width * 0.61f,
            height * 0.455f,
            cx,
            height * 0.405f
        )
        close()
    }
}
