package com.cbgm.sparrow.feature.chats.presentation.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.dp
import dev.chrisbanes.haze.HazeInput
import dev.chrisbanes.haze.HazePerformanceMode
import dev.chrisbanes.haze.blur.hazeBlur
import dev.chrisbanes.haze.blur.materials.HazeMaterials
import dev.chrisbanes.haze.hazeSource
import dev.chrisbanes.haze.rememberHazeState
import kotlin.math.roundToInt

internal data class MessageContextAnchor(
    val messageId: String,
    val boundsInRoot: IntRect,
    val isMine: Boolean
)

internal fun Modifier.captureMessageContextAnchor(
    messageId: String,
    isMine: Boolean,
    onAnchorChanged: (MessageContextAnchor) -> Unit
): Modifier = onGloballyPositioned { coords ->
    onAnchorChanged(
        MessageContextAnchor(
            messageId = messageId,
            boundsInRoot = coords.boundsInRoot().toIntRect(),
            isMine = isMine
        )
    )
}

@Composable
internal fun MessageContextHost(
    anchor: MessageContextAnchor?,
    menuColor: Color,
    onDismiss: () -> Unit,
    onReplyClick: () -> Unit,
    onReactionClick: (String) -> Unit,
    modifier: Modifier = Modifier,
    reactionBurst: MessageReactionBurst? = null,
    onReactionBurstDismiss: () -> Unit = {},
    onCopyClick: () -> Unit = {},
    onDeleteClick: () -> Unit,
    preview: @Composable () -> Unit,
    content: @Composable () -> Unit
) {
    val hazeState = rememberHazeState()
    val contextBlurStyle = HazeMaterials
        .regular()
        .then {
            blurRadius(6.dp)
            noiseFactor(0.8f)
        }

    Box(modifier = modifier.fillMaxSize()) {
        Box(modifier = Modifier.fillMaxSize().hazeSource(hazeState)) {
            content()
        }

        if (anchor != null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .hazeBlur(
                        input = HazeInput.Sources(hazeState),
                        style = contextBlurStyle,
                        performanceMode = HazePerformanceMode.Quality,
                        expandLayerBounds = false
                    )
            )

            MessageContextOverlay(
                anchor = anchor,
                menuColor = menuColor,
                onDismiss = onDismiss,
                onReplyClick = {
                    onDismiss()
                    onReplyClick()
                },
                onReactionClick = { emoji ->
                    onDismiss()
                    onReactionClick(emoji)
                },
                onCopyClick = {
                    onDismiss()
                    onCopyClick()
                },
                onDeleteClick = {
                    onDismiss()
                    onDeleteClick()
                },
                preview = preview
            )
        }

        reactionBurst?.let { burst ->
            MessageReactionBurstOverlay(
                burst = burst,
                onDismiss = onReactionBurstDismiss
            )
        }
    }
}

@Composable
internal fun MessageContextOverlay(
    anchor: MessageContextAnchor,
    menuColor: Color,
    onDismiss: () -> Unit,
    onReplyClick: () -> Unit,
    onReactionClick: (String) -> Unit,
    onDeleteClick: () -> Unit,
    modifier: Modifier = Modifier,
    onCopyClick: () -> Unit = {},
    preview: @Composable () -> Unit
) {
    val density = LocalDensity.current
    val horizontalMarginPx = with(density) { 12.dp.roundToPx() }
    val verticalMarginPx = with(density) { 16.dp.roundToPx() }
    val previewWidth = with(density) { anchor.boundsInRoot.width.toDp() }

    var overlayOriginInRoot by remember { mutableStateOf(IntOffset.Zero) }
    val interactionSource = remember { MutableInteractionSource() }

    Layout(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.scrim.copy(alpha = 0.01f))
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onDismiss
            )
            .onGloballyPositioned { coords ->
                val bounds = coords.boundsInRoot()
                overlayOriginInRoot = IntOffset(bounds.left.roundToInt(), bounds.top.roundToInt())
            },
        content = {
            Column(
                modifier = Modifier.widthIn(min = 300.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                horizontalAlignment = if (anchor.isMine) Alignment.End else Alignment.Start
            ) {
                Box(modifier = Modifier.width(previewWidth)) { preview() }
                MessageContextActionMenu(
                    color = menuColor,
                    onReplyClick = onReplyClick,
                    onReactionClick = onReactionClick,
                    onCopyClick = onCopyClick,
                    showDelete = anchor.isMine,
                    onDeleteClick = onDeleteClick
                )
            }
        }
    ) { measurables, constraints ->
        val contentPlaceable =
            measurables.single().measure(constraints.copy(minWidth = 0, minHeight = 0))

        val anchorLeft = anchor.boundsInRoot.left - overlayOriginInRoot.x
        val anchorTop = anchor.boundsInRoot.top - overlayOriginInRoot.y
        val anchorRight = anchor.boundsInRoot.right - overlayOriginInRoot.x

        val desiredX = if (anchor.isMine) anchorRight - contentPlaceable.width else anchorLeft
        val maxX =
            (constraints.maxWidth - horizontalMarginPx - contentPlaceable.width).coerceAtLeast(
                horizontalMarginPx
            )
        val x = desiredX.coerceIn(horizontalMarginPx, maxX)

        val maxY =
            (constraints.maxHeight - verticalMarginPx - contentPlaceable.height).coerceAtLeast(
                verticalMarginPx
            )
        val y = anchorTop.coerceIn(verticalMarginPx, maxY)

        layout(constraints.maxWidth, constraints.maxHeight) {
            contentPlaceable.placeRelative(x, y)
        }
    }
}

private fun Rect.toIntRect(): IntRect = IntRect(
    left = left.roundToInt(),
    top = top.roundToInt(),
    right = right.roundToInt(),
    bottom = bottom.roundToInt()
)
