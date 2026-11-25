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
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.cbgm.sparrow.feature.onboarding.presentation.pages.component.progress

@Composable
fun ContactsGraphic(
    clock: State<Long>,
    modifier: Modifier = Modifier
) {
    val pulse by rememberInfiniteTransition(label = "contacts").animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(PULSE_MILLISECONDS, easing = LinearEasing), RepeatMode.Restart),
        label = "contactsPulse"
    )
    val you = MaterialTheme.colorScheme.primary
    val contact = MaterialTheme.colorScheme.onBackground
    Canvas(modifier = modifier) {
        val inset = DOT_RADIUS.dp.toPx() + size.height * VERTICAL_INSET
        val place = { x: Float, y: Float -> Offset(x * size.width, inset + y * (size.height - 2 * inset)) }
        val center = place(HUB.first, HUB.second)
        CONTACTS.forEachIndexed { index, (x, y) ->
            val point = place(x, y)
            val start = FIRST_MILLISECONDS + index * STEP_MILLISECONDS
            val reach = clock.progress(start + LINE_DELAY_MILLISECONDS, LINE_MILLISECONDS)
            drawLine(you.copy(alpha = LINE_ALPHA), center, center + (point - center) * reach, strokeWidth = 1.1.dp.toPx())
            drawCircle(contact.copy(alpha = clock.progress(start, DOT_MILLISECONDS)), DOT_RADIUS.dp.toPx(), point)
        }
        drawCircle(you, 7.dp.toPx(), center)
        drawCircle(you.copy(alpha = (1f - pulse) * PULSE_ALPHA), (7f + pulse * PULSE_GROWTH).dp.toPx(), center, style = Stroke(1.2.dp.toPx()))
    }
}

private val HUB = 0.5f to 0.41f
private val CONTACTS = listOf(0.18f to 0.07f, 0.29f to 0.81f, 0.74f to 0f, 0.83f to 0.66f, 0.55f to 1f, 0.09f to 0.57f, 0.91f to 0.23f)
private const val VERTICAL_INSET = 0.12f
private const val DOT_RADIUS = 4.5f
private const val FIRST_MILLISECONDS = 500L
private const val STEP_MILLISECONDS = 380L
private const val LINE_DELAY_MILLISECONDS = 200L
private const val LINE_MILLISECONDS = 600L
private const val DOT_MILLISECONDS = 500L
private const val LINE_ALPHA = 0.7f
private const val PULSE_ALPHA = 0.6f
private const val PULSE_GROWTH = 20f
private const val PULSE_MILLISECONDS = 2_000
