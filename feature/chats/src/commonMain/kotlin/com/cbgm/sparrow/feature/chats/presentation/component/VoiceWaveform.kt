package com.cbgm.sparrow.feature.chats.presentation.component

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import kotlin.math.max

private val PlaceholderWaveform =
    listOf(
        0.24f,
        0.42f,
        0.68f,
        0.36f,
        0.82f,
        0.54f,
        0.32f,
        0.74f,
        0.46f,
        0.9f,
        0.58f,
        0.3f,
        0.64f,
        0.4f,
        0.78f,
        0.5f,
        0.28f,
        0.7f,
        0.44f,
        0.86f,
        0.52f,
        0.34f,
        0.66f,
        0.38f
    )

@Composable
internal fun VoiceWaveform(
    waveform: List<Float>,
    progress: Float,
    playedColor: Color,
    remainingColor: Color,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val values = waveform.ifEmpty { PlaceholderWaveform }
        if (values.isEmpty() || size.width <= 0f || size.height <= 0f) return@Canvas

        val slotWidth = size.width / values.size
        val barWidth = max(1f, slotWidth * 0.42f)
        val radius = barWidth / 2f
        val progressIndex = progress.coerceIn(0f, 1f) * values.size

        values.forEachIndexed { index, amplitude ->
            val barHeight = size.height * amplitude.coerceIn(0.12f, 1f)
            val left = index * slotWidth + (slotWidth - barWidth) / 2f
            val top = (size.height - barHeight) / 2f

            drawRoundRect(
                color = if (index < progressIndex) playedColor else remainingColor,
                topLeft = Offset(left, top),
                size = Size(barWidth, barHeight),
                cornerRadius = CornerRadius(radius, radius)
            )
        }
    }
}

internal fun formatVoiceDuration(durationMilliseconds: Long): String {
    val totalSeconds = (durationMilliseconds.coerceAtLeast(0L) / 1_000L).toInt()
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "$minutes:${seconds.toString().padStart(2, '0')}"
}
