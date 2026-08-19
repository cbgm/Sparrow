package com.cbgm.sparrow.core.ui.avatar.editor.crop

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import com.cbgm.sparrow.core.ui.theme.Alpha
import com.cbgm.sparrow.core.ui.theme.Dimens
import com.cbgm.sparrow.core.ui.theme.FunctionalColors
import kotlin.math.roundToInt

@Composable
internal fun ProfilePictureCropCanvas(
    image: ImageBitmap,
    onCropRegionChanged: (ProfilePictureCropRegion) -> Unit,
    modifier: Modifier = Modifier
) {
    var viewportSize by remember { mutableStateOf(IntSize.Zero) }
    var cropCenter by remember(image) { mutableStateOf<Offset?>(null) }
    var zoom by remember(image) { mutableFloatStateOf(MIN_ZOOM) }
    var imageOffset by remember(image) { mutableStateOf(Offset.Zero) }
    val currentOnCropRegionChanged by rememberUpdatedState(onCropRegionChanged)
    val scrimColor = FunctionalColors.MediaBackground
    val guideColor = FunctionalColors.MediaForeground

    val geometry =
        remember(
            viewportSize,
            image.width,
            image.height,
            zoom,
            imageOffset
        ) {
            viewportSize
                .takeIf { it.width > 0 && it.height > 0 }
                ?.let {
                    profilePictureCropGeometry(
                        viewportWidth = it.width.toFloat(),
                        viewportHeight = it.height.toFloat(),
                        sourceWidth = image.width,
                        sourceHeight = image.height,
                        zoom = zoom,
                        imageOffset = imageOffset
                    )
                }
        }

    LaunchedEffect(geometry) {
        val activeGeometry = geometry ?: return@LaunchedEffect
        val center = activeGeometry.clampCropCenter(cropCenter ?: activeGeometry.imageCenter)
        cropCenter = center
        currentOnCropRegionChanged(activeGeometry.toCropRegion(center))
    }

    Canvas(
        modifier =
            modifier
                .onSizeChanged { viewportSize = it }
                .profilePictureCropGestures(
                    image = image,
                    currentZoom = { zoom },
                    currentImageOffset = { imageOffset },
                    currentCropCenter = { cropCenter },
                    onTransform = { updatedZoom, updatedOffset, updatedCenter ->
                        zoom = updatedZoom
                        imageOffset = updatedOffset
                        cropCenter = updatedCenter
                        val activeGeometry =
                            profilePictureCropGeometry(
                                viewportWidth = viewportSize.width.toFloat(),
                                viewportHeight = viewportSize.height.toFloat(),
                                sourceWidth = image.width,
                                sourceHeight = image.height,
                                zoom = updatedZoom,
                                imageOffset = updatedOffset
                            )
                        currentOnCropRegionChanged(
                            activeGeometry.toCropRegion(updatedCenter)
                        )
                    }
                )
    ) {
        val activeGeometry = geometry ?: return@Canvas
        val center =
            activeGeometry.clampCropCenter(
                cropCenter ?: activeGeometry.imageCenter
            )

        drawImage(
            image = image,
            dstOffset =
                IntOffset(
                    activeGeometry.imageLeft.roundToInt(),
                    activeGeometry.imageTop.roundToInt()
                ),
            dstSize =
                IntSize(
                    activeGeometry.imageWidth.roundToInt().coerceAtLeast(1),
                    activeGeometry.imageHeight.roundToInt().coerceAtLeast(1)
                )
        )

        val mask =
            Path().apply {
                fillType = PathFillType.EvenOdd
                addRect(Rect(0f, 0f, size.width, size.height))
                addOval(
                    Rect(
                        left = center.x - activeGeometry.cropRadius,
                        top = center.y - activeGeometry.cropRadius,
                        right = center.x + activeGeometry.cropRadius,
                        bottom = center.y + activeGeometry.cropRadius
                    )
                )
            }

        drawPath(
            path = mask,
            color = scrimColor.copy(alpha = Alpha.ProfilePictureCropScreen.scrim)
        )
        drawCircle(
            color = guideColor,
            radius = activeGeometry.cropRadius,
            center = center,
            style = Stroke(width = Dimens.ProfilePictureCropScreen.guideStrokeWidth.toPx())
        )
    }
}

