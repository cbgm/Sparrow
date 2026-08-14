package com.cbgm.sparrow.feature.transport.gateway.model

import com.cbgm.sparrow.core.protocol.mailbox.MailboxDeliveryRoute
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
)
