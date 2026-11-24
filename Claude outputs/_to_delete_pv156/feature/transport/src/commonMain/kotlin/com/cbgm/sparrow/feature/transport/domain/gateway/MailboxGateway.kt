package com.cbgm.sparrow.feature.transport.domain.gateway

import com.cbgm.sparrow.feature.transport.domain.model.FederatedEnvelope
import com.cbgm.sparrow.protocol.mailbox.LocalMailboxCredential

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
     * Opens this device's invitation mailbox ([InvitationMailbox]): anyone who looks up the device at a
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
