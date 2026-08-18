package com.cbgm.sparrow.feature.chats.presentation.component

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.constrainHeight
import androidx.compose.ui.unit.constrainWidth
import androidx.compose.ui.unit.dp
import com.cbgm.sparrow.core.ui.theme.SparrowTheme
import com.cbgm.sparrow.core.ui.theme.spacing

private val FieldColor = Color(0xFF102A46)
private val ButtonNotchRadius = 14.dp
private val ButtonRightCornerRadius = 28.dp

private const val ShapeAnimationDuration = 220

@Composable
internal fun MessageInput(
    value: String,
    onValueChange: (String) -> Unit,
    onSendClick: () -> Unit,
    enabled: Boolean,
    modifier: Modifier = Modifier
) {
    var textLineCount by remember { mutableIntStateOf(1) }

    val isMultiline = textLineCount > 1

    /*
     * NEVER CHANGE THESE DURING THE MORPH.
     */
    val buttonWidth = 42.dp
    val buttonHeight = 38.dp
    val overlap = 10.dp

    val morphProgress by animateFloatAsState(
        targetValue =
            if (isMultiline) {
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

    /*
     * Single-line icon:
     *
     * centered inside the original 56dp button body.
     *
     * Multiline:
     *
     * centered inside the rightmost 38dp circle.
     *
     * This animation is entirely internal and cannot affect
     * TextField measurement.
     */
    val iconAreaWidth by animateDpAsState(
        targetValue =
            if (isMultiline) {
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

    Row(
        modifier = modifier
            .fillMaxWidth()
            .windowInsetsPadding(
                WindowInsets.navigationBars
            )
            .imePadding(),
        verticalAlignment = Alignment.Bottom
    ) {
        /*
         * NO start spacer.
         *
         * MessageInput therefore starts directly at its
         * available left edge.
         */
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier
                .weight(1f)
                .background(
                    color = FieldColor,
                    shape = RoundedCornerShape(20.dp)
                )
                .padding(
                    horizontal = MaterialTheme.spacing.base,
                    vertical = 8.dp
                ),
            enabled = enabled,
            minLines = 1,
            maxLines = 5,
            textStyle =
                MaterialTheme.typography.bodyMedium.copy(
                    color =
                        MaterialTheme.colorScheme.onBackground
                ),
            cursorBrush =
                SolidColor(
                    MaterialTheme.colorScheme.secondary
                ),
            keyboardOptions =
                KeyboardOptions(
                    imeAction = ImeAction.Default
                ),
            onTextLayout = { result ->
                textLineCount = result.lineCount
            },
            decorationBox = { innerTextField ->
                innerTextField()
            }
        )

        SendButtonSlot(
            buttonWidth = buttonWidth,
            buttonHeight = buttonHeight,
            overlap = overlap,
            modifier = Modifier.align(
                if (isMultiline) {
                    Alignment.Bottom
                } else {
                    Alignment.CenterVertically
                }
            )
        ) {
            /*
             * ALWAYS exactly:
             *
             * 56 + 16 = 72dp wide
             *
             * NEVER animates.
             */
            Box(
                modifier = Modifier
                    .width(buttonWidth + overlap)
                    .height(buttonHeight)
                    .clip(
                        MorphingSendButtonShape(
                            progress = morphProgress,
                            notchRadius = ButtonNotchRadius,
                            rightCornerRadius = ButtonRightCornerRadius
                        )
                    )
                    .background(FieldColor)
                    .clickable(
                        enabled = enabled && value.isNotBlank(),
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
                        imageVector = Icons.AutoMirrored.Filled.Send,
                        contentDescription = null,
                        tint =
                            if (enabled && value.isNotBlank()) {
                                MaterialTheme.colorScheme.secondary
                            } else {
                                MaterialTheme.colorScheme.secondary.copy(alpha = 0.35f)
                            },
                        modifier = Modifier
                            .padding(start = MaterialTheme.spacing.base.div(2))
                            .size(18.dp)
                    )
                }
            }
        }
    }
}

/**
 * Fixed send-button layout.
 *
 * The Row always sees:
 *
 *     buttonWidth + endSpacing
 *
 * The actual blue canvas is:
 *
 *     buttonWidth + overlap
 *
 * and is placed overlap dp to the LEFT.
 *
 * Therefore:
 *
 * - overlap never changes
 * - right spacing never changes
 * - TextField width never changes
 */
@Composable
private fun SendButtonSlot(
    buttonWidth: Dp,
    buttonHeight: Dp,
    overlap: Dp,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Layout(
        content = content,
        modifier = modifier
            .width(buttonWidth)
            .height(buttonHeight)
    ) { measurables, constraints ->

        val overlapPx = overlap.roundToPx()

        val canvasWidthPx = (buttonWidth + overlap).roundToPx()

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
            /*
             * 72dp canvas placed at -16dp:
             *
             * left  = -16
             * right = 56
             *
             * slot width = 64
             *
             * therefore exactly 8dp remains on the right.
             */
            placeable.place(x = -overlapPx, y = 0)
        }
    }
}

@Preview
@Composable
private fun MessageInputPreview() {
    SparrowTheme {
        MessageInput(
            value = "Hello",
            onValueChange = {},
            onSendClick = {},
            enabled = true
        )
    }
}

@Preview
@Composable
private fun MultilineMessageInputPreview() {
    SparrowTheme {
        MessageInput(
            value =
                "Hello, this is a longer message that wraps " +
                    "onto a second line.",
            onValueChange = {},
            onSendClick = {},
            enabled = true
        )
    }
}
