package com.cbgm.sparrow.core.ui.avatar.editor.platform

import android.content.Context
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import java.io.ByteArrayOutputStream
import java.io.File
import java.nio.ByteBuffer
import kotlin.math.ceil

internal fun decodeProfilePictureBitmap(
    context: Context,
    uri: Uri
): Bitmap? =
    runCatching {
        decodeProfilePictureBitmap(
            ImageDecoder.createSource(context.contentResolver, uri)
        )
    }.getOrNull()

internal fun decodeProfilePictureBitmap(file: File): Bitmap? =
    runCatching {
        decodeProfilePictureBitmap(ImageDecoder.createSource(file))
    }.getOrNull()

internal fun decodeProfilePictureBitmap(bytes: ByteArray): Bitmap? =
    runCatching {
        decodeProfilePictureBitmap(
            ImageDecoder.createSource(ByteBuffer.wrap(bytes))
        )
    }.getOrNull()

internal fun encodeProfilePictureSource(bitmap: Bitmap): ByteArray =
    ByteArrayOutputStream().use { output ->
        check(bitmap.compress(Bitmap.CompressFormat.JPEG, SOURCE_JPEG_QUALITY, output)) {
            "Profile picture source could not be encoded"
        }
        output.toByteArray()
    }

private fun decodeProfilePictureBitmap(source: ImageDecoder.Source): Bitmap =
    ImageDecoder.decodeBitmap(source) { decoder, info, _ ->
        decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
        val largestDimension = maxOf(info.size.width, info.size.height)

        if (largestDimension > MAX_DECODE_DIMENSION) {
            decoder.setTargetSampleSize(
                ceil(largestDimension.toDouble() / MAX_DECODE_DIMENSION).toInt()
            )
        }
    }

private const val MAX_DECODE_DIMENSION = 2048
private const val SOURCE_JPEG_QUALITY = 92
