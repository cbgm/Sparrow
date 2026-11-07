package com.cbgm.sparrow.feature.transport.gateway.model

import com.cbgm.sparrow.protocol.mailbox.MailboxDeliveryRoute
import kotlinx.serialization.Serializable

@Serializable
data class FederatedEnvelope(
    val envelopeId: String,
    val senderRoutingId: String,
    val recipientDeviceRoutingId: String,
    val mailboxRoute: MailboxDeliveryRoute? = null,
    val encryptedPayload: String,
    val createdAtEpochMilliseconds: Long,
    val expiresAtEpochMilliseconds: Long
) {
    /**
     * The envelope as it goes into a contact's mailbox: without the sender's and the recipient's routing IDs, so the
     * nodes that pass it on and the mailbox that keeps it do not learn who writes to whom. The recipient knows the
     * sender anyway: only that contact holds the mailbox's send capability.
     */
    fun withoutRoutingIds(): FederatedEnvelope = copy(senderRoutingId = HIDDEN_ROUTING_ID, recipientDeviceRoutingId = HIDDEN_ROUTING_ID)

    companion object {
        const val HIDDEN_ROUTING_ID = "mailbox"
    }
}
