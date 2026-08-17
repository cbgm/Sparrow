package com.cbgm.sparrow.feature.settings.presentation.profile.platform

import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
import platform.CoreGraphics.CGRectMake
import platform.CoreGraphics.CGSizeMake
import platform.Foundation.NSData
import platform.UIKit.UIImage
import platform.UIKit.UIImageJPEGRepresentation
import platform.UIKit.UIGraphicsBeginImageContextWithOptions
import platform.UIKit.UIGraphicsEndImageContext
import platform.UIKit.UIGraphicsGetImageFromCurrentImageContext
import platform.posix.memcpy

@OptIn(ExperimentalForeignApi::class)
internal fun UIImage.normalizedProfilePictureBytes(): ByteArray? {
    val width = size.width
    val height = size.height

    if (width <= 0.0 || height <= 0.0) {
        return null
    }

    UIGraphicsBeginImageContextWithOptions(
        CGSizeMake(width, height),
        true,
        1.0
    )

    val normalized =
        try {
            drawInRect(
                CGRectMake(
                    0.0,
                    0.0,
                    width,
                    height
                )
            )
            UIGraphicsGetImageFromCurrentImageContext()
        } finally {
            UIGraphicsEndImageContext()
        }

    return normalized
        ?.let { UIImageJPEGRepresentation(it, SOURCE_JPEG_QUALITY) }
        ?.toByteArray()
}

@OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)
internal fun ByteArray.toNSData(): NSData {
    require(isNotEmpty()) { "Image bytes must not be empty" }

    return usePinned { pinned ->
        NSData.create(
            bytes = pinned.addressOf(0),
            length = size.toULong()
        )
    }
}

@OptIn(ExperimentalForeignApi::class)
internal fun NSData.toByteArray(): ByteArray {
    if (length == 0UL) {
        return ByteArray(0)
    }

    return ByteArray(length.toInt()).also { result ->
        result.usePinned { pinned ->
            memcpy(
                pinned.addressOf(0),
                bytes,
                length
            )
        }
    }
}

private const val SOURCE_JPEG_QUALITY = 0.92
