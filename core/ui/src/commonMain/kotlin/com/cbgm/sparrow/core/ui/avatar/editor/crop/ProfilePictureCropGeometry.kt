package com.cbgm.sparrow.core.ui.avatar.editor.crop

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import kotlin.math.max
import kotlin.math.min

internal fun profilePictureCropGeometry(
    viewportWidth: Float,
    viewportHeight: Float,
    sourceWidth: Int,
    sourceHeight: Int,
    zoom: Float,
    imageOffset: Offset = Offset.Zero
): ProfilePictureCropGeometry {
    val fitScale =
        min(
            viewportWidth / sourceWidth.toFloat(),
            viewportHeight / sourceHeight.toFloat()
        )
    val displayScale = fitScale * zoom
    val imageWidth = sourceWidth * displayScale
    val imageHeight = sourceHeight * displayScale
    val centeredImageLeft = (viewportWidth - imageWidth) / 2f
    val centeredImageTop = (viewportHeight - imageHeight) / 2f
    val imageLeft = centeredImageLeft + imageOffset.x
    val imageTop = centeredImageTop + imageOffset.y

    val fittedImageWidth = sourceWidth * fitScale
    val fittedImageHeight = sourceHeight * fitScale
    val cropDiameter = min(fittedImageWidth, fittedImageHeight) * CROP_FRAME_FRACTION

    return ProfilePictureCropGeometry(
        sourceWidth = sourceWidth,
        sourceHeight = sourceHeight,
        viewportWidth = viewportWidth,
        viewportHeight = viewportHeight,
        displayScale = displayScale,
        centeredImageLeft = centeredImageLeft,
        centeredImageTop = centeredImageTop,
        imageLeft = imageLeft,
        imageTop = imageTop,
        imageWidth = imageWidth,
        imageHeight = imageHeight,
        cropDiameter = cropDiameter
    )
}

internal data class ProfilePictureCropGeometry(
    private val sourceWidth: Int,
    private val sourceHeight: Int,
    private val viewportWidth: Float,
    private val viewportHeight: Float,
    val displayScale: Float,
    val centeredImageLeft: Float,
    val centeredImageTop: Float,
    val imageLeft: Float,
    val imageTop: Float,
    val imageWidth: Float,
    val imageHeight: Float,
    val cropDiameter: Float
) {
    val cropRadius: Float = cropDiameter / 2f

    val imageCenter: Offset =
        Offset(
            x = imageLeft + imageWidth / 2f,
            y = imageTop + imageHeight / 2f
        )

    val visibleImageRect: Rect =
        Rect(
            left = max(imageLeft, 0f),
            top = max(imageTop, 0f),
            right = min(imageLeft + imageWidth, viewportWidth),
            bottom = min(imageTop + imageHeight, viewportHeight)
        )

    fun clampCropCenter(center: Offset): Offset {
        val minX = visibleImageRect.left + cropRadius
        val maxX = visibleImageRect.right - cropRadius
        val minY = visibleImageRect.top + cropRadius
        val maxY = visibleImageRect.bottom - cropRadius

        return Offset(
            x = center.x.coerceIn(minX, maxX),
            y = center.y.coerceIn(minY, maxY)
        )
    }

    fun imageOffsetAdjustmentToCoverCrop(center: Offset): Offset {
        val cropLeft = center.x - cropRadius
        val cropTop = center.y - cropRadius
        val cropRight = center.x + cropRadius
        val cropBottom = center.y + cropRadius

        val horizontalAdjustment =
            when {
                imageLeft > cropLeft -> cropLeft - imageLeft
                imageLeft + imageWidth < cropRight ->
                    cropRight - (imageLeft + imageWidth)
                else -> 0f
            }

        val verticalAdjustment =
            when {
                imageTop > cropTop -> cropTop - imageTop
                imageTop + imageHeight < cropBottom ->
                    cropBottom - (imageTop + imageHeight)
                else -> 0f
            }

        return Offset(
            x = horizontalAdjustment,
            y = verticalAdjustment
        )
    }

    fun toCropRegion(center: Offset): ProfilePictureCropRegion {
        val sourceCenterX = (center.x - imageLeft) / displayScale
        val sourceCenterY = (center.y - imageTop) / displayScale
        val sourceCropSize = cropDiameter / displayScale
        val shortEdge = min(sourceWidth, sourceHeight).toFloat()

        return ProfilePictureCropRegion(
            centerXFraction = (sourceCenterX / sourceWidth).coerceIn(0f, 1f),
            centerYFraction = (sourceCenterY / sourceHeight).coerceIn(0f, 1f),
            sizeFractionOfShortEdge = (sourceCropSize / shortEdge).coerceIn(0f, 1f)
        )
    }
}

private const val CROP_FRAME_FRACTION = 0.68f
