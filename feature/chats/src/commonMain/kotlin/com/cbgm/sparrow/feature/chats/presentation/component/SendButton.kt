package com.cbgm.sparrow.feature.chats.presentation.component

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.constrainHeight
import androidx.compose.ui.unit.constrainWidth
import com.cbgm.sparrow.core.ui.theme.Alpha
import com.cbgm.sparrow.core.ui.theme.Dimens
import com.cbgm.sparrow.core.ui.theme.messageInput
import com.cbgm.sparrow.core.ui.theme.spacing

private const val ShapeAnimationDuration = 220

@Composable
internal fun SendButton(
    buttonWidth: Dp,
    buttonHeight: Dp,
    isRound: Boolean,
    onSendClick: () -> Unit,
    enabled: Boolean,
    isEditing: Boolean,
    modifier: Modifier = Modifier
) {
    val messageInputShapes = MaterialTheme.shapes.messageInput

    val morphProgress by animateFloatAsState(
        targetValue =
            if (isRound) {
                1f
            } else {
                0f
            },
        animationSpec =
            tween(
                durationMillis = ShapeAnimationDuration
            ),
        label = "SendButtonMorph"
    )

    val iconAreaWidth by animateDpAsState(
        targetValue =
            if (isRound) {
                buttonHeight
            } else {
                buttonWidth
            },
        animationSpec =
            tween(
                durationMillis = ShapeAnimationDuration
            ),
        label = "SendIconAreaWidth"
    )

    SendButtonSlot(
        buttonWidth = buttonWidth,
        buttonHeight = buttonHeight,
        modifier = modifier
    ) {
        Box(
            modifier = Modifier
                .width(buttonWidth)
                .height(buttonHeight)
                .clip(
                    MorphingSendButtonShape(
                        progress = morphProgress,
                        notchRadius = messageInputShapes.buttonNotchRadius,
                        rightCornerRadius = messageInputShapes.buttonRightCornerRadius
                    )
                )
                .background(MaterialTheme.colorScheme.surfaceContainer)
                .clickable(
                    enabled = enabled,
                    onClick = onSendClick
                ),
            contentAlignment = Alignment.CenterEnd
        ) {
            Box(
                modifier = Modifier
                    .width(iconAreaWidth)
                    .height(buttonHeight),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isEditing) Icons.Default.Check else Icons.AutoMirrored.Filled.Send,
                    contentDescription = null,
                    tint =
                        if (enabled) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.primary.copy(alpha = Alpha.MessageInput.buttonBackground)
                        },
                    modifier = Modifier
                        .padding(start = MaterialTheme.spacing.base.div(2))
                        .size(Dimens.MessageInput.iconSize)
                )
            }
        }
    }
}

@Composable
private fun SendButtonSlot(
    buttonWidth: Dp,
    buttonHeight: Dp,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Layout(
        content = content,
        modifier = modifier
            .width(buttonWidth)
            .height(buttonHeight)
    ) { measurables, constraints ->

        val canvasWidthPx = (buttonWidth).roundToPx()

        val buttonHeightPx = buttonHeight.roundToPx()

        val slotWidthPx = (buttonWidth).roundToPx()

        val placeable = measurables.single().measure(
            Constraints.fixed(
                width = canvasWidthPx,
                height = buttonHeightPx
            )
        )

        layout(
            width = constraints.constrainWidth(slotWidthPx),
            height = constraints.constrainHeight(buttonHeightPx)
        ) {
            placeable.place(x = 0, y = 0)
        }
    }
}
