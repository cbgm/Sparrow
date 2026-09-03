package com.cbgm.sparrow.feature.chats.presentation.component

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import com.cbgm.sparrow.core.ui.theme.Dimens
import com.cbgm.sparrow.feature.chats.presentation.component.model.MessageReactionUi
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt

internal data class MessageReactionBurst(
    val reactions: List<MessageReactionUi>,
    val boundsInRoot: IntRect
)

@Composable
internal fun MessageReactionBurstOverlay(
    burst: MessageReactionBurst,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val emojis = remember(burst.reactions) {
        burst.reactions.flatMap { reaction -> List(reaction.count.coerceAtLeast(1)) { reaction.emoji } }
    }
    if (emojis.isEmpty()) return

    val d = remember(burst) { Dimens.MessageReaction }
    val staggerMillis = (d.burstStaggerWindowMillis / emojis.size)
        .coerceIn(d.burstStaggerMinMillis, d.burstStaggerMaxMillis)
    val totalDurationMillis = d.burstItemDurationMillis + staggerMillis * (emojis.size - 1)

    val animationProgress = remember(burst) { Animatable(0f) }
    LaunchedEffect(burst) {
        animationProgress.snapTo(0f)
        animationProgress.animateTo(1f, animationSpec = androidx.compose.animation.core.tween(totalDurationMillis, easing = LinearEasing))
        onDismiss()
    }

    val density = LocalDensity.current
    var overlayOriginInRoot by remember { mutableStateOf(IntOffset.Zero) }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onDismiss)
            .onGloballyPositioned { coords ->
                val bounds = coords.boundsInRoot()
                overlayOriginInRoot = IntOffset(bounds.left.roundToInt(), bounds.top.roundToInt())
            }
    ) {
        val widthPx = with(density) { maxWidth.toPx() }
        val heightPx = with(density) { maxHeight.toPx() }
        val burstLayerSizePx = with(density) { d.burstLayerSize.toPx() }
        val burstSpreadRadiusPx = with(density) { d.burstSpreadRadius.toPx() }
        val halfLayerSizePx = burstLayerSizePx / 2f

        val startX = (burst.boundsInRoot.left + burst.boundsInRoot.right) / 2f - overlayOriginInRoot.x
        val startY = (burst.boundsInRoot.top + burst.boundsInRoot.bottom) / 2f - overlayOriginInRoot.y
        val goldenAngle = PI * (3.0 - sqrt(5.0))

        emojis.forEachIndexed { index, emoji ->
            val elapsedMillis = animationProgress.value * totalDurationMillis
            val itemStartMillis = index * staggerMillis
            val isStarted = elapsedMillis >= itemStartMillis
            val itemProgress = ((elapsedMillis - itemStartMillis) / d.burstItemDurationMillis).coerceIn(0f, 1f)

            // Berechnungen für Flugbahn-Winkel und Ziel-Koordinaten
            val angle = -PI / 2.0 + goldenAngle * index
            val radiusFactor = d.burstRadiusBaseFactor + d.burstRadiusStepFactor * (index % d.burstRadiusVariantCount)
            val radiusPx = burstSpreadRadiusPx * radiusFactor

            val targetX = (startX + cos(angle).toFloat() * radiusPx).coerceIn(halfLayerSizePx, widthPx - halfLayerSizePx)
            val targetY = (startY + sin(angle).toFloat() * radiusPx).coerceIn(halfLayerSizePx, heightPx - halfLayerSizePx)

            val props = calculateItemProperties(itemProgress, startX, startY, targetX, targetY, index, d)

            Box(
                modifier = Modifier
                    .offset { IntOffset((props.x - halfLayerSizePx).roundToInt(), (props.y - halfLayerSizePx).roundToInt()) }
                    .size(d.burstLayerSize)
                    .graphicsLayer {
                        scaleX = props.scale
                        scaleY = props.scale
                        rotationZ = props.rotation
                        alpha = if (isStarted) props.alpha else 0f
                    },
                contentAlignment = Alignment.Center
            ) {
                Text(text = emoji, style = MaterialTheme.typography.headlineMedium)
            }
        }
    }
}

private data class ItemProperties(
    val x: Float,
    val y: Float,
    val scale: Float,
    val rotation: Float,
    val alpha: Float
)

private fun calculateItemProperties(
    itemProgress: Float,
    startX: Float,
    startY: Float,
    targetX: Float,
    targetY: Float,
    index: Int,
    d: Dimens.MessageReaction
): ItemProperties {
    val travelProgress = LinearOutSlowInEasing.transform((itemProgress / d.burstTravelEndFraction).coerceIn(0f, 1f))
    val fadeProgress = ((itemProgress - d.burstFadeStartFraction) / (1f - d.burstFadeStartFraction)).coerceIn(0f, 1f)
    val scaleUpProgress = (itemProgress / d.burstPeakScaleFraction).coerceIn(0f, 1f)

    val x = startX + (targetX - startX) * travelProgress
    val y = startY + (targetY - startY) * travelProgress

    val scale = if (itemProgress <= d.burstPeakScaleFraction) {
        d.burstInitialScale + (d.burstPeakScale - d.burstInitialScale) * scaleUpProgress
    } else {
        d.burstPeakScale + (d.burstEndScale - d.burstPeakScale) * fadeProgress
    }

    val rotationDirection = if (index % 2 == 0) 1f else -1f
    val rotation = d.burstRotationDegrees * rotationDirection * travelProgress

    return ItemProperties(x = x, y = y, scale = scale, rotation = rotation, alpha = 1f - fadeProgress)
}
