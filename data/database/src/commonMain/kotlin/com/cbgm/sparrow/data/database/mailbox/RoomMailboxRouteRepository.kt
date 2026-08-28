package com.cbgm.sparrow.data.database.mailbox

import com.cbgm.sparrow.core.protocol.mailbox.LocalMailboxCredential
import com.cbgm.sparrow.core.protocol.mailbox.MailboxDeliveryRoute
import com.cbgm.sparrow.core.protocol.mailbox.MailboxRouteRepository
import com.cbgm.sparrow.data.database.dao.MailboxRouteDao
import com.cbgm.sparrow.data.database.entity.LocalMailboxCredentialEntity
import com.cbgm.sparrow.data.database.entity.RemoteMailboxRouteEntity

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
}

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
