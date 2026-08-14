package com.cbgm.sparrow.core.crypto.transport

sealed interface DecodedTransportMessage {
    data class Readable(
        val plaintext: ByteArray,
        val mode: TransportEncryptionMode
    ) : DecodedTransportMessage {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true

            if (other !is Readable) return false

            return mode == other.mode && plaintext.contentEquals(other.plaintext)
        }

        override fun hashCode(): Int {
            var result = plaintext.contentHashCode()

            result = 31 * result + mode.hashCode()

            return result
        }
    }

    /**
     * The outer packet could not be decoded.
     *
     * Examples:
     * - invalid prefix
     * - invalid Base64
     * - invalid mode
     * - unsupported structure
     */
    data class InvalidPacket(
        val cause: Throwable? = null
    ) : DecodedTransportMessage

    /**
     * A PLAINTEXT packet contained invalid UTF-8.
     *
     * No sealed-box decryption was attempted.
     */
    data class InvalidPlaintext(
        val cause: Throwable? = null
    ) : DecodedTransportMessage

    /**
     * A SEALED_BOX packet could not be decrypted.
     *
     * The ciphertext must never be interpreted as plaintext.
     */
    data class DecryptionFailed(
        val cause: Throwable? = null
    ) : DecodedTransportMessage
}
