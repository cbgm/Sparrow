@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package com.cbgm.sparrow.feature.media.device

import kotlinx.cinterop.addressOf
import kotlinx.cinterop.useContents
import kotlinx.cinterop.usePinned
import platform.CoreGraphics.CGContextSetInterpolationQuality
import platform.CoreGraphics.CGImageGetWidth
import platform.CoreGraphics.CGImageRelease
import platform.CoreGraphics.CGRectMake
import platform.CoreGraphics.CGSizeMake
import platform.CoreGraphics.kCGInterpolationNone
import platform.CoreImage.CIContext
import platform.CoreImage.CIFilter
import platform.Foundation.NSData
import platform.Foundation.NSString
import platform.Foundation.setValue
import platform.UIKit.NSFontAttributeName
import platform.UIKit.NSForegroundColorAttributeName
import platform.UIKit.UIColor
import platform.UIKit.UIFont
import platform.UIKit.UIGraphicsBeginImageContextWithOptions
import platform.UIKit.UIGraphicsEndImageContext
import platform.UIKit.UIGraphicsGetCurrentContext
import platform.UIKit.UIGraphicsGetImageFromCurrentImageContext
import platform.UIKit.UIImage
import platform.UIKit.UIImagePNGRepresentation
import platform.UIKit.UIRectFill
import platform.UIKit.drawInRect
import platform.UIKit.sizeWithAttributes
import platform.posix.memcpy

actual fun renderQrCode(request: QrCodeRenderRequest): ByteArray {
    val bytes = request.content.encodeToByteArray()
    val data = bytes.usePinned { NSData.create(bytes = it.addressOf(0), length = bytes.size.toULong()) }
    val filter = requireNotNull(CIFilter.filterWithName("CIQRCodeGenerator"))
    filter.setValue(data, forKey = "inputMessage")
    filter.setValue(
        when (request.errorCorrection) {
            QrErrorCorrection.LOW -> "L"
            QrErrorCorrection.MEDIUM -> "M"
            QrErrorCorrection.QUARTILE -> "Q"
            QrErrorCorrection.HIGH -> "H"
        },
        forKey = "inputCorrectionLevel"
    )
    val output = requireNotNull(filter.outputImage)
    val context = CIContext.contextWithOptions(null)
    val cgImage = requireNotNull(context.createCGImage(output, output.extent))
    try {
        val modules = CGImageGetWidth(cgImage).toInt()
        val scale = request.size / (modules + 2 * request.margin)
        require(scale > 0) { "QR image size is too small" }
        val drawSize = modules * scale
        val quietSpace = (request.size - drawSize) / 2
        val layout = QrCodeImageLayout(request.size)
        val attributes = mapOf<Any?, Any?>(
            NSFontAttributeName to UIFont.systemFontOfSize(layout.textSize.toDouble()),
            NSForegroundColorAttributeName to UIColor.blackColor
        )
        val lines = layout.wrap(request.caption) {
            (it as NSString).sizeWithAttributes(attributes).useContents { width }
        }
        val hasCaption = request.caption.isNotEmpty()
        val width = if (hasCaption) layout.width else request.size
        val height = if (hasCaption) layout.height(lines.size) else request.size
        val qrLeft = if (hasCaption) layout.qrLeft else 0
        val qrTop = if (hasCaption) layout.qrTop else 0
        UIGraphicsBeginImageContextWithOptions(CGSizeMake(width.toDouble(), height.toDouble()), true, 1.0)
        try {
            UIColor.whiteColor.setFill()
            UIRectFill(CGRectMake(0.0, 0.0, width.toDouble(), height.toDouble()))
            CGContextSetInterpolationQuality(requireNotNull(UIGraphicsGetCurrentContext()), kCGInterpolationNone)
            UIImage.imageWithCGImage(cgImage).drawInRect(
                CGRectMake(
                    (qrLeft + quietSpace).toDouble(),
                    (qrTop + quietSpace).toDouble(),
                    drawSize.toDouble(),
                    drawSize.toDouble()
                )
            )
            lines.forEachIndexed { index, line ->
                (line as NSString).drawInRect(
                    CGRectMake(
                        layout.textLeft.toDouble(),
                        (layout.textTop + index * layout.lineHeight).toDouble(),
                        layout.textWidth.toDouble(),
                        layout.lineHeight.toDouble()
                    ),
                    withAttributes = attributes
                )
            }
            val png = requireNotNull(UIImagePNGRepresentation(requireNotNull(UIGraphicsGetImageFromCurrentImageContext())))
            require(png.length <= Int.MAX_VALUE.toULong())
            return ByteArray(png.length.toInt()).also { result ->
                result.usePinned { memcpy(it.addressOf(0), png.bytes, png.length) }
            }
        } finally {
            UIGraphicsEndImageContext()
        }
    } finally {
        CGImageRelease(cgImage)
    }
}
