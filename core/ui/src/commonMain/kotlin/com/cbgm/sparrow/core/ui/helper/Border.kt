package com.cbgm.sparrow.core.ui.helper

import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

data class BorderSides(
    val top: Boolean = true,
    val bottom: Boolean = true,
    val left: Boolean = true,
    val right: Boolean = true
)

fun Modifier.drawShapeBorder(
    shape: Shape,
    color: Color,
    strokeWidth: Dp = 1.dp,
    sides: BorderSides = BorderSides()
): Modifier = this.drawBehind {
    val thickness = strokeWidth.toPx()

    val clipLeft = if (sides.left) -thickness else thickness
    val clipTop = if (sides.top) -thickness else thickness
    val clipRight = size.width + if (sides.right) thickness else -thickness
    val clipBottom = size.height + if (sides.bottom) thickness else -thickness

    clipRect(
        left = clipLeft,
        top = clipTop,
        right = clipRight,
        bottom = clipBottom
    ) {
        val outline = shape.createOutline(size, layoutDirection, this)

        drawOutline(
            outline = outline,
            color = color,
            style = Stroke(width = thickness * 2)
        )
    }
}
