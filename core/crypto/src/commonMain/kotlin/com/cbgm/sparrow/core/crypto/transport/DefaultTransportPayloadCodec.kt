package com.cbgm.sparrow.core.crypto.transport

import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

class DefaultTransportPayloadCodec : TransportPayloadCodec {
    @OptIn(ExperimentalEncodingApi::class)
    override fun encode(payload: EncryptedTransportPayload): String {
        val encodedPayload = Base64.Default.encode(payload.payload)

        return buildString {
            append(PREFIX)
            append(':')
            append(payload.version)
            append(':')
            append(payload.mode.name)
            append(':')
            append(encodedPayload)
        }
    }

    @OptIn(ExperimentalEncodingApi::class)
    override fun decode(encoded: String): Result<EncryptedTransportPayload> =
        runCatching {
            val parts: List<String> = encoded.split(':', limit = 4)

            require(parts.size == 4) {
                "Invalid transport payload"
            }

            val prefix: String = parts[0]

            require(prefix == PREFIX) {
                "Unsupported transport payload prefix"
            }

            val version: Int = parts[1].toInt()

            val mode: TransportEncryptionMode = TransportEncryptionMode.valueOf(parts[2])

            val payload: ByteArray = Base64.decode(parts[3])

            EncryptedTransportPayload(
                version = version,
                mode = mode,
                payload = payload
            )
        }

    private companion object {
        const val PREFIX = "scmsg"
    }
}
