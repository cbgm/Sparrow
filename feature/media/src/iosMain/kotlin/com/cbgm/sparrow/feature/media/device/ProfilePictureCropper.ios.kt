package com.cbgm.sparrow.feature.media.device

import com.cbgm.sparrow.feature.media.domain.model.ProfilePictureCropRegion
import kotlinx.cinterop.ExperimentalForeignApi
import platform.CoreGraphics.CGRectMake
import platform.CoreGraphics.CGSizeMake
import platform.UIKit.UIGraphicsBeginImageContextWithOptions
import platform.UIKit.UIGraphicsEndImageContext
import platform.UIKit.UIGraphicsGetImageFromCurrentImageContext
import platform.UIKit.UIImage
import platform.UIKit.UIImageJPEGRepresentation
import kotlin.math.min

@OptIn(ExperimentalForeignApi::class)
internal actual fun cropAndEncodeProfilePicture(
    sourceBytes: ByteArray,
    cropRegion: ProfilePictureCropRegion
): ByteArray? =
    runCatching {
        val image =
            requireNotNull(UIImage(data = sourceBytes.toNSData())) {
                "Profile picture could not be decoded"
            }
        val sourceWidth = image.size.width
        val sourceHeight = image.size.height

        require(sourceWidth > 0.0 && sourceHeight > 0.0) {
            "Profile picture has invalid dimensions"
        }

        val cropSize =
            cropRegion.sizeFractionOfShortEdge *
                min(sourceWidth, sourceHeight)
        val centerX = cropRegion.centerXFraction * sourceWidth
        val centerY = cropRegion.centerYFraction * sourceHeight
        val left =
            (centerX - cropSize / 2.0)
                .coerceIn(0.0, sourceWidth - cropSize)
        val top =
            (centerY - cropSize / 2.0)
                .coerceIn(0.0, sourceHeight - cropSize)
        val outputScale = PROFILE_PICTURE_SIZE / cropSize

        UIGraphicsBeginImageContextWithOptions(
            CGSizeMake(PROFILE_PICTURE_SIZE, PROFILE_PICTURE_SIZE),
            true,
            1.0
        )

        val cropped =
            try {
                image.drawInRect(
                    CGRectMake(
                        -left * outputScale,
                        -top * outputScale,
                        sourceWidth * outputScale,
                        sourceHeight * outputScale
                    )
                )
                requireNotNull(UIGraphicsGetImageFromCurrentImageContext()) {
                    "Profile picture crop could not be rendered"
                }
            } finally {
                UIGraphicsEndImageContext()
            }

        requireNotNull(
            UIImageJPEGRepresentation(
                cropped,
                PROFILE_PICTURE_JPEG_QUALITY
            )
        ) {
            "Profile picture could not be encoded"
        }.toByteArray()
    }.getOrNull()

private const val PROFILE_PICTURE_SIZE = 512.0
private const val PROFILE_PICTURE_JPEG_QUALITY = 0.88
