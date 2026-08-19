package com.cbgm.sparrow.core.ui.component

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import com.cbgm.sparrow.core.ui.theme.Dimens
import com.cbgm.sparrow.core.ui.theme.spacing

private data class ShadowLayer(
    val outline: Outline,
    val spread: Float,
    val alpha: Float
)

private fun Modifier.bottomFadingShadow(
    shape: Shape,
    size: Dp = Dimens.Card.iconSize,
    color: Color,
    maxAlpha: Float = 0.30f,
    layerCount: Int = 32
): Modifier =
    drawWithCache {
        val maximumSpread = size.toPx()

        val layers =
            (layerCount downTo 1).map { index ->
                val fraction = index.toFloat() / layerCount
                val spread = maximumSpread * fraction

                ShadowLayer(
                    outline =
                        shape.createOutline(
                            size =
                                Size(
                                    width = this.size.width + spread * 2f,
                                    height = this.size.height + spread * 2f
                                ),
                            layoutDirection = layoutDirection,
                            density = this
                        ),
                    spread = spread,
                    alpha = maxAlpha * 2f * (1f - fraction) / layerCount
                )
            }

        onDrawBehind {
            layers.forEach { layer ->
                translate(
                    left = -layer.spread,
                    top = 0f
                ) {
                    drawOutline(
                        outline = layer.outline,
                        color = color.copy(alpha = color.alpha * layer.alpha)
                    )
                }
            }
        }
    }

@Composable
fun SparrowCard(
    modifier: Modifier = Modifier,
    isFadingEnabled: Boolean = false,
    content: @Composable () -> Unit
) {
    var animationStarted by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        animationStarted = true
    }

    val cardAlpha by animateFloatAsState(
        targetValue =
            if (animationStarted) {
                1f
            } else {
                0f
            },
        animationSpec = tween(durationMillis = 500, delayMillis = 260),
        label = "startupCardAlpha"
    )

    val cardTranslation by animateFloatAsState(
        targetValue =
            if (animationStarted) {
                0f
            } else {
                42f
            },
        animationSpec =
            tween(
                durationMillis = 650,
                delayMillis = 180,
                easing = FastOutSlowInEasing
            ),
        label = "startupCardTranslation"
    )

    val shape = MaterialTheme.shapes.small

    Surface(
        modifier =
            modifier
                .fillMaxWidth()
                .padding(bottom = MaterialTheme.spacing.card.bottomShadowPadding)
                .graphicsLayer {
                    if (isFadingEnabled) {
                        alpha = cardAlpha
                        translationY = cardTranslation
                    }
                }.bottomFadingShadow(
                    shape = shape,
                    size = Dimens.Card.iconSize,
                    color = MaterialTheme.colorScheme.scrim,
                    maxAlpha = 0.30f
                ),
        color = MaterialTheme.colorScheme.surfaceContainer,
        shape = shape,
        tonalElevation = Dimens.Base.zero,
        shadowElevation = Dimens.Base.zero,
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
        shape = MaterialTheme.shapes.small,
        color = MaterialTheme.colorScheme.surfaceContainer,
        content = content
    )
}
