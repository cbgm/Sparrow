package com.cbgm.sparrow.core.crypto.transport

/**
 * Pads a packet before it is sealed, so a node that stores or passes on the envelope learns only a size class, not
 * the size: `length (4 bytes, big-endian) || packet || zeros` up to 1, 4, 16, 64 or 256 KiB, beyond that up to the
 * next 64 KiB. A short "ok" and a long text look the same.
 */
internal object TransportPadding {
    private const val LENGTH_BYTES = 4
    private val SIZE_CLASSES = listOf(1, 4, 16, 64, 256).map { kibibytes -> kibibytes * 1_024 }
    private const val LARGE_STEP = 64 * 1_024

    fun pad(packet: ByteArray): ByteArray {
        val padded = ByteArray(paddedSize(LENGTH_BYTES + packet.size))
        for (index in 0 until LENGTH_BYTES) padded[index] = (packet.size ushr (Byte.SIZE_BITS * (LENGTH_BYTES - 1 - index))).toByte()
        packet.copyInto(padded, LENGTH_BYTES)
        return padded
    }

    /** The packet inside [padded]; fails when the length does not fit. */
    fun unpad(padded: ByteArray): ByteArray {
        require(padded.size >= LENGTH_BYTES) { "Padded transport payload is too short" }
        val length = (0 until LENGTH_BYTES).fold(0) { value, index -> (value shl Byte.SIZE_BITS) or (padded[index].toInt() and 0xFF) }
        require(length in 1..padded.size - LENGTH_BYTES) { "Padded transport payload has an invalid length" }
        return padded.copyOfRange(LENGTH_BYTES, LENGTH_BYTES + length)
    }

    fun paddedSize(size: Int): Int = SIZE_CLASSES.firstOrNull { sizeClass -> size <= sizeClass } ?: ((size + LARGE_STEP - 1) / LARGE_STEP * LARGE_STEP)
}
