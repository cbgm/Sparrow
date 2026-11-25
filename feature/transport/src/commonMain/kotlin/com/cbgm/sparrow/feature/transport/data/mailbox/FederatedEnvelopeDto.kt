package com.cbgm.sparrow.feature.transport.data.mailbox

import com.cbgm.sparrow.feature.transport.domain.model.FederatedEnvelope
import com.cbgm.sparrow.protocol.mailbox.MailboxDeliveryRoute
import kotlinx.serialization.Serializable

/** An envelope as the mailbox nodes send and receive it. */
@Serializable
internal data class FederatedEnvelopeDto(
    val envelopeId: String,
    val senderRoutingId: String,
    val recipientDeviceRoutingId: String,
    val mailboxRoute: MailboxDeliveryRoute? = null,
    val encryptedPayload: String,
    val createdAtEpochMilliseconds: Long,
    val expiresAtEpochMilliseconds: Long
)

internal fun FederatedEnvelopeDto.toDomain(): FederatedEnvelope =
    FederatedEnvelope(
        envelopeId = envelopeId,
        senderRoutingId = senderRoutingId,
        recipientDeviceRoutingId = recipientDeviceRoutingId,
        mailboxRoute = mailboxRoute,
        encryptedPayload = encryptedPayload,
        createdAtEpochMilliseconds = createdAtEpochMilliseconds,
        expiresAtEpochMilliseconds = expiresAtEpochMilliseconds
    )

internal fun FederatedEnvelope.toDto(): FederatedEnvelopeDto =
    FederatedEnvelopeDto(
        envelopeId = envelopeId,
        senderRoutingId = senderRoutingId,
        recipientDeviceRoutingId = recipientDeviceRoutingId,
        mailboxRoute = mailboxRoute,
        encryptedPayload = encryptedPayload,
        createdAtEpochMilliseconds = createdAtEpochMilliseconds,
        expiresAtEpochMilliseconds = expiresAtEpochMilliseconds
    )
