package com.cbgm.sparrow.feature.onboarding.presentation.pages.graphic

import androidx.compose.foundation.Canvas
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.cbgm.sparrow.core.ui.theme.divider
import com.cbgm.sparrow.feature.onboarding.presentation.pages.component.progress

@Composable
fun PermissionDotsGraphic(
    count: Int,
    clock: State<Long>,
    firstMillis: Long,
    stepMillis: Long,
    modifier: Modifier = Modifier
) {
    val line = MaterialTheme.colorScheme.divider
    val unlit = MaterialTheme.colorScheme.outlineVariant
    val lit = MaterialTheme.colorScheme.primary
    Canvas(modifier = modifier) {
        val gap = size.width * SPREAD / (count - 1)
        val left = size.width * (1f - SPREAD) / 2f
        val y = size.height / 2f
        drawLine(line, Offset(left, y), Offset(left + gap * (count - 1), y), strokeWidth = 1.dp.toPx())
        repeat(count) { index ->
            val center = Offset(left + gap * index, y)
            val start = firstMillis + index * stepMillis
            drawCircle(unlit, DOT_RADIUS.dp.toPx(), center)
            drawCircle(lit.copy(alpha = clock.progress(start, LIGHT_MILLISECONDS)), DOT_RADIUS.dp.toPx(), center)
            val ring = clock.progress(start, RING_MILLISECONDS)
            if (ring > 0f && ring < 1f) {
                drawCircle(lit.copy(alpha = 1f - ring), (DOT_RADIUS + ring * RING_GROWTH).dp.toPx(), center, style = Stroke(1.2.dp.toPx()))
            }
        }
    }
}

private const val SPREAD = 0.75f
private const val DOT_RADIUS = 5f
private const val RING_GROWTH = 12f
private const val LIGHT_MILLISECONDS = 400L
private const val RING_MILLISECONDS = 1_000L
