package com.cbgm.sparrow.feature.conversationorchestration.delivery.runtime.mailbox

import com.cbgm.sparrow.core.util.logging.SparrowLog
import com.cbgm.sparrow.core.util.time.SystemClock
import com.cbgm.sparrow.feature.contacts.domain.repository.ContactBlocklistRepository
import com.cbgm.sparrow.feature.contacts.domain.usecase.GetMailboxContactStatesUseCase
import com.cbgm.sparrow.feature.transport.connection.isRecoverableConnectivityFailure
import com.cbgm.sparrow.feature.transport.discovery.NodeEndpoint
import com.cbgm.sparrow.feature.transport.discovery.NodeEndpointResolver
import com.cbgm.sparrow.feature.transport.mailbox.MailboxGateway
import com.cbgm.sparrow.feature.transport.routing.LocalRoutingIdProvider
import com.cbgm.sparrow.protocol.mailbox.LocalMailboxCredential
import com.cbgm.sparrow.protocol.mailbox.MailboxCapabilityLifecycle
import com.cbgm.sparrow.protocol.mailbox.MailboxRouteRepository
import com.cbgm.sparrow.protocol.outbox.ProtocolOutbox
import com.cbgm.sparrow.protocol.packet.MailboxRoutePacket
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
            followMovedNodes(availableNodes)
            mailboxCapabilityLifecycle.retryPendingRevocations()
                .onFailure { failure ->
                    if (failure is CancellationException) throw failure
                    if (failure.isRecoverableConnectivityFailure()) {
                        SparrowLog.withTag("MailboxRouteProvisioner").debug {
                            "Mailbox revocation deferred while its server is unreachable"
                        }
                    } else {
                        SparrowLog.diagnostic("MailboxRouteProvisioner", "Retrying mailbox revocations failed", failure)
                    }
                }
            revokeExpiredRetiringMailboxes(now, availableNodes)
            val blockedContactIds = contactBlocklistRepository.getBlockedContactIds().getOrThrow()
            var provisioned = 0

            getMailboxContactStates().getOrThrow().forEach { contactState ->
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
                                SparrowLog.diagnostic(
                                    "MailboxRouteProvisioner",
                                    "Mailbox revocation failed for $contactId",
                                    failure
                                )
                            }
                        }
                    return@forEach
                }
                if (!contactState.isProvisioningEligible) return@forEach

                val current = mailboxRouteRepository.localForContact(contactId).getOrThrow()
                // Only the signed node directory can retire an old mailbox. A previous installation's capability cannot retrieve from
                // a newly installed server, even if its public hostname is the same.
                val retired = current != null && current.isRetiredFrom(availableNodes)
                // Contacts still deliver to the old address of a moved node until a new route is shared.
                val moved = current != null && current.hasMovedIn(availableNodes)
                if (current?.revocationPending == true && !retired) return@forEach
                val credential =
                    if (
                        current != null && !retired && !moved &&
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

    // A node keeps its ID when its address changes (e.g. a new public IP of the server). The
    // verified directory has the current address; the saved mailboxes are reached there.
    private suspend fun followMovedNodes(availableNodes: List<NodeEndpoint>) {
        mailboxRouteRepository.allLocal().getOrThrow().forEach { credential ->
            movedAccessEndpoint(credential, availableNodes)?.let { accessEndpoint ->
                mailboxRouteRepository.saveLocal(credential.copy(accessEndpoint = accessEndpoint)).getOrThrow()
            }
        }
        mailboxRouteRepository.allRetiring().getOrThrow().forEach { credential ->
            movedAccessEndpoint(credential, availableNodes)?.let { accessEndpoint ->
                mailboxRouteRepository.saveRetiring(credential.copy(accessEndpoint = accessEndpoint)).getOrThrow()
            }
        }
    }

    private fun movedAccessEndpoint(credential: LocalMailboxCredential, availableNodes: List<NodeEndpoint>): String? =
        availableNodes
            .firstOrNull { node -> node.nodeId == credential.deliveryRoute.nodeId }
            ?.mailboxAccessEndpoint
            ?.takeIf { accessEndpoint -> accessEndpoint != credential.accessEndpoint }

    /**
     * A retiring mailbox is revoked only after it expired, and only once the pending synchronization emptied it
     * (or it stayed expired for [RETIRING_GRACE_MILLISECONDS]): the node deletes stored envelopes together with
     * the mailbox. A mailbox whose node left the signed directory is dropped without a revocation, like a current
     * one. A failed revocation while the node is unreachable is retried by the next provisioning; any other failure
     * drops the local copy, the node expires the mailbox on its own.
     */
    private suspend fun revokeExpiredRetiringMailboxes(now: Long, availableNodes: List<NodeEndpoint>) {
        mailboxRouteRepository
            .allRetiring()
            .getOrThrow()
            .filter { credential ->
                !credential.revocationPending && credential.deliveryRoute.expiresAtEpochMilliseconds <= now
            }.forEach { credential ->
                if (credential.isRetiredFrom(availableNodes)) {
                    mailboxRouteRepository.deleteRetiring(credential.deliveryRoute.routeId).getOrThrow()
                    return@forEach
                }
                if (credential.stillHoldsEnvelopes(now)) return@forEach
                mailboxGateway.revoke(credential)
                    .onSuccess { mailboxRouteRepository.deleteRetiring(credential.deliveryRoute.routeId).getOrThrow() }
                    .onFailure { failure ->
                        if (failure is CancellationException) throw failure
                        if (failure.isRecoverableConnectivityFailure()) return@onFailure
                        SparrowLog.diagnostic(
                            "MailboxRouteProvisioner",
                            "Expired mailbox of ${credential.contactId} could not be revoked",
                            failure
                        )
                        mailboxRouteRepository.deleteRetiring(credential.deliveryRoute.routeId).getOrThrow()
                    }
            }
    }

    /** Envelopes still wait in the expired mailbox and the grace period is not over: synchronize them first. */
    private suspend fun LocalMailboxCredential.stillHoldsEnvelopes(now: Long): Boolean {
        if (deliveryRoute.expiresAtEpochMilliseconds + RETIRING_GRACE_MILLISECONDS <= now) return false
        val pending = mailboxGateway.pending(this).getOrElse { failure ->
            if (failure is CancellationException) throw failure
            // An unreachable node may still hold envelopes; anything else is left to the revocation.
            return failure.isRecoverableConnectivityFailure()
        }
        return pending.isNotEmpty()
    }

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
        // The old mailbox may still hold envelopes, and the contact keeps sending to it until the queued route
        // packet arrived. It stays readable as a retiring mailbox and is revoked only after its expiry.
        val saved =
            if (current != null) {
                mailboxRouteRepository.retireLocal(current, replacement)
            } else {
                mailboxRouteRepository.saveLocal(replacement)
            }
        saved.getOrElse { persistenceError ->
            mailboxGateway.revoke(replacement)
                .onFailure { failure ->
                    SparrowLog.diagnostic("MailboxRouteProvisioner", "Mailbox replacement cleanup failed for $contactId", failure)
                }
            throw persistenceError
        }
    }

    private companion object {
        const val ROUTE_LIFETIME_MILLISECONDS = 30L * 24L * 60L * 60L * 1_000L
        const val RENEWAL_WINDOW_MILLISECONDS = 3L * 24L * 60L * 60L * 1_000L
        const val RETIRING_GRACE_MILLISECONDS = 7L * 24L * 60L * 60L * 1_000L
    }
}

/** The node of this mailbox is still in the directory, but under another route address. */
internal fun LocalMailboxCredential.hasMovedIn(availableNodes: List<NodeEndpoint>): Boolean =
    availableNodes.any { node ->
        node.nodeId == deliveryRoute.nodeId &&
            node.mailboxRouteEndpoint != null &&
            node.mailboxRouteEndpoint != deliveryRoute.nodeEndpoint
    }

/** Do not discard a mailbox merely because its original node is temporarily unreachable. */
internal fun LocalMailboxCredential.isRetiredFrom(availableNodes: List<NodeEndpoint>): Boolean =
    availableNodes.isNotEmpty() && availableNodes.none { node -> node.nodeId == deliveryRoute.nodeId }
