package com.cbgm.sparrow.feature.search.data.embedding

internal object EmbeddingCodec {
    fun encode(values: FloatArray): ByteArray {
        val result = ByteArray(values.size * Float.SIZE_BYTES)
        values.forEachIndexed { index, value ->
            val bits = value.toBits()
            val offset = index * Float.SIZE_BYTES
            result[offset] = (bits ushr 24).toByte()
            result[offset + 1] = (bits ushr 16).toByte()
            result[offset + 2] = (bits ushr 8).toByte()
            result[offset + 3] = bits.toByte()
        }
        return result
    }

    fun decode(bytes: ByteArray): FloatArray {
        require(bytes.size % Float.SIZE_BYTES == 0) { "Invalid embedding byte count" }
        return FloatArray(bytes.size / Float.SIZE_BYTES) { index ->
            val offset = index * Float.SIZE_BYTES
            val bits =
                ((bytes[offset].toInt() and 0xff) shl 24) or
                    ((bytes[offset + 1].toInt() and 0xff) shl 16) or
                    ((bytes[offset + 2].toInt() and 0xff) shl 8) or
                    (bytes[offset + 3].toInt() and 0xff)
            Float.fromBits(bits)
        }
    }
}
