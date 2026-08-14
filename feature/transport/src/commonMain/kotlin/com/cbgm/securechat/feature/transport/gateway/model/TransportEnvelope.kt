package com.cbgm.securechat.feature.transport.gateway.model

import kotlinx.serialization.Serializable

/**
 * Opaque message routed through the gateway.
 *
 * The gateway sees routing metadata but must never decrypt or interpret
 * the SecureChat transport payload.
 */
@Serializable
data class TransportEnvelope(
    val version: Int = CURRENT_VERSION,
    val envelopeId: String,
    /**
     * Stable routing address derived from the sender's signing identity.
     */
    val senderId: String,
    /**
     * Stable routing address derived from the recipient's signing
     * identity.
     */
    val recipientId: String,
    /**
     * Fully encoded SecureChat transport payload.
     *
     * Examples:
     *
     * scmsg:1:PLAINTEXT:...
     * scmsg:1:SEALED_BOX:...
     */
    val payload: String,
    val createdAtEpochMilliseconds: Long
) {
    init {
        require(version > 0) {
            "Transport-envelope version must be positive"
        }

        require(envelopeId.isNotBlank()) {
            "Envelope ID must not be blank"
        }

        require(senderId.isNotBlank()) {
            "Sender ID must not be blank"
        }

        require(recipientId.isNotBlank()) {
            "Recipient ID must not be blank"
        }

        require(payload.isNotBlank()) {
            "Transport payload must not be blank"
        }

        require(createdAtEpochMilliseconds >= 0L) {
            "Envelope timestamp must not be negative"
        }
    }

    companion object {
        const val CURRENT_VERSION = 1
    }
}
