package com.cbgm.sparrow.feature.chats.presentation.component

import androidx.compose.runtime.Stable
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection

@Stable
class MorphingSendButtonShape(
    private val progress: Float,
    private val notchRadius: Dp,
    private val rightCornerRadius: Dp
) : Shape {
    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density
    ): Outline {
        val morph = progress.coerceIn(0f, 1f)

        val width = size.width
        val height = size.height
        val centerY = height / 2f
        val verticalRadius = height / 2f

        val notchDepth = with(density) {
            notchRadius.toPx()
        }.coerceAtMost(width - verticalRadius)

        val requestedRightRadius = with(density) {
            rightCornerRadius.toPx()
        }

        /*
         * With a 38dp-high button, the maximum physically valid
         * radius is 19dp.
         */
        val initialRightRadius =
            requestedRightRadius.coerceAtMost(verticalRadius)

        val rightRadius = lerp(
            start = initialRightRadius,
            end = verticalRadius,
            fraction = morph
        )

        /*
         * In the round state the circle occupies only the
         * rightmost 38dp of the fixed canvas.
         *
         * Canvas:
         *
         * 72dp total
         *
         * Circle:
         *
         * 72 - 38 = 34dp left edge
         */
        val circleLeft = width - height
        val circleCenterX = width - verticalRadius

        /*
         * At morph = 0:
         *
         * top/bottom begin at x = 0
         *
         * At morph = 1:
         *
         * top/bottom begin at the circle center.
         */
        val leftTopX = lerp(
            start = 0f,
            end = circleCenterX,
            fraction = morph
        )

        /*
         * At morph = 0:
         *
         * deepest point of the concave cutout = notchDepth
         *
         * At morph = 1:
         *
         * deepest point becomes the LEFT edge of the circle.
         *
         * This flips the left side smoothly from concave
         * to convex.
         */
        val leftMiddleX = lerp(
            start = notchDepth,
            end = circleLeft,
            fraction = morph
        )

        val kappa = 0.5522848f

        /*
         * Control point used by both halves of the left curve.
         *
         * It works whether:
         *
         * leftMiddleX > leftTopX  -> concave
         *
         * or
         *
         * leftMiddleX < leftTopX  -> convex
         */
        val leftControlX = leftTopX + (leftMiddleX - leftTopX) * kappa

        val path = Path().apply {
            // Top-left / beginning of left curve.
            moveTo(leftTopX, 0f)

            // Top.
            lineTo(width - rightRadius, 0f)

            // Top-right.
            quadraticTo(width, 0f, width, rightRadius)

            // Right.
            lineTo(width, height - rightRadius)

            // Bottom-right.
            quadraticTo(width, height, width - rightRadius, height)

            // Bottom.
            lineTo(leftTopX, height)

            /*
             * Lower left curve:
             *
             * concave at progress 0
             * convex at progress 1
             */
            cubicTo(
                leftControlX,
                height,
                leftMiddleX,
                centerY + verticalRadius * kappa,
                leftMiddleX,
                centerY
            )

            /*
             * Upper left curve.
             */
            cubicTo(leftMiddleX, centerY - verticalRadius * kappa, leftControlX, 0f, leftTopX, 0f)

            close()
        }

        return Outline.Generic(path)
    }
}

private fun lerp(
    start: Float,
    end: Float,
    fraction: Float
): Float = start + (end - start) * fraction
