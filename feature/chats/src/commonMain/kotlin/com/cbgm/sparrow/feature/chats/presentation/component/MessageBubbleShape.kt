package com.cbgm.sparrow.feature.chats.presentation.component

import androidx.compose.runtime.Stable
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import kotlin.math.min

@Stable
class MessageBubbleShape(
    private val isMine: Boolean,
    private val cornerRadius: Dp,
    private val tailWidth: Dp,
    private val tailHeight: Dp,
    private val tailReturnOffset: Dp
) : Shape {
    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density
    ): Outline {
        with(density) {
            val radius = min(cornerRadius.toPx(), size.height / 2f)
            val tailWidth = tailWidth.toPx()
            val tailHeight = tailHeight.toPx()

            return Outline.Generic(
                if (isMine) {
                    outgoingBubble(
                        size = size,
                        radius = radius,
                        tailWidth = tailWidth,
                        tailHeight = tailHeight
                    )
                } else {
                    incomingBubble(
                        size = size,
                        radius = radius,
                        tailWidth = tailWidth,
                        tailHeight = tailHeight
                    )
                }
            )
        }
    }

    private fun outgoingBubble(
        size: Size,
        radius: Float,
        tailWidth: Float,
        tailHeight: Float
    ): Path {
        val bodyRight = size.width - tailWidth
        val bottom = size.height

        return Path().apply {
            // Top-left
            moveTo(radius, 0f)

            // Top
            lineTo(bodyRight - radius, 0f)

            // Top-right
            quadraticTo(
                bodyRight,
                0f,
                bodyRight,
                radius
            )

            // Right side
            lineTo(
                bodyRight,
                bottom - tailHeight
            )

            /*
             * Tail:
             *
             *     |
             *     |
             *     \__
             *        `.
             *          >
             *       _.'
             *  ____/
             *
             * First curve goes outward to the tip.
             */
            cubicTo(
                bodyRight,
                bottom - tailHeight * 0.45f,
                bodyRight + tailWidth * 0.35f,
                bottom - tailHeight * 0.10f,
                size.width,
                bottom
            )

            /*
             * Concave return curve.
             *
             * This is the important part that makes it look
             * like an actual speech-bubble tail.
             */
            cubicTo(
                bodyRight + tailWidth * 0.35f,
                bottom - tailReturnOffset.value,
                bodyRight + tailWidth * 0.05f,
                bottom - tailHeight * 0.15f,
                bodyRight - radius * 0.45f,
                bottom - radius * 0.10f
            )

            // Bottom-right into bottom edge
            quadraticTo(
                bodyRight - radius * 0.70f,
                bottom,
                bodyRight - radius,
                bottom
            )

            // Bottom
            lineTo(radius, bottom)

            // Bottom-left
            quadraticTo(
                0f,
                bottom,
                0f,
                bottom - radius
            )

            // Left side
            lineTo(0f, radius)

            // Top-left
            quadraticTo(
                0f,
                0f,
                radius,
                0f
            )

            close()
        }
    }

    private fun incomingBubble(
        size: Size,
        radius: Float,
        tailWidth: Float,
        tailHeight: Float
    ): Path {
        val bodyLeft = tailWidth
        val bottom = size.height

        return Path().apply {
            // Top-left
            moveTo(bodyLeft + radius, 0f)

            // Top
            lineTo(size.width - radius, 0f)

            // Top-right
            quadraticTo(
                size.width,
                0f,
                size.width,
                radius
            )

            // Right side
            lineTo(
                size.width,
                bottom - radius
            )

            // Bottom-right
            quadraticTo(
                size.width,
                bottom,
                size.width - radius,
                bottom
            )

            // Bottom
            lineTo(bodyLeft + radius, bottom)

            // Flow toward concave side of tail
            quadraticTo(
                bodyLeft + radius * 0.70f,
                bottom,
                bodyLeft + radius * 0.45f,
                bottom - radius * 0.10f
            )

            /*
             * Concave curve into the tail.
             */
            cubicTo(
                bodyLeft - tailWidth * 0.05f,
                bottom - tailHeight * 0.15f,
                bodyLeft - tailWidth * 0.35f,
                bottom - tailReturnOffset.value,
                0f,
                bottom
            )

            /*
             * Outer tail curve back into the side.
             */
            cubicTo(
                bodyLeft - tailWidth * 0.35f,
                bottom - tailHeight * 0.10f,
                bodyLeft,
                bottom - tailHeight * 0.45f,
                bodyLeft,
                bottom - tailHeight
            )

            // Left side
            lineTo(bodyLeft, radius)

            // Top-left
            quadraticTo(
                bodyLeft,
                0f,
                bodyLeft + radius,
                0f
            )

            close()
        }
    }
}
