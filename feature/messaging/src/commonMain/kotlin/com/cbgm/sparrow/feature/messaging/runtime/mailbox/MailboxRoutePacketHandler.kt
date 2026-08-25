package com.cbgm.sparrow.feature.messaging.runtime.mailbox

import com.cbgm.sparrow.core.crypto.signature.DetachedSignatureCrypto
import com.cbgm.sparrow.core.protocol.handler.IncomingPacketContext
import com.cbgm.sparrow.core.protocol.handler.TypedProtocolPacketHandler
import com.cbgm.sparrow.core.protocol.mailbox.MailboxRouteRepository
import com.cbgm.sparrow.core.protocol.packet.MailboxRoutePacket
import com.cbgm.sparrow.core.protocol.packet.SparrowPacket
import com.cbgm.sparrow.core.time.SystemClock
import com.cbgm.sparrow.feature.messaging.data.datasource.MailboxContactDataSource

class MailboxRoutePacketHandler(
    private val mailboxContactDataSource: MailboxContactDataSource,
    private val repository: MailboxRouteRepository,
    private val signatureCrypto: DetachedSignatureCrypto,
    private val payloadEncoder: MailboxRoutePayloadEncoder
) : TypedProtocolPacketHandler {
    override fun canHandle(packet: SparrowPacket): Boolean = packet is MailboxRoutePacket

    override suspend fun handle(
        context: IncomingPacketContext,
        packet: SparrowPacket
    ): Result<Unit> =
        runCatching {
            val route = (packet as MailboxRoutePacket).deliveryRoute
            validateRoute(route)
            val signingPublicKey =
                mailboxContactDataSource.findMutualSigningPublicKey(context.contactId)
                    ?: error("Contact identity is unavailable or identity exchange is incomplete")
            signatureCrypto
                .verify(
                    payload = payloadEncoder.encode(route.copy(identitySignature = byteArrayOf())),
                    signingPublicKey = signingPublicKey,
                    signature = route.identitySignature
                ).getOrThrow()
            repository.saveRemote(context.contactId, route).getOrThrow()
        }

    private fun validateRoute(
        route: com.cbgm.sparrow.core.protocol.mailbox.MailboxDeliveryRoute
    ) {
        require(route.routeId.isNotBlank() && route.nodeId.isNotBlank()) {
            "Mailbox route identity is invalid"
        }
        require(route.mailboxId.isNotBlank() && route.sendCapability.isNotBlank()) {
            "Mailbox capability is invalid"
        }
        require(route.nodeEndpoint.startsWith("https://") || route.nodeEndpoint.startsWith("http://")) {
            "Mailbox endpoint must use HTTP(S)"
        }
        require(route.sequence >= 0L) { "Mailbox route sequence is invalid" }
        require(route.expiresAtEpochMilliseconds > SystemClock.nowEpochMilliseconds()) {
            "Mailbox route has expired"
        }
        require(route.identitySignature.isNotEmpty()) { "Mailbox route is unsigned" }
    }
}
