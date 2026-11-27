package com.cbgm.sparrow.feature.onboarding.presentation.pages.graphic

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.PathOperation
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.cbgm.sparrow.core.ui.theme.AppThemeMode
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/** Sun, phone and moon in a row; the chosen one lights up and a dot slides along the curve below to it. */
@Composable
fun ThemeGraphic(
    mode: AppThemeMode,
    modifier: Modifier = Modifier
) {
    val position by animateFloatAsState(
        targetValue =
            when (mode) {
                AppThemeMode.LIGHT -> 0f
                AppThemeMode.SYSTEM -> HALF
                AppThemeMode.DARK -> 1f
            },
        animationSpec = tween(MOVE_MILLISECONDS),
        label = "themePosition"
    )
    val chosen = MaterialTheme.colorScheme.primary
    val other = MaterialTheme.colorScheme.outline
    val sun by animateColorAsState(if (mode == AppThemeMode.LIGHT) chosen else other, label = "themeSun")
    val phone by animateColorAsState(if (mode == AppThemeMode.SYSTEM) chosen else other, label = "themePhone")
    val moon by animateColorAsState(if (mode == AppThemeMode.DARK) chosen else other, label = "themeMoon")
    Canvas(modifier = modifier) {
        val stroke = Stroke(1.4.dp.toPx(), cap = StrokeCap.Round)
        val icon = ICON_SIZE.dp.toPx()
        val dot = 5.dp.toPx()
        val xs = listOf(size.width * END_INSET, size.width * HALF, size.width * (1f - END_INSET))
        val iconCenter = icon / 2f
        drawSun(Offset(xs[0], iconCenter), icon, sun, stroke)
        drawPhone(Offset(xs[1], iconCenter), icon, phone, stroke)
        drawMoon(Offset(xs[2], iconCenter), icon, moon, stroke)

        val top = icon + CURVE_GAP.dp.toPx()
        val bottom = size.height - dot
        val start = Offset(xs[0], top)
        val end = Offset(xs[2], top)
        val control = Offset(xs[1], 2f * bottom - top)
        val curve = Path().apply {
            moveTo(start.x, start.y)
            quadraticTo(control.x, control.y, end.x, end.y)
        }
        drawPath(curve, other, style = Stroke(1.2.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(3.dp.toPx(), 6.dp.toPx()))))
        val t = position
        val point = start * ((1f - t) * (1f - t)) + control * (2f * (1f - t) * t) + end * (t * t)
        drawCircle(chosen, dot, point)
    }
}

private fun DrawScope.drawSun(
    center: Offset,
    size: Float,
    color: Color,
    stroke: Stroke
) {
    drawCircle(color, size * SUN_CORE, center, style = stroke)
    repeat(SUN_RAYS) { index ->
        val angle = 2.0 * PI * index / SUN_RAYS
        val direction = Offset(cos(angle).toFloat(), sin(angle).toFloat())
        drawLine(color, center + direction * (size * SUN_RAY_START), center + direction * (size * HALF), stroke.width, StrokeCap.Round)
    }
}

private fun DrawScope.drawPhone(
    center: Offset,
    size: Float,
    color: Color,
    stroke: Stroke
) {
    val phoneSize = Size(size * PHONE_WIDTH, size * PHONE_HEIGHT)
    drawRoundRect(
        color = color,
        topLeft = Offset(center.x - phoneSize.width / 2f, center.y - phoneSize.height / 2f),
        size = phoneSize,
        cornerRadius = CornerRadius(size * PHONE_CORNER),
        style = stroke
    )
}

private fun DrawScope.drawMoon(
    center: Offset,
    size: Float,
    color: Color,
    stroke: Stroke
) {
    val radius = size * MOON_RADIUS
    val shadow = Offset(radius * MOON_SHADOW_OFFSET, -radius * MOON_SHADOW_OFFSET)
    val moon = Path().apply { addOval(Rect(center, radius)) }
    val cut = Path().apply { addOval(Rect(center + shadow, radius)) }
    drawPath(Path.combine(PathOperation.Difference, moon, cut), color, style = stroke)
}

private const val ICON_SIZE = 40
private const val CURVE_GAP = 16
private const val END_INSET = 0.2f
private const val HALF = 0.5f
private const val SUN_CORE = 0.22f
private const val SUN_RAY_START = 0.34f
private const val SUN_RAYS = 8
private const val PHONE_WIDTH = 0.5f
private const val PHONE_HEIGHT = 0.9f
private const val PHONE_CORNER = 0.12f
private const val MOON_RADIUS = 0.4f
private const val MOON_SHADOW_OFFSET = 0.55f
private const val MOVE_MILLISECONDS = 600
