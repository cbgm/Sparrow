package com.cbgm.sparrow.feature.messaging.runtime.mailbox

import com.cbgm.sparrow.core.protocol.mailbox.LocalMailboxCredential
import com.cbgm.sparrow.core.protocol.mailbox.MailboxCapabilityLifecycle
import com.cbgm.sparrow.core.protocol.mailbox.MailboxRouteRepository
import com.cbgm.sparrow.core.protocol.outbox.ProtocolOutbox
import com.cbgm.sparrow.core.protocol.packet.MailboxRoutePacket
import com.cbgm.sparrow.core.security.ContactBlocklistRepository
import com.cbgm.sparrow.core.time.SystemClock
import com.cbgm.sparrow.feature.messaging.data.datasource.MailboxContactDataSource
import com.cbgm.sparrow.feature.transport.discovery.NodeEndpointResolver
import com.cbgm.sparrow.feature.transport.mailbox.MailboxGateway
import com.cbgm.sparrow.feature.transport.routing.LocalRoutingIdProvider

class MailboxRouteProvisioner(
    private val mailboxContactDataSource: MailboxContactDataSource,
    private val localRoutingIdProvider: LocalRoutingIdProvider,
    private val nodeEndpointResolver: NodeEndpointResolver,
    private val mailboxGateway: MailboxGateway,
    private val mailboxRouteRepository: MailboxRouteRepository,
    private val mailboxCapabilityLifecycle: MailboxCapabilityLifecycle,
    private val contactBlocklistRepository: ContactBlocklistRepository,
    private val credentialFactory: MailboxCredentialFactory,
    private val protocolOutbox: ProtocolOutbox
) {
    suspend fun provision(): Result<Int> =
        runCatching {
            val now = SystemClock.nowEpochMilliseconds()
            val node = resolveMailboxNode()
            mailboxCapabilityLifecycle.retryPendingRevocations().getOrNull()
            val blockedContactIds = contactBlocklistRepository.getBlockedContactIds()
            var provisioned = 0

            mailboxContactDataSource.findContactStates().forEach { contactState ->
                val contactId = contactState.contactId
                if (contactId in blockedContactIds) {
                    mailboxCapabilityLifecycle.revokeForContact(contactId).getOrNull()
                    return@forEach
                }
                if (!contactState.isProvisioningEligible) return@forEach

                val current = mailboxRouteRepository.localForContact(contactId).getOrThrow()
                if (current?.revocationPending == true) return@forEach
                val credential =
                    if (
                        current != null &&
                        current.deliveryRoute.expiresAtEpochMilliseconds > now + RENEWAL_WINDOW_MILLISECONDS
                    ) {
                        current
                    } else {
                        credentialFactory
                            .create(
                                contactId = contactId,
                                nodeId = node.nodeId,
                                routeEndpoint = checkNotNull(node.mailboxRouteEndpoint),
                                accessEndpoint = checkNotNull(node.mailboxAccessEndpoint),
                                sequence = maxOf((current?.deliveryRoute?.sequence ?: -1L) + 1L, now),
                                expiresAtEpochMilliseconds = now + ROUTE_LIFETIME_MILLISECONDS
                            ).also { replacement ->
                                replaceCredential(contactId, current, replacement)
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

    private suspend fun resolveMailboxNode() =
        nodeEndpointResolver
            .resolve(localRoutingIdProvider.getLocalRoutingId().getOrThrow())
            .getOrThrow()
            .firstOrNull { node ->
                node.mailboxRouteEndpoint != null && node.mailboxAccessEndpoint != null
            } ?: error("No mailbox-capable node is available")

    private suspend fun replaceCredential(
        contactId: String,
        current: LocalMailboxCredential?,
        replacement: LocalMailboxCredential
    ) {
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
    }

    private companion object {
        const val ROUTE_LIFETIME_MILLISECONDS = 30L * 24L * 60L * 60L * 1_000L
        const val RENEWAL_WINDOW_MILLISECONDS = 3L * 24L * 60L * 60L * 1_000L
    }
}
