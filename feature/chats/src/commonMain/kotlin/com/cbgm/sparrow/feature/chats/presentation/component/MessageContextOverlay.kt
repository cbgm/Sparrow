package com.cbgm.sparrow.feature.chats.presentation.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntRect
import com.cbgm.sparrow.core.ui.component.SparrowOverlay
import com.cbgm.sparrow.core.ui.component.SparrowOverlayAnchor
import com.cbgm.sparrow.core.ui.component.captureSparrowOverlayAnchor
import com.cbgm.sparrow.core.ui.theme.Dimens
import com.cbgm.sparrow.core.ui.theme.spacing
import dev.chrisbanes.haze.HazeInput
import dev.chrisbanes.haze.HazePerformanceMode
import dev.chrisbanes.haze.blur.hazeBlur
import dev.chrisbanes.haze.blur.materials.HazeMaterials
import dev.chrisbanes.haze.hazeSource
import dev.chrisbanes.haze.rememberHazeState

private const val CONTEXT_BLUR_NOISE_FACTOR = 0.8f

internal data class MessageContextAnchor(
    val messageId: String,
    val overlayAnchor: SparrowOverlayAnchor,
    val isMine: Boolean
)

internal fun Modifier.captureMessageContextAnchor(
    messageId: String,
    isMine: Boolean,
    onAnchorChanged: (MessageContextAnchor) -> Unit
): Modifier =
    captureSparrowOverlayAnchor { overlayAnchor ->
        onAnchorChanged(
            MessageContextAnchor(
                messageId = messageId,
                overlayAnchor = overlayAnchor,
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
    onForwardClick: () -> Unit,
    onReactionClick: (String) -> Unit,
    showEdit: Boolean,
    onEditClick: () -> Unit,
    onCopyClick: () -> Unit,
    onDeleteClick: () -> Unit,
    modifier: Modifier = Modifier,
    preview: @Composable () -> Unit,
    content: @Composable () -> Unit
) {
    val hazeState = rememberHazeState()

    Box(modifier = modifier.fillMaxSize()) {
        Box(
            modifier =
                Modifier
                    .fillMaxSize()
                    .hazeSource(hazeState)
        ) {
            content()
        }

        anchor?.let { contextAnchor ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .hazeBlur(
                        input = HazeInput.Sources(hazeState),
                        style =
                            HazeMaterials.regular().then {
                                blurRadius(Dimens.MessageContext.blurRadius)
                                noiseFactor(CONTEXT_BLUR_NOISE_FACTOR)
                            },
                        performanceMode = HazePerformanceMode.Quality,
                        expandLayerBounds = false
                    )
            )

            SparrowOverlay(anchor = contextAnchor.overlayAnchor) {
                MessageContextOverlay(
                    anchorBounds = anchorBounds,
                    isMine = contextAnchor.isMine,
                    menuColor = menuColor,
                    onDismiss = onDismiss,
                    onReplyClick = {
                        onDismiss()
                        onReplyClick()
                    },
                    onForwardClick = {
                        onDismiss()
                        onForwardClick()
                    },
                    onReactionClick = { emoji ->
                        onDismiss()
                        onReactionClick(emoji)
                    },
                    showEdit = showEdit,
                    onEditClick = {
                        onDismiss()
                        onEditClick()
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
        }
    }
}

@Composable
private fun MessageContextOverlay(
    anchorBounds: IntRect,
    isMine: Boolean,
    menuColor: Color,
    onDismiss: () -> Unit,
    onReplyClick: () -> Unit,
    onForwardClick: () -> Unit,
    onReactionClick: (String) -> Unit,
    showEdit: Boolean,
    onEditClick: () -> Unit,
    onCopyClick: () -> Unit,
    onDeleteClick: () -> Unit,
    modifier: Modifier = Modifier,
    preview: @Composable () -> Unit
) {
    val density = LocalDensity.current
    val spacing = MaterialTheme.spacing
    val hMarginPx = with(density) { spacing.messageList.horizontalPadding.roundToPx() }
    val vMarginPx = with(density) { spacing.small.roundToPx() }
    val previewWidth = with(density) { anchorBounds.width.toDp() }

    Layout(
        modifier =
            modifier
                .fillMaxSize()
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onDismiss
                ),
        content = {
            Column(
                verticalArrangement = Arrangement.spacedBy(spacing.messageList.horizontalPadding),
                horizontalAlignment = if (isMine) Alignment.End else Alignment.Start
            ) {
                Box(modifier = Modifier.width(previewWidth)) {
                    preview()
                }

                MessageContextActionMenu(
                    color = menuColor,
                    onReplyClick = onReplyClick,
                    onForwardClick = onForwardClick,
                    onReactionClick = onReactionClick,
                    showEdit = showEdit,
                    onEditClick = onEditClick,
                    onCopyClick = onCopyClick,
                    showDelete = isMine,
                    onDeleteClick = onDeleteClick
                )
            }
        }
    ) { measurables, constraints ->
        val placeable = measurables.single().measure(constraints.copy(minWidth = 0, minHeight = 0))

        val desiredX = if (isMine) anchorBounds.right - placeable.width else anchorBounds.left
        val maxX = (constraints.maxWidth - hMarginPx - placeable.width).coerceAtLeast(hMarginPx)
        val x = desiredX.coerceIn(hMarginPx, maxX)

        val maxY = (constraints.maxHeight - vMarginPx - placeable.height).coerceAtLeast(vMarginPx)
        val y = anchorBounds.top.coerceIn(vMarginPx, maxY)

        layout(constraints.maxWidth, constraints.maxHeight) {
            placeable.placeRelative(x, y)
        }
    }
}
