package com.cbgm.sparrow.feature.conversationorchestration.runtime.mailbox

import com.cbgm.sparrow.core.logging.SparrowLog
import com.cbgm.sparrow.core.protocol.mailbox.LocalMailboxCredential
import com.cbgm.sparrow.core.protocol.mailbox.MailboxCapabilityLifecycle
import com.cbgm.sparrow.core.protocol.mailbox.MailboxRouteRepository
import com.cbgm.sparrow.core.protocol.outbox.ProtocolOutbox
import com.cbgm.sparrow.core.protocol.packet.MailboxRoutePacket
import com.cbgm.sparrow.core.time.SystemClock
import com.cbgm.sparrow.feature.contacts.domain.repository.ContactBlocklistRepository
import com.cbgm.sparrow.feature.contacts.domain.usecase.GetMailboxContactStatesUseCase
import com.cbgm.sparrow.feature.transport.connection.isRecoverableConnectivityFailure
import com.cbgm.sparrow.feature.transport.discovery.NodeEndpoint
import com.cbgm.sparrow.feature.transport.discovery.NodeEndpointResolver
import com.cbgm.sparrow.feature.transport.mailbox.MailboxGateway
import com.cbgm.sparrow.feature.transport.routing.LocalRoutingIdProvider
import kotlinx.coroutines.CancellationException

class MailboxRouteProvisioner(
    private val getMailboxContactStates: GetMailboxContactStatesUseCase,
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
            // Node discovery validates signatures before returning a directory. It is
            // required before deciding that a saved mailbox belongs to an old server. Never infer this
            // from a socket failure: an offline node may still own pending envelopes.
            val availableNodes = resolveMailboxNodes()
            val node = availableNodes.first()
            mailboxCapabilityLifecycle.retryPendingRevocations()
                .onFailure { failure ->
                    if (failure is CancellationException) throw failure
                    if (failure.isRecoverableConnectivityFailure()) {
                        SparrowLog.withTag("MailboxRouteProvisioner").debug {
                            "Mailbox revocation deferred while its server is unreachable"
                        }
                    } else {
                        SparrowLog.error("MailboxRouteProvisioner", "Retrying mailbox revocations failed", failure)
                    }
                }
            val blockedContactIds = contactBlocklistRepository.getBlockedContactIds()
            var provisioned = 0

            getMailboxContactStates().forEach { contactState ->
                val contactId = contactState.contactId
                if (contactId in blockedContactIds) {
                    mailboxCapabilityLifecycle.revokeForContact(contactId)
                        .onFailure { failure ->
                            if (failure is CancellationException) throw failure
                            if (failure.isRecoverableConnectivityFailure()) {
                                SparrowLog.withTag("MailboxRouteProvisioner").debug {
                                    "Mailbox revocation deferred for blocked contact while its node is offline"
                                }
                            } else {
                                SparrowLog.error("MailboxRouteProvisioner", "Mailbox revocation failed for $contactId", failure)
                            }
                        }
                    return@forEach
                }
                if (!contactState.isProvisioningEligible) return@forEach

                val current = mailboxRouteRepository.localForContact(contactId).getOrThrow()
                // Only the signed node directory can retire an old mailbox. A previous installation's capability cannot retrieve from
                // a newly installed server, even if its public hostname is the same.
                val retired = current != null && current.isRetiredFrom(availableNodes)
                if (current?.revocationPending == true && !retired) return@forEach
                val credential =
                    if (
                        current != null && !retired &&
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
                                replaceCredential(contactId, current, replacement, retired)
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

    private suspend fun resolveMailboxNodes(): List<NodeEndpoint> =
        nodeEndpointResolver
            .resolve(localRoutingIdProvider.getLocalRoutingId().getOrThrow(), forceRefresh = true)
            .getOrThrow()
            .filter { node ->
                node.mailboxRouteEndpoint != null && node.mailboxAccessEndpoint != null
            }.also { nodes -> check(nodes.isNotEmpty()) { "No mailbox-capable node is available" } }

    private suspend fun replaceCredential(
        contactId: String,
        current: LocalMailboxCredential?,
        replacement: LocalMailboxCredential,
        retired: Boolean
    ) {
        if (current != null && retired) {
            // The old server is absent from the freshly verified directory. Its
            // mailbox and capability were removed by the server reinstall, so a
            // revocation against its old LAN address can never succeed. Save the
            // newly created mailbox first: a DB failure leaves the old local row
            // intact instead of destroying it before replacement is ready.
            mailboxRouteRepository.saveLocal(replacement).getOrThrow()
            SparrowLog.withTag("MailboxRouteProvisioner").warn {
                "Replaced mailbox on retired node ${current.deliveryRoute.nodeId} " +
                    "with mailbox on ${replacement.deliveryRoute.nodeId}; old server-side envelopes cannot be recovered"
            }
            return
        }
        if (current != null) {
            mailboxRouteRepository.markLocalRevocationPending(contactId).getOrThrow()
            mailboxGateway.revoke(current).getOrElse { revocationError ->
                mailboxGateway.revoke(replacement)
                    .onFailure { failure -> SparrowLog.error("MailboxRouteProvisioner", "Mailbox replacement cleanup failed for $contactId", failure) }
                throw revocationError
            }
            mailboxRouteRepository.deleteLocal(contactId).getOrThrow()
        }

        mailboxRouteRepository.saveLocal(replacement).getOrElse { persistenceError ->
            mailboxGateway.revoke(replacement)
                .onFailure { failure -> SparrowLog.error("MailboxRouteProvisioner", "Mailbox replacement cleanup failed for $contactId", failure) }
            throw persistenceError
        }
    }

    private companion object {
        const val ROUTE_LIFETIME_MILLISECONDS = 30L * 24L * 60L * 60L * 1_000L
        const val RENEWAL_WINDOW_MILLISECONDS = 3L * 24L * 60L * 60L * 1_000L
    }
}

/** Do not discard a mailbox merely because its original node is temporarily unreachable. */
internal fun LocalMailboxCredential.isRetiredFrom(availableNodes: List<NodeEndpoint>): Boolean =
    availableNodes.isNotEmpty() && availableNodes.none { node -> node.nodeId == deliveryRoute.nodeId }
