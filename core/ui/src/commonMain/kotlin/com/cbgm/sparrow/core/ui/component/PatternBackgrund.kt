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
import com.cbgm.sparrow.core.ui.theme.Dimens
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
                    size = Dimens.PatternBackground.firstIconSize,
                    rotation = -12f,
                    offsetX = Dimens.PatternBackground.firstOffsetX,
                    offsetY = Dimens.PatternBackground.firstOffsetY
                ),
                PatternElement(
                    resource = Res.drawable.startup,
                    size = Dimens.PatternBackground.secondIconSize,
                    rotation = 8f,
                    offsetX = Dimens.PatternBackground.secondOffsetX,
                    offsetY = Dimens.PatternBackground.secondOffsetY
                ),
                PatternElement(
                    resource = Res.drawable.startup,
                    size = Dimens.PatternBackground.thirdIconSize,
                    rotation = 18f,
                    offsetX = Dimens.PatternBackground.thirdOffsetX,
                    offsetY = Dimens.PatternBackground.thirdOffsetY
                ),
                PatternElement(
                    resource = Res.drawable.startup,
                    size = Dimens.PatternBackground.fourthIconSize,
                    rotation = -20f,
                    offsetX = Dimens.PatternBackground.fourthOffsetX,
                    offsetY = Dimens.PatternBackground.fourthOffsetY
                ),
                PatternElement(
                    resource = Res.drawable.startup,
                    size = Dimens.PatternBackground.fifthIconSize,
                    rotation = 6f,
                    offsetX = Dimens.PatternBackground.fifthOffsetX,
                    offsetY = Dimens.PatternBackground.fifthOffsetY
                ),
                PatternElement(
                    resource = Res.drawable.startup,
                    size = Dimens.PatternBackground.sixthIconSize,
                    rotation = 24f,
                    offsetX = Dimens.PatternBackground.sixthOffsetX,
                    offsetY = Dimens.PatternBackground.sixthOffsetY
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
            val cellWidthPx = Dimens.PatternBackground.cellWidth.toPx()
            val cellHeightPx = Dimens.PatternBackground.cellHeight.toPx()

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
