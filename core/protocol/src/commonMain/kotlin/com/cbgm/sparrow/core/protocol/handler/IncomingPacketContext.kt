package com.cbgm.sparrow.core.protocol.handler

/**
 * Metadata belonging to a received protocol packet.
 *
 * The protocol module deliberately stores transport mode as a String
 * so core:protocol does not depend on core:crypto.
 */
data class IncomingPacketContext(
    val contactId: String,
    val conversationId: String,
    val encodedTransportPayload: String,
    val transportMode: String,
    val receivedAtEpochMilliseconds: Long
) {
    init {
        require(contactId.isNotBlank()) {
            "Contact ID must not be blank"
        }

        require(conversationId.isNotBlank()) {
            "Conversation ID must not be blank"
        }

        require(encodedTransportPayload.isNotBlank()) {
            "Encoded transport payload must not be blank"
        }

        require(transportMode.isNotBlank()) {
            "Transport mode must not be blank"
        }

        require(receivedAtEpochMilliseconds >= 0L) {
            "Received timestamp must not be negative"
        }
    }
}