private fun Modifier.profilePictureCropGestures(
    image: ImageBitmap,
    currentZoom: () -> Float,
    currentImageOffset: () -> Offset,
    currentCropCenter: () -> Offset?,
    onTransform: (
        zoom: Float,
        imageOffset: Offset,
        cropCenter: Offset
    ) -> Unit
): Modifier =
    pointerInput(image.width, image.height) {
        awaitEachGesture {
            awaitFirstDown(requireUnconsumed = false)
            var multiTouchStarted = false

            while (true) {
                val event = awaitPointerEvent()
                val pressed = event.changes.filter { it.pressed }

                if (pressed.isEmpty()) {
                    break
                }

                val zoom = currentZoom()
                val imageOffset = currentImageOffset()
                val currentGeometry =
                    profilePictureCropGeometry(
                        viewportWidth = size.width.toFloat(),
                        viewportHeight = size.height.toFloat(),
                        sourceWidth = image.width,
                        sourceHeight = image.height,
                        zoom = zoom,
                        imageOffset = imageOffset
                    )
                val cropCenter =
                    currentGeometry.clampCropCenter(
                        currentCropCenter() ?: currentGeometry.imageCenter
                    )

                if (pressed.size >= 2) {
                    multiTouchStarted = true

                    val zoomChange = event.calculateZoom()
                    val panChange = event.calculatePan()
                    val nextZoom =
                        if (zoomChange.isFinite() && zoomChange > 0f) {
                            (zoom * zoomChange)
                                .coerceIn(MIN_ZOOM, MAX_ZOOM)
                        } else {
                            zoom
                        }

                    val proposedOffset =
                        if (nextZoom <= MIN_ZOOM) {
                            Offset.Zero
                        } else {
                            zoomedImageOffset(
                                currentGeometry = currentGeometry,
                                cropCenter = cropCenter,
                                currentZoom = zoom,
                                nextZoom = nextZoom,
                                panChange = panChange,
                                viewportWidth = size.width.toFloat(),
                                viewportHeight = size.height.toFloat(),
                                sourceWidth = image.width,
                                sourceHeight = image.height
                            )
                        }

                    val proposedGeometry =
                        profilePictureCropGeometry(
                            viewportWidth = size.width.toFloat(),
                            viewportHeight = size.height.toFloat(),
                            sourceWidth = image.width,
                            sourceHeight = image.height,
                            zoom = nextZoom,
                            imageOffset = proposedOffset
                        )
                    val clampedOffset =
                        if (nextZoom <= MIN_ZOOM) {
                            Offset.Zero
                        } else {
                            proposedOffset +
                                proposedGeometry.imageOffsetAdjustmentToCoverCrop(cropCenter)
                        }
                    val updatedGeometry =
                        profilePictureCropGeometry(
                            viewportWidth = size.width.toFloat(),
                            viewportHeight = size.height.toFloat(),
                            sourceWidth = image.width,
                            sourceHeight = image.height,
                            zoom = nextZoom,
                            imageOffset = clampedOffset
                        )
                    val updatedCenter =
                        updatedGeometry.clampCropCenter(cropCenter)

                    onTransform(
                        nextZoom,
                        clampedOffset,
                        updatedCenter
                    )
                    event.changes.forEach { it.consume() }
                } else if (!multiTouchStarted) {
                    val change = pressed.first()

                    if (currentGeometry.visibleImageRect.contains(change.position)) {
                        change.consume()
                        onTransform(
                            zoom,
                            imageOffset,
                            currentGeometry.clampCropCenter(change.position)
                        )
                    }
                }
            }
        }
    }

private fun zoomedImageOffset(
    currentGeometry: ProfilePictureCropGeometry,
    cropCenter: Offset,
    currentZoom: Float,
    nextZoom: Float,
    panChange: Offset,
    viewportWidth: Float,
    viewportHeight: Float,
    sourceWidth: Int,
    sourceHeight: Int
): Offset {
    val zoomRatio =
        if (currentZoom > 0f) {
            nextZoom / currentZoom
        } else {
            1f
        }

    val desiredImageLeft =
        cropCenter.x -
            (cropCenter.x - currentGeometry.imageLeft) * zoomRatio +
            panChange.x
    val desiredImageTop =
        cropCenter.y -
            (cropCenter.y - currentGeometry.imageTop) * zoomRatio +
            panChange.y

    val centeredGeometry =
        profilePictureCropGeometry(
            viewportWidth = viewportWidth,
            viewportHeight = viewportHeight,
            sourceWidth = sourceWidth,
            sourceHeight = sourceHeight,
            zoom = nextZoom
        )

    return Offset(
        x = desiredImageLeft - centeredGeometry.centeredImageLeft,
        y = desiredImageTop - centeredGeometry.centeredImageTop
    )
}

private const val MIN_ZOOM = 1f
private const val MAX_ZOOM = 6f
