package com.cbgm.sparrow.core.ui.component

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import com.cbgm.sparrow.core.ui.theme.Dimens

@Composable
fun SparrowCard(
    modifier: Modifier = Modifier,
    isFadingEnabled: Boolean = false,
    content: @Composable () -> Unit
) {
    var animationStarted by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { animationStarted = true }

    val cardAlpha by animateFloatAsState(
        targetValue = if (animationStarted) 1f else 0f,
        animationSpec = tween(durationMillis = 500, delayMillis = 260),
        label = "startupCardAlpha"
    )
    val cardTranslation by animateFloatAsState(
        targetValue = if (animationStarted) 0f else 42f,
        animationSpec = tween(
            durationMillis = 650,
            delayMillis = 180,
            easing = FastOutSlowInEasing
        ),
        label = "startupCardTranslation"
    )

    Surface(
        modifier = modifier.fillMaxWidth().graphicsLayer {
            if (isFadingEnabled) {
                alpha = cardAlpha
                translationY = cardTranslation
            }
        },
        color = MaterialTheme.colorScheme.background,
        shape = MaterialTheme.shapes.small,
        content = content
    )
}

@Composable
fun SparrowCardNoAnimation(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.background,
        border = BorderStroke(Dimens.Base.borderStrokeWidth, MaterialTheme.colorScheme.outlineVariant),
        content = content
    )
}
