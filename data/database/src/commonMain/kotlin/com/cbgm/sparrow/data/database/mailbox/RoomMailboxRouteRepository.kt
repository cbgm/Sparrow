package com.cbgm.sparrow.data.database.mailbox

import com.cbgm.sparrow.data.database.dao.MailboxRouteDao
import com.cbgm.sparrow.data.database.entity.LocalMailboxCredentialEntity
import com.cbgm.sparrow.data.database.entity.RemoteMailboxRouteEntity
import com.cbgm.sparrow.data.database.entity.RetiringMailboxCredentialEntity
import com.cbgm.sparrow.protocol.mailbox.LocalMailboxCredential
import com.cbgm.sparrow.protocol.mailbox.MailboxDeliveryRoute
import com.cbgm.sparrow.protocol.mailbox.MailboxRouteRepository

class RoomMailboxRouteRepository(
    private val dao: MailboxRouteDao
) : MailboxRouteRepository {
    override suspend fun localForContact(contactId: String): Result<LocalMailboxCredential?> = runCatching { dao.findLocal(contactId)?.toLocalMailboxCredential() }

    override suspend fun remoteForRecipientRoutingId(routingId: String): Result<MailboxDeliveryRoute?> = runCatching { dao.findRemoteByRoutingId(routingId)?.toMailboxDeliveryRoute() }

    override suspend fun allLocal(): Result<List<LocalMailboxCredential>> = runCatching { dao.allLocal().map(LocalMailboxCredentialEntity::toLocalMailboxCredential) }

    override suspend fun saveLocal(credential: LocalMailboxCredential): Result<Unit> = runCatching { dao.upsertLocal(credential.toLocalMailboxCredentialEntity()) }

    override suspend fun saveRemote(
        contactId: String,
        route: MailboxDeliveryRoute
    ): Result<Unit> =
        runCatching {
            val current = dao.findRemote(contactId)
            if (current == null || route.sequence > current.sequence ||
                (route.sequence == current.sequence && route.routeId == current.routeId)
            ) {
                dao.upsertRemote(route.toRemoteMailboxRouteEntity(contactId))
            }
        }

    override suspend fun markLocalRevocationPending(contactId: String): Result<Unit> = runCatching { dao.markLocalRevocationPending(contactId) }

    override suspend fun deleteLocal(contactId: String): Result<Unit> = runCatching { dao.deleteLocal(contactId) }

    override suspend fun deleteRemote(contactId: String): Result<Unit> = runCatching { dao.deleteRemote(contactId) }

    override suspend fun deleteAllRemote(): Result<Unit> = runCatching { dao.deleteAllRemote() }

    override suspend fun allRetiring(): Result<List<LocalMailboxCredential>> =
        runCatching { dao.allRetiring().map(RetiringMailboxCredentialEntity::toLocalMailboxCredential) }

    override suspend fun retireLocal(
        retiring: LocalMailboxCredential,
        replacement: LocalMailboxCredential
    ): Result<Unit> =
        runCatching {
            dao.retireLocal(retiring.toRetiringMailboxCredentialEntity(), replacement.toLocalMailboxCredentialEntity())
        }

    override suspend fun saveRetiring(credential: LocalMailboxCredential): Result<Unit> =
        runCatching { dao.upsertRetiring(credential.toRetiringMailboxCredentialEntity()) }

    override suspend fun markRetiringRevocationPending(contactId: String): Result<Unit> =
        runCatching { dao.markRetiringRevocationPending(contactId) }

    override suspend fun deleteRetiring(routeId: String): Result<Unit> = runCatching { dao.deleteRetiring(routeId) }
}

private fun RetiringMailboxCredentialEntity.toLocalMailboxCredential() =
    LocalMailboxCredential(
        contactId,
        MailboxDeliveryRoute(
            routeId,
            nodeId,
            nodeEndpoint,
            mailboxId,
            sendCapability,
            sequence,
            expiresAtEpochMilliseconds,
            identitySignature
        ),
        accessEndpoint,
        retrievalCapability,
        revocationPending
    )

private fun LocalMailboxCredential.toRetiringMailboxCredentialEntity() =
    RetiringMailboxCredentialEntity(
        deliveryRoute.routeId,
        contactId,
        deliveryRoute.nodeId,
        deliveryRoute.nodeEndpoint,
        deliveryRoute.mailboxId,
        deliveryRoute.sendCapability,
        accessEndpoint,
        retrievalCapability,
        deliveryRoute.sequence,
        deliveryRoute.expiresAtEpochMilliseconds,
        deliveryRoute.identitySignature,
        revocationPending
    )

private fun LocalMailboxCredentialEntity.toLocalMailboxCredential() =
    LocalMailboxCredential(
        contactId,
        toMailboxDeliveryRoute(),
        accessEndpoint,
        retrievalCapability,
        revocationPending
    )

private fun LocalMailboxCredentialEntity.toMailboxDeliveryRoute() =
    MailboxDeliveryRoute(
        routeId,
        nodeId,
        nodeEndpoint,
        mailboxId,
        sendCapability,
        sequence,
        expiresAtEpochMilliseconds,
        identitySignature
    )

private fun RemoteMailboxRouteEntity.toMailboxDeliveryRoute() =
    MailboxDeliveryRoute(
        routeId,
        nodeId,
        nodeEndpoint,
        mailboxId,
        sendCapability,
        sequence,
        expiresAtEpochMilliseconds,
        identitySignature
    )

private fun LocalMailboxCredential.toLocalMailboxCredentialEntity() =
    LocalMailboxCredentialEntity(
        contactId,
        deliveryRoute.routeId,
        deliveryRoute.nodeId,
        deliveryRoute.nodeEndpoint,
        deliveryRoute.mailboxId,
        deliveryRoute.sendCapability,
        accessEndpoint,
        retrievalCapability,
        deliveryRoute.sequence,
        deliveryRoute.expiresAtEpochMilliseconds,
        deliveryRoute.identitySignature,
        revocationPending
    )

private fun MailboxDeliveryRoute.toRemoteMailboxRouteEntity(contactId: String) =
    RemoteMailboxRouteEntity(
        contactId,
        routeId,
        nodeId,
        nodeEndpoint,
        mailboxId,
        sendCapability,
        sequence,
        expiresAtEpochMilliseconds,
        identitySignature
    )
