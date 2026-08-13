package com.cbgm.securechat.feature.messaging.application.mailbox

import com.cbgm.securechat.core.crypto.signature.DetachedSignatureCrypto
import com.cbgm.securechat.core.protocol.identity.LocalSigningKeyPairProvider
import com.cbgm.securechat.core.protocol.mailbox.LocalMailboxCredential
import com.cbgm.securechat.core.protocol.mailbox.MailboxCapabilityLifecycle
import com.cbgm.securechat.core.protocol.mailbox.MailboxRouteRepository
import com.cbgm.securechat.core.protocol.outbox.ProtocolOutbox
import com.cbgm.securechat.core.protocol.packet.MailboxRoutePacket
import com.cbgm.securechat.core.security.ContactBlocklistRepository
import com.cbgm.securechat.core.time.SystemClock
import com.cbgm.securechat.data.database.dao.ContactDao
import com.cbgm.securechat.data.database.dao.ContactRoutingIdDao
import com.cbgm.securechat.feature.messaging.application.incoming.IncomingEnvelopeProcessingResult
import com.cbgm.securechat.feature.messaging.application.incoming.IncomingEnvelopeProcessor
import com.cbgm.securechat.feature.transport.discovery.NodeEndpointResolver
import com.cbgm.securechat.feature.transport.mailbox.MailboxGateway
import com.cbgm.securechat.feature.transport.routing.LocalRoutingIdProvider
import kotlinx.coroutines.flow.first

@Suppress("LongParameterList")
class DefaultMailboxCoordinator(
    private val contactDao: ContactDao,
    private val contactRoutingIdDao: ContactRoutingIdDao,
    private val localRoutingIdProvider: LocalRoutingIdProvider,
    private val nodeEndpointResolver: NodeEndpointResolver,
    private val mailboxGateway: MailboxGateway,
    private val mailboxRouteRepository: MailboxRouteRepository,
    private val mailboxCapabilityLifecycle: MailboxCapabilityLifecycle,
    private val contactBlocklistRepository: ContactBlocklistRepository,
    private val signingKeyPairProvider: LocalSigningKeyPairProvider,
    private val signatureCrypto: DetachedSignatureCrypto,
    private val payloadEncoder: MailboxRoutePayloadEncoder,
    private val protocolOutbox: ProtocolOutbox,
    private val incomingEnvelopeProcessor: IncomingEnvelopeProcessor
) : MailboxCoordinator {
    override suspend fun provisionRoutes(): Result<Int> =
        runCatching {
            val now = SystemClock.nowEpochMilliseconds()
            val localRoutingId = localRoutingIdProvider.getLocalRoutingId().getOrThrow()
            val node =
                nodeEndpointResolver
                    .resolve(localRoutingId)
                    .getOrThrow()
                    .firstOrNull { it.mailboxRouteEndpoint != null && it.mailboxAccessEndpoint != null }
                    ?: error("No mailbox-capable node is available")
            var provisioned = 0
            mailboxCapabilityLifecycle.retryPendingRevocations().getOrNull()
            val blockedContactIds = contactBlocklistRepository.getBlockedContactIds()

            contactDao.observeAll().first().forEach { contact ->
                val contactId = contact.contact.id
                if (contactId in blockedContactIds) {
                    mailboxCapabilityLifecycle.revokeForContact(contactId).getOrNull()
                    return@forEach
                }
                val identity = contact.publicIdentity ?: return@forEach
                if (identity.keyExchangeStatus != "MUTUAL") return@forEach
                if (contactRoutingIdDao.findRoutingIdByContactId(contactId).isNullOrBlank()) return@forEach

                val current = mailboxRouteRepository.localForContact(contactId).getOrThrow()
                if (current?.revocationPending == true) return@forEach
                val credential =
                    if (
                        current != null &&
                        current.deliveryRoute.expiresAtEpochMilliseconds > now + RENEWAL_WINDOW_MILLISECONDS
                    ) {
                        current
                    } else {
                        createSignedCredential(
                            contactId = contactId,
                            nodeId = node.nodeId,
                            routeEndpoint = checkNotNull(node.mailboxRouteEndpoint),
                            accessEndpoint = checkNotNull(node.mailboxAccessEndpoint),
                            sequence =
                                maxOf(
                                    (current?.deliveryRoute?.sequence ?: -1L) + 1L,
                                    now
                                ),
                            expiresAt = now + ROUTE_LIFETIME_MILLISECONDS
                        ).also { replacement ->
                            if (current != null) {
                                mailboxRouteRepository.markLocalRevocationPending(contactId).getOrThrow()
                                mailboxGateway.revoke(current).getOrElse { revocationError ->
                                    mailboxGateway.revoke(replacement).getOrNull()
                                    throw revocationError
                                }
                                mailboxRouteRepository.deleteLocal(contactId).getOrThrow()
                            }
                            mailboxRouteRepository.saveLocal(replacement).getOrElse { persistenceError ->
                                mailboxGateway.revoke(replacement).getOrNull()
                                throw persistenceError
                            }
                            provisioned += 1
                        }
                    }

                protocolOutbox
                    .enqueue(
                        contactId = contactId,
                        packet =
                            MailboxRoutePacket(
                                packetId = "mailbox-route-${credential.deliveryRoute.routeId}",
                                deliveryRoute = credential.deliveryRoute
                            )
                    ).getOrThrow()
            }
            provisioned
        }

    override suspend fun synchronizePending(): Result<Int> =
        runCatching {
            var processed = 0
            mailboxRouteRepository
                .allLocal()
                .getOrThrow()
                .filterNot(LocalMailboxCredential::revocationPending)
                .forEach { credential ->
                    mailboxGateway.pending(credential).getOrThrow().forEach { envelope ->
                        when (
                            incomingEnvelopeProcessor
                                .process(
                                    envelopeId = envelope.envelopeId,
                                    senderRoutingId = envelope.senderRoutingId,
                                    encodedTransportPayload = envelope.encryptedPayload
                                ).getOrThrow()
                        ) {
                            IncomingEnvelopeProcessingResult.Processed -> {
                                mailboxGateway.acknowledge(credential, envelope.envelopeId).getOrThrow()
                                processed += 1
                            }
                            IncomingEnvelopeProcessingResult.UnknownSender -> Unit
                        }
                    }
                }
            processed
        }

    private suspend fun createSignedCredential(
        contactId: String,
        nodeId: String,
        routeEndpoint: String,
        accessEndpoint: String,
        sequence: Long,
        expiresAt: Long
    ): LocalMailboxCredential {
        val created =
            mailboxGateway
                .create(
                    contactId,
                    nodeId,
                    routeEndpoint,
                    accessEndpoint,
                    sequence,
                    expiresAt
                ).getOrThrow()
        val keyPair = signingKeyPairProvider.getSigningKeyPair().getOrThrow()
        val route = created.deliveryRoute.copy(identitySignature = byteArrayOf())
        val signature = signatureCrypto.sign(payloadEncoder.encode(route), keyPair.privateKey).getOrThrow()
        return created.copy(deliveryRoute = route.copy(identitySignature = signature))
    }

    private companion object {
        const val ROUTE_LIFETIME_MILLISECONDS = 30L * 24L * 60L * 60L * 1_000L
        const val RENEWAL_WINDOW_MILLISECONDS = 3L * 24L * 60L * 60L * 1_000L
    }
}
