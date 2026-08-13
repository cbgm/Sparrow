package com.cbgm.securechat.feature.transport.mailbox

import com.cbgm.securechat.core.protocol.mailbox.LocalMailboxCredential
import com.cbgm.securechat.core.protocol.mailbox.MailboxDeliveryRoute
import com.cbgm.securechat.feature.transport.gateway.model.FederatedEnvelope
import kotlinx.serialization.Serializable

interface MailboxGateway {
    suspend fun create(
        contactId: String,
        nodeId: String,
        routeEndpoint: String,
        accessEndpoint: String,
        sequence: Long,
        expiresAtEpochMilliseconds: Long
    ): Result<LocalMailboxCredential>

    suspend fun pending(credential: LocalMailboxCredential): Result<List<FederatedEnvelope>>

    suspend fun acknowledge(
        credential: LocalMailboxCredential,
        envelopeId: String
    ): Result<Unit>

    suspend fun revoke(credential: LocalMailboxCredential): Result<Unit>
}

@Serializable
internal data class CreateMailboxRequest(
    val nodeId: String,
    val nodeEndpoint: String,
    val routeSequence: Long,
    val expiresAtEpochMilliseconds: Long
)

@Serializable
internal data class CreateMailboxResponse(
    val deliveryRoute: MailboxDeliveryRoute,
    val retrievalCapability: String
)

@Serializable
internal data class MailboxEnvelopesResponse(
    val envelopes: List<FederatedEnvelope>
)
