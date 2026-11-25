package com.cbgm.sparrow.feature.transport.data.mailbox

import com.cbgm.sparrow.feature.transport.domain.model.FederatedEnvelope
import com.cbgm.sparrow.feature.transport.domain.repository.MailboxRepository
import com.cbgm.sparrow.protocol.mailbox.LocalMailboxCredential

internal class MailboxRepositoryImpl(
    private val remoteDataSource: MailboxRemoteDataSource
) : MailboxRepository {
    override suspend fun create(
        contactId: String,
        nodeId: String,
        routeEndpoint: String,
        accessEndpoint: String,
        sequence: Long,
        expiresAtEpochMilliseconds: Long
    ): Result<LocalMailboxCredential> = remoteDataSource.create(contactId, nodeId, routeEndpoint, accessEndpoint, sequence, expiresAtEpochMilliseconds)

    override suspend fun createInvitation(
        nodeId: String,
        routeEndpoint: String,
        accessEndpoint: String,
        expiresAtEpochMilliseconds: Long
    ): Result<LocalMailboxCredential> = remoteDataSource.createInvitation(nodeId, routeEndpoint, accessEndpoint, expiresAtEpochMilliseconds)

    override suspend fun pending(credential: LocalMailboxCredential): Result<List<FederatedEnvelope>> = remoteDataSource.pending(credential)

    override suspend fun acknowledge(
        credential: LocalMailboxCredential,
        envelopeId: String
    ): Result<Unit> = remoteDataSource.acknowledge(credential, envelopeId)

    override suspend fun revoke(credential: LocalMailboxCredential): Result<Unit> = remoteDataSource.revoke(credential)

    override suspend fun deliver(envelope: FederatedEnvelope): Result<Unit> = remoteDataSource.deliver(envelope)

    override suspend fun depositInvitation(envelope: FederatedEnvelope): Result<Unit> = remoteDataSource.depositInvitation(envelope)

    override suspend fun ready(
        accessEndpoint: String,
        credentials: List<LocalMailboxCredential>
    ): Result<Set<String>> = remoteDataSource.ready(accessEndpoint, credentials)
}
