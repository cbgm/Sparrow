package com.cbgm.sparrow.feature.media.device

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
    uri: Uri,
    maxDimension: Int = DEFAULT_MAX_DECODE_DIMENSION
): Bitmap? =
    runCatching {
        decodeProfilePictureBitmap(
            source = ImageDecoder.createSource(context.contentResolver, uri),
            maxDimension = maxDimension
        )
    }.getOrNull()

internal fun decodeProfilePictureBitmap(
    file: File,
    maxDimension: Int = DEFAULT_MAX_DECODE_DIMENSION
): Bitmap? =
    runCatching {
        decodeProfilePictureBitmap(
            source = ImageDecoder.createSource(file),
            maxDimension = maxDimension
        )
    }.getOrNull()

internal fun decodeProfilePictureBitmap(
    bytes: ByteArray,
    maxDimension: Int = DEFAULT_MAX_DECODE_DIMENSION
): Bitmap? =
    runCatching {
        decodeProfilePictureBitmap(
            source = ImageDecoder.createSource(ByteBuffer.wrap(bytes)),
            maxDimension = maxDimension
        )
    }.getOrNull()

internal fun encodeProfilePictureSource(bitmap: Bitmap): ByteArray =
    ByteArrayOutputStream().use { output ->
        check(bitmap.compress(Bitmap.CompressFormat.JPEG, SOURCE_JPEG_QUALITY, output)) {
            "Profile picture source could not be encoded"
        }
        output.toByteArray()
    }

private fun decodeProfilePictureBitmap(
    source: ImageDecoder.Source,
    maxDimension: Int
): Bitmap =
    ImageDecoder.decodeBitmap(source) { decoder, info, _ ->
        decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
        val largestDimension = maxOf(info.size.width, info.size.height)

        if (largestDimension > maxDimension) {
            decoder.setTargetSampleSize(
                ceil(largestDimension.toDouble() / maxDimension.toDouble()).toInt()
            )
        }
    }

private const val DEFAULT_MAX_DECODE_DIMENSION = 2048
private const val SOURCE_JPEG_QUALITY = 92
