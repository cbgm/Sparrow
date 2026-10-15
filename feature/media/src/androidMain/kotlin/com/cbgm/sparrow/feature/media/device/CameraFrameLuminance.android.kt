package com.cbgm.sparrow.feature.media.device

import androidx.camera.core.ImageProxy

internal class UprightLuminance(
    val bytes: ByteArray,
    val width: Int,
    val height: Int
)

internal fun ImageProxy.uprightLuminance(): UprightLuminance =
    rotateLuminance(
        source = copyLuminancePlane(),
        width = width,
        height = height,
        rotationDegrees = imageInfo.rotationDegrees
    )

private fun ImageProxy.copyLuminancePlane(): ByteArray {
    val plane = planes.first()

    val buffer = plane.buffer

    val rowStride = plane.rowStride

    val pixelStride = plane.pixelStride

    val output = ByteArray(width * height)

    buffer.rewind()

    var outputIndex = 0

    for (row in 0 until height) {
        val rowStart = row * rowStride

        for (column in 0 until width) {
            val bufferIndex = rowStart + column * pixelStride

            if (bufferIndex < buffer.limit()) {
                output[outputIndex] = buffer.get(bufferIndex)
            }

            outputIndex += 1
        }
    }

    return output
}

private fun rotateLuminance(
    source: ByteArray,
    width: Int,
    height: Int,
    rotationDegrees: Int
): UprightLuminance =
    when (rotationDegrees) {
        90 -> {
            val rotated = ByteArray(source.size)

            var destinationIndex = 0

            for (x in 0 until width) {
                for (y in height - 1 downTo 0) {
                    rotated[destinationIndex++] = source[y * width + x]
                }
            }

            UprightLuminance(bytes = rotated, width = height, height = width)
        }

        180 -> {
            val rotated = ByteArray(source.size)

            source.indices.forEach { index ->
                rotated[source.lastIndex - index] = source[index]
            }

            UprightLuminance(bytes = rotated, width = width, height = height)
        }

        270 -> {
            val rotated = ByteArray(source.size)

            var destinationIndex = 0

            for (x in width - 1 downTo 0) {
                for (y in 0 until height) {
                    rotated[destinationIndex++] = source[y * width + x]
                }
            }

            UprightLuminance(bytes = rotated, width = height, height = width)
        }

        else -> {
            UprightLuminance(bytes = source, width = width, height = height)
        }
    }
