package com.cbgm.sparrow.feature.transport.relay

import kotlinx.serialization.Serializable

/**
 * One onion layer (same wire format as `RelayLayer` in `:server:protocol`), sealed for a relay's key with
 * [RELAY_CONTEXT]. It names the next relay ([forward]) or the destination ([exit]); the relay seals its reply with
 * [replyKey].
 */
@Serializable
internal class RelayLayer(
    val replyKey: ByteArray,
    val forward: RelayForward? = null,
    val exit: RelayExit? = null
)

@Serializable
internal class RelayForward(
    val relayUrl: String,
    val layer: ByteArray
)

@Serializable
class RelayExit(
    val method: String,
    val url: String,
    val contentType: String? = null,
    val body: ByteArray? = null
) {
    override fun toString(): String = "RelayExit(redacted)"
}

@Serializable
class RelayExitResponse(
    val status: Int,
    val contentType: String? = null,
    val body: ByteArray = byteArrayOf()
) {
    override fun toString(): String = "RelayExitResponse(status=$status)"
}

internal const val RELAY_CONTEXT = "sparrow-relay-v1"
