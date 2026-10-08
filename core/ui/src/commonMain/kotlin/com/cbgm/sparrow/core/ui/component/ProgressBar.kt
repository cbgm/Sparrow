package com.cbgm.sparrow.core.ui.component

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
fun PercentageBar(
    percentage: Int,
    modifier: Modifier = Modifier,
    height: Dp = 5.dp,
    trackColor: Color = MaterialTheme.colorScheme.outlineVariant,
    progressColor: Color = MaterialTheme.colorScheme.primary
) {
    val progress by animateFloatAsState(
        targetValue = percentage.coerceIn(0, 100) / 100f,
        animationSpec = tween(durationMillis = 400),
        label = "percentageBar"
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .clip(CircleShape)
            .background(trackColor)
            .semantics {
                progressBarRangeInfo = ProgressBarRangeInfo(progress, 0f..1f)
            }
    ) {
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .fillMaxWidth(progress)
                .background(progressColor, CircleShape)
        )
    }
}
