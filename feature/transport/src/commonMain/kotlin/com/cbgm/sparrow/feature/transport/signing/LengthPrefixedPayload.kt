package com.cbgm.sparrow.feature.transport.signing

/**
 * Builds the bytes of a signed protocol payload. Each field is length-prefixed (4-byte big-endian) so
 * different field values can never produce the same bytes. Must match the server's ProtocolSignatures.
 */
internal fun lengthPrefixed(vararg fields: ByteArray): ByteArray {
    val output = ByteArray(fields.sumOf { field -> LENGTH_PREFIX_BYTES + field.size })
    var offset = 0
    fields.forEach { field ->
        for (shift in LENGTH_PREFIX_SHIFTS) {
            output[offset++] = (field.size ushr shift).toByte()
        }
        field.copyInto(output, destinationOffset = offset)
        offset += field.size
    }
    return output
}

/** 8-byte big-endian encoding, as java.nio.ByteBuffer.putLong writes it on the server. */
internal fun Long.toBigEndianBytes(): ByteArray = ByteArray(Long.SIZE_BYTES) { index -> (this ushr (56 - 8 * index)).toByte() }

private const val LENGTH_PREFIX_BYTES = 4
private val LENGTH_PREFIX_SHIFTS = intArrayOf(24, 16, 8, 0)
