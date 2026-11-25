package com.cbgm.sparrow.feature.transport.data.route.relay

import kotlinx.serialization.Serializable

/**
 * One onion layer (same wire format as `RelayLayer` in `:server:protocol`), sealed for a relay's key with
 * [RELAY_CONTEXT]. It names the next relay ([forward]) or the destination ([exit]); the relay seals its reply with
 * [replyKey].
 */
@Serializable
internal class RelayLayerDto(
    val replyKey: ByteArray,
    val forward: RelayForwardDto? = null,
    val exit: RelayExitDto? = null
)

@Serializable
internal class RelayForwardDto(
    val relayUrl: String,
    val layer: ByteArray
)

@Serializable
internal class RelayExitDto(
    val method: String,
    val url: String,
    val contentType: String? = null,
    val body: ByteArray? = null
) {
    override fun toString(): String = "RelayExitDto(redacted)"
}

@Serializable
internal class RelayExitResponseDto(
    val status: Int,
    val contentType: String? = null,
    val body: ByteArray = byteArrayOf()
) {
    override fun toString(): String = "RelayExitResponseDto(status=$status)"
}

internal const val RELAY_CONTEXT = "sparrow-relay-v1"
