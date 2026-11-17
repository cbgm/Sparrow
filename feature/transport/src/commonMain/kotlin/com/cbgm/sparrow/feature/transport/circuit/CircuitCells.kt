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

    /** A part of a request or an answer; more parts follow. */
    const val STREAM_DATA = 6

    /** The last part of a request or an answer. */
    const val STREAM_END = 7

    /** The session node will not make the request. */
    const val STREAM_REFUSED = 8

    /** The session node could not make the request. */
    const val STREAM_FAILED = 9
}

/**
 * An HTTP request the session node makes for the app, as the gateway's `CircuitHttpRequest`:
 * `method length (1) || method || url length (2) || url || token length (2) || bearer token || content type length (1) ||
 * content type || body`.
 */
internal class CircuitHttpRequest(
    val method: String,
    val url: String,
    val bearerToken: String?,
    val contentType: String?,
    val body: ByteArray
) {
    fun encode(): ByteArray =
        field(method.encodeToByteArray(), 1) +
            field(url.encodeToByteArray(), 2) +
            field(bearerToken.orEmpty().encodeToByteArray(), 2) +
            field(contentType.orEmpty().encodeToByteArray(), 1) +
            body

    private fun field(
        value: ByteArray,
        lengthBytes: Int
    ): ByteArray {
        require(value.size < (1 shl (8 * lengthBytes))) { "Field too long" }
        return ByteArray(lengthBytes) { index -> (value.size ushr (8 * (lengthBytes - 1 - index))).toByte() } + value
    }
}

/** The answer: `status (2) || content type length (1) || content type || body`. */
internal class CircuitHttpResponse(
    val status: Int,
    val contentType: String?,
    val body: ByteArray
) {
    companion object {
        fun decode(bytes: ByteArray): CircuitHttpResponse? {
            if (bytes.size < 3) return null
            val typeLength = bytes[2].toInt() and 0xFF
            if (bytes.size < 3 + typeLength) return null
            return CircuitHttpResponse(
                status = readShort(bytes, 0),
                contentType = bytes.copyOfRange(3, 3 + typeLength).decodeToString().ifEmpty { null },
                body = bytes.copyOfRange(3 + typeLength, bytes.size)
            )
        }
    }
}

/** [data] cut into stream parts of at most [RelayPayload.MAXIMUM_DATA_BYTES]; at least one, possibly empty. */
internal fun streamParts(data: ByteArray): List<ByteArray> =
    (data.indices step RelayPayload.MAXIMUM_DATA_BYTES)
        .map { start -> data.copyOfRange(start, minOf(start + RelayPayload.MAXIMUM_DATA_BYTES, data.size)) }
        .ifEmpty { listOf(ByteArray(0)) }

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
