package com.cbgm.sparrow.feature.transport.circuit

/** A fixed-size circuit cell, as the gateway's `CircuitCell`: `circuit ID (4) || command (1) || body`, [SIZE] bytes. */
internal class CircuitCell(
    val circuitId: Int,
    val command: Int,
    val body: ByteArray
) {
    fun encode(): ByteArray =
        ByteArray(SIZE).also { bytes ->
            writeInt(bytes, 0, circuitId)
            bytes[4] = command.toByte()
            body.copyInto(bytes, destinationOffset = HEADER_BYTES, endIndex = minOf(body.size, BODY_BYTES))
        }

    companion object {
        const val SIZE = 1024
        const val HEADER_BYTES = 5
        const val BODY_BYTES = SIZE - HEADER_BYTES
        const val CREATE = 1
        const val CREATED = 2
        const val RELAY = 3
        const val DESTROY = 4
        const val PADDING = 5

        fun decode(frame: ByteArray): CircuitCell? =
            frame.takeIf { it.size == SIZE }?.let { bytes ->
                CircuitCell(readInt(bytes, 0), bytes[4].toInt(), bytes.copyOfRange(HEADER_BYTES, SIZE))
            }
    }
}

/** What the last hop reads: `command (1) || stream (2) || length (2) || data || zeros`, as the gateway's. */
internal class RelayPayload(
    val command: Int,
    val stream: Int,
    val data: ByteArray
) {
    fun encode(): ByteArray {
        require(data.size <= MAXIMUM_DATA_BYTES) { "Relay data must fit one cell" }
        return ByteArray(PLAINTEXT_BYTES).also { bytes ->
            bytes[0] = command.toByte()
            writeShort(bytes, 1, stream)
            writeShort(bytes, 3, data.size)
            data.copyInto(bytes, destinationOffset = HEADER_BYTES)
        }
    }

    companion object {
        const val TAG_BYTES = 16
        const val HEADER_BYTES = 5

        /** The cell body less the authentication tag of the last hop's seal. */
        const val PLAINTEXT_BYTES = CircuitCell.BODY_BYTES - TAG_BYTES
        const val MAXIMUM_DATA_BYTES = PLAINTEXT_BYTES - HEADER_BYTES

        fun decode(plaintext: ByteArray): RelayPayload? {
            if (plaintext.size != PLAINTEXT_BYTES) return null
            val length = readShort(plaintext, 3)
            return plaintext
                .takeIf { length <= MAXIMUM_DATA_BYTES }
                ?.let { RelayPayload(it[0].toInt(), readShort(it, 1), it.copyOfRange(HEADER_BYTES, HEADER_BYTES + length)) }
        }
    }
}

internal object RelayCommand {
    const val EXTEND = 1
    const val EXTENDED = 2
    const val EXTEND_FAILED = 3
    const val PING = 4
    const val PONG = 5
}

/** EXTEND data: `url length (2) || url (UTF-8) || the app's ephemeral key for the next hop (32)`. */
internal fun extendRequest(
    url: String,
    ephemeralPublicKey: ByteArray
): ByteArray {
    val urlBytes = url.encodeToByteArray()
    return ByteArray(2 + urlBytes.size + ephemeralPublicKey.size).also { bytes ->
        writeShort(bytes, 0, urlBytes.size)
        urlBytes.copyInto(bytes, destinationOffset = 2)
        ephemeralPublicKey.copyInto(bytes, destinationOffset = 2 + urlBytes.size)
    }
}

private fun writeInt(
    bytes: ByteArray,
    offset: Int,
    value: Int
) {
    (0 until 4).forEach { index -> bytes[offset + index] = (value ushr (8 * (3 - index))).toByte() }
}

private fun readInt(
    bytes: ByteArray,
    offset: Int
): Int = (0 until 4).fold(0) { value, index -> (value shl 8) or (bytes[offset + index].toInt() and 0xFF) }

private fun writeShort(
    bytes: ByteArray,
    offset: Int,
    value: Int
) {
    bytes[offset] = (value ushr 8).toByte()
    bytes[offset + 1] = value.toByte()
}

private fun readShort(
    bytes: ByteArray,
    offset: Int
): Int = ((bytes[offset].toInt() and 0xFF) shl 8) or (bytes[offset + 1].toInt() and 0xFF)
