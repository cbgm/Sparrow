package com.cbgm.sparrow.feature.media.domain.model

import kotlin.math.min
import kotlin.math.roundToInt

internal data class ProfilePictureCropRegion(
    val centerXFraction: Float,
    val centerYFraction: Float,
    val sizeFractionOfShortEdge: Float
) {
    fun toProfilePictureSourceRect(
        sourceWidth: Int,
        sourceHeight: Int
    ): ProfilePictureSourceRect {
        val shortEdge = min(sourceWidth, sourceHeight).toFloat()
        val cropSize =
            (sizeFractionOfShortEdge * shortEdge)
                .roundToInt()
                .coerceIn(1, min(sourceWidth, sourceHeight))

        val centerX = centerXFraction * sourceWidth
        val centerY = centerYFraction * sourceHeight
        val halfCrop = cropSize / 2f

        val left =
            (centerX - halfCrop)
                .roundToInt()
                .coerceIn(0, sourceWidth - cropSize)
        val top =
            (centerY - halfCrop)
                .roundToInt()
                .coerceIn(0, sourceHeight - cropSize)

        return ProfilePictureSourceRect(
            left = left,
            top = top,
            size = cropSize
        )
    }
}

internal data class ProfilePictureSourceRect(
    val left: Int,
    val top: Int,
    val size: Int
)
