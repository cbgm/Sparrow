package com.cbgm.sparrow.server.protocol

import kotlinx.serialization.Serializable

/**
 * One onion layer, sealed for a relay's [SparrowNodeDescriptor.relayKey] (context [RELAY_CONTEXT]) and sent as the
 * body of `POST /v1/relay`. It names either the next relay ([forward]) or the destination ([exit]). The relay seals its
 * reply with [replyKey], so the first relay knows the app's address but not the destination, and the second the
 * destination but not the address. Neither can read what the app sends or gets back.
 */
@Serializable
data class RelayLayer(
    val replyKey: ByteArray,
    val forward: RelayForward? = null,
    val exit: RelayExit? = null
) {
    override fun toString(): String = "RelayLayer(redacted)"
}

/** Pass [layer] (sealed for the next relay) to that relay's `POST /v1/relay` at [relayUrl]. */
@Serializable
data class RelayForward(
    val relayUrl: String,
    val layer: ByteArray
) {
    override fun toString(): String = "RelayForward(redacted)"
}

/** The request the last relay makes on the app's behalf; only Control Plane paths are allowed. */
@Serializable
data class RelayExit(
    val method: String,
    val url: String,
    val contentType: String? = null,
    val body: ByteArray? = null
) {
    override fun toString(): String = "RelayExit(redacted)"
}

/** What the destination answered; the last relay seals it for the app. */
@Serializable
data class RelayExitResponse(
    val status: Int,
    val contentType: String? = null,
    val body: ByteArray = byteArrayOf()
) {
    override fun toString(): String = "RelayExitResponse(status=$status)"
}

const val RELAY_CONTEXT = "sparrow-relay-v1"
const val ALIAS_CLAIM_CONTEXT = "sparrow-alias-claim-v1"
