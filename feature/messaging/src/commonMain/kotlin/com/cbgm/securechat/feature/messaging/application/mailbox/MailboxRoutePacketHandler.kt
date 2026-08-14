package com.cbgm.securechat.feature.messaging.application.mailbox

import com.cbgm.securechat.core.crypto.signature.DetachedSignatureCrypto
import com.cbgm.securechat.core.protocol.handler.IncomingPacketContext
import com.cbgm.securechat.core.protocol.handler.TypedProtocolPacketHandler
import com.cbgm.securechat.core.protocol.mailbox.MailboxRouteRepository
import com.cbgm.securechat.core.protocol.packet.MailboxRoutePacket
import com.cbgm.securechat.core.protocol.packet.SecureChatPacket
import com.cbgm.securechat.core.time.SystemClock
import com.cbgm.securechat.data.database.dao.ContactDao

class MailboxRoutePacketHandler(
    private val contactDao: ContactDao,
    private val repository: MailboxRouteRepository,
    private val signatureCrypto: DetachedSignatureCrypto,
    private val payloadEncoder: MailboxRoutePayloadEncoder
) : TypedProtocolPacketHandler {
    override fun canHandle(packet: SecureChatPacket): Boolean = packet is MailboxRoutePacket

    override suspend fun handle(
        context: IncomingPacketContext,
        packet: SecureChatPacket
    ): Result<Unit> =
        runCatching {
            val route = (packet as MailboxRoutePacket).deliveryRoute
            require(route.routeId.isNotBlank() && route.nodeId.isNotBlank()) {
                "Mailbox route identity is invalid"
            }
            require(route.mailboxId.isNotBlank() && route.sendCapability.isNotBlank()) {
                "Mailbox capability is invalid"
            }
            require(
                route.nodeEndpoint.startsWith("https://") ||
                    route.nodeEndpoint.startsWith("http://")
            ) { "Mailbox endpoint must use HTTP(S)" }
            require(route.sequence >= 0L) { "Mailbox route sequence is invalid" }
            require(route.expiresAtEpochMilliseconds > SystemClock.nowEpochMilliseconds()) {
                "Mailbox route has expired"
            }
            require(route.identitySignature.isNotEmpty()) { "Mailbox route is unsigned" }
            val identity =
                contactDao.findPublicIdentityByContactId(context.contactId)
                    ?: error("Contact identity is unavailable")
            check(identity.keyExchangeStatus == "MUTUAL") { "Contact identity exchange is incomplete" }
            signatureCrypto
                .verify(
                    payload = payloadEncoder.encode(route.copy(identitySignature = byteArrayOf())),
                    signingPublicKey = identity.signingPublicKey,
                    signature = route.identitySignature
                ).getOrThrow()
            repository.saveRemote(context.contactId, route).getOrThrow()
        }
}
