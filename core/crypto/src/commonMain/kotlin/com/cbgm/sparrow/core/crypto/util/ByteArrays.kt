package com.cbgm.sparrow.core.crypto.util

object ByteArrays {
    fun concatenate(vararg arrays: ByteArray): ByteArray {
        val totalSize = arrays.sumOf { array -> array.size }

        val result = ByteArray(size = totalSize)

        var offset = 0

        arrays.forEach { source ->
            source.copyInto(
                destination = result,
                destinationOffset = offset
            )

            offset += source.size
        }

        return result
    }

    fun encodeInt(value: Int): ByteArray {
        require(value >= 0) {
            "Encoded integer must not be negative"
        }

        return byteArrayOf(
            (value ushr 24 and 0xFF).toByte(),
            (value ushr 16 and 0xFF).toByte(),
            (value ushr 8 and 0xFF).toByte(),
            (value and 0xFF).toByte()
        )
    }

    fun encodeLong(value: Long): ByteArray {
        require(value >= 0L) {
            "Encoded long must not be negative"
        }

        return ByteArray(Long.SIZE_BYTES) { index ->
            (
                value ushr ((Long.SIZE_BYTES - index - 1) * Byte.SIZE_BITS) and 0xFF
            ).toByte()
        }
    }

    fun withLengthPrefix(value: ByteArray): ByteArray = concatenate(encodeInt(value.size), value)

    fun compareUnsigned(
        first: ByteArray,
        second: ByteArray
    ): Int {
        val sharedSize = minOf(first.size, second.size)

        for (index in 0 until sharedSize) {
            val firstValue = first[index].toInt() and 0xFF

            val secondValue = second[index].toInt() and 0xFF

            if (firstValue != secondValue) {
                return firstValue - secondValue
            }
        }

        return first.size - second.size
    }

    fun contentEqualsConstantTime(
        first: ByteArray,
        second: ByteArray
    ): Boolean {
        if (first.size != second.size) return false

        var difference = 0

        first.indices.forEach { index ->
            difference = difference or (first[index].toInt() xor second[index].toInt())
        }

        return difference == 0
    }
}
