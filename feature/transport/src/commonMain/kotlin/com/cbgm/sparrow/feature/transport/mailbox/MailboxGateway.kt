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
        expiresAtEpochMilliseconds: Long
    ): Result<LocalMailboxCredential>

    /**
     * Opens this device's invitation mailbox ([InvitationMailboxRenewal]): anyone who looks up the device at a
     * presence directory may put envelopes there, each paid with a proof of work.
     */
    suspend fun createInvitation(
        nodeId: String,
        routeEndpoint: String,
        accessEndpoint: String,
        expiresAtEpochMilliseconds: Long
    ): Result<LocalMailboxCredential>

    suspend fun pending(credential: LocalMailboxCredential): Result<List<FederatedEnvelope>>

    suspend fun acknowledge(
        credential: LocalMailboxCredential,
        envelopeId: String
    ): Result<Unit>

    suspend fun revoke(credential: LocalMailboxCredential): Result<Unit>

    /** Puts [envelope] into the contact's mailbox it names ([FederatedEnvelope.mailboxRoute]). */
    suspend fun deliver(envelope: FederatedEnvelope): Result<Unit>

    /** Puts [envelope] into the invitation mailbox it names, paying the deposit with a proof of work. */
    suspend fun depositInvitation(envelope: FederatedEnvelope): Result<Unit>

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
    val creationWork: MailboxCreationWork,
    val invitation: Boolean = false
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
    val envelope: FederatedEnvelope,
    /** Only an invitation mailbox asks for it. */
    val depositWork: MailboxCreationWork? = null
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
