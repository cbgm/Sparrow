package com.cbgm.sparrow.core.ui.component

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import kotlin.math.roundToInt

@Immutable
data class SparrowOverlayAnchor(
    val boundsInRoot: IntRect
)

interface SparrowOverlayScope {
    val anchorBounds: IntRect
}

fun Modifier.captureSparrowOverlayAnchor(
    onAnchorChanged: (SparrowOverlayAnchor) -> Unit
): Modifier =
    onGloballyPositioned { coordinates ->
        onAnchorChanged(
            SparrowOverlayAnchor(
                boundsInRoot = coordinates.boundsInRoot().toIntRect()
            )
        )
    }

@Composable
fun SparrowOverlay(
    anchor: SparrowOverlayAnchor,
    modifier: Modifier = Modifier,
    content: @Composable SparrowOverlayScope.() -> Unit
) {
    var originInRoot by remember { mutableStateOf(IntOffset.Zero) }

    Box(
        modifier =
            modifier
                .fillMaxSize()
                .onGloballyPositioned { coordinates ->
                    val bounds = coordinates.boundsInRoot()
                    originInRoot =
                        IntOffset(
                            x = bounds.left.roundToInt(),
                            y = bounds.top.roundToInt()
                        )
                }
    ) {
        val anchorBounds = anchor.boundsInRoot.relativeTo(originInRoot)
        val scope = remember(anchorBounds) { SparrowOverlayScopeImpl(anchorBounds) }
        scope.content()
    }
}

private class SparrowOverlayScopeImpl(
    override val anchorBounds: IntRect
) : SparrowOverlayScope

private fun IntRect.relativeTo(origin: IntOffset): IntRect =
    IntRect(
        left = left - origin.x,
        top = top - origin.y,
        right = right - origin.x,
        bottom = bottom - origin.y
    )

private fun Rect.toIntRect(): IntRect =
    IntRect(
        left = left.roundToInt(),
        top = top.roundToInt(),
        right = right.roundToInt(),
        bottom = bottom.roundToInt()
    )
