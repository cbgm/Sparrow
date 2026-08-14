package com.cbgm.sparrow.core.crypto.transport

data class EncryptedTransportPayload(
    val version: Int,
    val mode: TransportEncryptionMode,
    val payload: ByteArray
) {
    init {
        require(version > 0) {
            "Transport payload version must be positive"
        }

        require(payload.isNotEmpty()) {
            "Transport payload must not be empty"
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true

        if (other !is EncryptedTransportPayload) return false

        return version == other.version && mode == other.mode && payload.contentEquals(other.payload)
    }

    override fun hashCode(): Int {
        var result = version

        result = 31 * result + mode.hashCode()

        result = 31 * result + payload.contentHashCode()

        return result
    }
}
