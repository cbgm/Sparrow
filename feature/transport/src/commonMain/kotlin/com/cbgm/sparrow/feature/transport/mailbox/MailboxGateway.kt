package com.cbgm.sparrow.feature.transport.mailbox

import com.cbgm.sparrow.feature.transport.gateway.model.FederatedEnvelope
import com.cbgm.sparrow.protocol.mailbox.LocalMailboxCredential
import com.cbgm.sparrow.protocol.mailbox.MailboxDeliveryRoute
import kotlinx.serialization.Serializable

interface MailboxGateway {
    suspend fun create(
        contactId: String,
        nodeId: String,
        routeEndpoint: String,
        accessEndpoint: String,
        sequence: Long,
        expiresAtEpochMilliseconds: Long,
        wakeTicket: MailboxWakeTicket
    ): Result<LocalMailboxCredential>

    suspend fun pending(credential: LocalMailboxCredential): Result<List<FederatedEnvelope>>

    suspend fun acknowledge(
        credential: LocalMailboxCredential,
        envelopeId: String
    ): Result<Unit>

    suspend fun revoke(credential: LocalMailboxCredential): Result<Unit>

    /** Puts [envelope] into the contact's mailbox it names ([FederatedEnvelope.mailboxRoute]). */
    suspend fun deliver(envelope: FederatedEnvelope): Result<Unit>

    /**
     * Waits a few seconds at [accessEndpoint] for envelopes in [credentials] (all kept there); the mailbox IDs that
     * hold some, empty when none arrived meanwhile.
     */
    suspend fun ready(
        accessEndpoint: String,
        credentials: List<LocalMailboxCredential>
    ): Result<Set<String>>
}

@Serializable
internal data class CreateMailboxRequest(
    val nodeId: String,
    val nodeEndpoint: String,
    val routeSequence: Long,
    val expiresAtEpochMilliseconds: Long,
    val wakeTicket: MailboxWakeTicket,
    val creationWork: MailboxCreationWork
)

/** The proof of work that pays for a new mailbox ([MailboxCreationWorkSolver]). */
@Serializable
internal data class MailboxCreationWork(
    val stamp: String,
    val createdAtEpochMilliseconds: Long,
    val nonce: Long
)

@Serializable
internal data class MailboxEnvelopeRequest(
    val envelope: FederatedEnvelope
)

@Serializable
internal data class MailboxWaitRequest(
    val mailboxes: List<MailboxWaitEntry>
)

@Serializable
internal data class MailboxWaitEntry(
    val mailboxId: String,
    val retrievalCapability: String
)

@Serializable
internal data class MailboxWaitResponse(
    val ready: List<String>
)

@Serializable
internal data class CreateMailboxResponse(
    val deliveryRoute: MailboxDeliveryRoute,
    val retrievalCapability: String
)
