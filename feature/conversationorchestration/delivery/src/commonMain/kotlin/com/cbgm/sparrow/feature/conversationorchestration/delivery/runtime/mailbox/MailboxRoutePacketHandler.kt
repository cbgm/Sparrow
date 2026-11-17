package com.cbgm.sparrow.feature.conversationorchestration.delivery.runtime.mailbox

import com.cbgm.sparrow.core.crypto.signature.DetachedSignatureCrypto
import com.cbgm.sparrow.core.util.logging.SparrowLog
import com.cbgm.sparrow.core.util.time.SystemClock
import com.cbgm.sparrow.feature.contacts.domain.usecase.GetMailboxRouteSigningPublicKeyUseCase
import com.cbgm.sparrow.feature.messaging.runtime.mailbox.MailboxRoutePayloadEncoder
import com.cbgm.sparrow.protocol.handler.IncomingPacketContext
import com.cbgm.sparrow.protocol.handler.TypedProtocolPacketHandler
import com.cbgm.sparrow.protocol.mailbox.MailboxDeliveryRoute
import com.cbgm.sparrow.protocol.mailbox.MailboxRouteRepository
import com.cbgm.sparrow.protocol.packet.MailboxRoutePacket
import com.cbgm.sparrow.protocol.packet.SparrowPacket

class MailboxRoutePacketHandler(
    private val getRouteSigningPublicKey: GetMailboxRouteSigningPublicKeyUseCase,
    private val repository: MailboxRouteRepository,
    private val signatureCrypto: DetachedSignatureCrypto,
    private val payloadEncoder: MailboxRoutePayloadEncoder
) : TypedProtocolPacketHandler {
    private val logger = SparrowLog.withTag("MailboxRoutePacketHandler")

    override fun canHandle(packet: SparrowPacket): Boolean = packet is MailboxRoutePacket

    override suspend fun handle(
        context: IncomingPacketContext,
        packet: SparrowPacket
    ): Result<Unit> =
        runCatching {
            val route = (packet as MailboxRoutePacket).deliveryRoute
            validateRoute(route)
            val signingPublicKey =
                getRouteSigningPublicKey(context.contactId).getOrThrow()
                    ?: error("Contact identity is unavailable or the handshake is incomplete")
            signatureCrypto
                .verify(
                    payload = payloadEncoder.encode(route.copy(identitySignature = byteArrayOf())),
                    signingPublicKey = signingPublicKey,
                    signature = route.identitySignature
                ).getOrThrow()
            repository.saveRemote(context.contactId, route).getOrThrow()
            logger.info { "Mailbox route of contact ${context.contactId} saved; messages to them now go into their mailbox" }
        }

    private fun validateRoute(
        route: MailboxDeliveryRoute
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
