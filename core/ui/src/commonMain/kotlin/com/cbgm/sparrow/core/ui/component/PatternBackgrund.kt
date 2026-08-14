package com.cbgm.sparrow.core.ui.component

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.cbgm.sparrow.resources.Res
import com.cbgm.sparrow.resources.startup
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource

@Immutable
private data class PatternElement(
    val resource: DrawableResource,
    val size: Dp,
    val rotation: Float,
    val offsetX: Dp,
    val offsetY: Dp
)

@Composable
fun PatternBackground(
    modifier: Modifier = Modifier,
    backgroundColor: Color,
    alpha: Float = 0.06f
) {
    val patternElements =
        remember {
            listOf(
                PatternElement(
                    resource = Res.drawable.startup,
                    size = 30.dp,
                    rotation = -12f,
                    offsetX = 10.dp,
                    offsetY = 12.dp
                ),
                PatternElement(
                    resource = Res.drawable.startup,
                    size = 44.dp,
                    rotation = 8f,
                    offsetX = 72.dp,
                    offsetY = 4.dp
                ),
                PatternElement(
                    resource = Res.drawable.startup,
                    size = 26.dp,
                    rotation = 18f,
                    offsetX = 142.dp,
                    offsetY = 34.dp
                ),
                PatternElement(
                    resource = Res.drawable.startup,
                    size = 38.dp,
                    rotation = -20f,
                    offsetX = 26.dp,
                    offsetY = 86.dp
                ),
                PatternElement(
                    resource = Res.drawable.startup,
                    size = 32.dp,
                    rotation = 6f,
                    offsetX = 104.dp,
                    offsetY = 104.dp
                ),
                PatternElement(
                    resource = Res.drawable.startup,
                    size = 22.dp,
                    rotation = 24f,
                    offsetX = 156.dp,
                    offsetY = 112.dp
                )
            )
        }

    val painters =
        patternElements.map { element ->
            painterResource(element.resource)
        }

    Box(
        modifier =
            modifier
                .fillMaxSize()
                .background(backgroundColor)
    ) {
        Canvas(
            modifier = Modifier.matchParentSize()
        ) {
            val cellWidthPx = 190.dp.toPx()
            val cellHeightPx = 150.dp.toPx()

            var row = 0
            var cellY = -cellHeightPx

            while (cellY < size.height + cellHeightPx) {
                var cellX =
                    if (row % 2 == 0) {
                        -cellWidthPx
                    } else {
                        -cellWidthPx / 2f
                    }

                while (cellX < size.width + cellWidthPx) {
                    patternElements.forEachIndexed { index, element ->
                        val painter = painters[index]

                        val elementSizePx = element.size.toPx()
                        val offsetXPx = element.offsetX.toPx()
                        val offsetYPx = element.offsetY.toPx()

                        withTransform({
                            translate(
                                left = cellX + offsetXPx,
                                top = cellY + offsetYPx
                            )

                            rotate(
                                degrees = element.rotation,
                                pivot =
                                    Offset(
                                        x = elementSizePx / 2f,
                                        y = elementSizePx / 2f
                                    )
                            )
                        }) {
                            with(painter) {
                                draw(
                                    size =
                                        Size(
                                            width = elementSizePx,
                                            height = elementSizePx
                                        ),
                                    alpha = alpha
                                )
                            }
                        }
                    }

                    cellX += cellWidthPx
                }

                cellY += cellHeightPx
                row++
            }
        }
    }
}
