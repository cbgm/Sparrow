package com.cbgm.sparrow.feature.transport.mailbox

import com.cbgm.sparrow.core.util.result.safeSuspendCall
import com.cbgm.sparrow.core.util.time.SystemClock
import com.cbgm.sparrow.feature.transport.discovery.NodeEndpoint
import com.cbgm.sparrow.feature.transport.discovery.NodeEndpointResolver
import com.cbgm.sparrow.feature.transport.routing.LocalRoutingIdProvider
import com.cbgm.sparrow.protocol.mailbox.LocalMailboxCredential
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Keeps this device's invitation mailbox: the one anyone without a mailbox route of this device puts packets into (a
 * first invitation, or the answers before the device's own route arrived). It lives [LIFETIME_MILLISECONDS]; a week
 * before it expires, or when its node left the signed directory or moved, a new one is opened on the first mailbox
 * node, and the alias claims name the new one from then on. The old one stays readable until it expired; the node
 * deletes it then.
 */
class InvitationMailboxRenewal(
    private val nodeEndpointResolver: NodeEndpointResolver,
    private val localRoutingIdProvider: LocalRoutingIdProvider,
    private val mailboxGateway: MailboxGateway,
    private val store: InvitationMailboxStore,
    private val now: () -> Long = SystemClock::nowEpochMilliseconds
) {
    private val renewing = Mutex()

    /** The mailbox the alias claims name, opened or renewed first when needed. */
    suspend fun current(): Result<LocalMailboxCredential> =
        safeSuspendCall(logFailure = false) {
            renewing.withLock {
                val at = now()
                val stored = store.all()
                val live = stored.filter { mailbox -> mailbox.deliveryRoute.expiresAtEpochMilliseconds > at }
                // Without a fresh directory (offline) a mailbox that does not expire soon is kept as it is.
                val nodes = mailboxNodes()
                val current = live.firstOrNull()?.takeIf { mailbox -> mailbox.staysUsable(at, nodes.getOrNull()) }
                if (current != null) {
                    if (live != stored) store.replace(live)
                    return@withLock current
                }
                val node = nodes.getOrThrow().first()
                val opened =
                    mailboxGateway
                        .createInvitation(
                            nodeId = node.nodeId,
                            routeEndpoint = checkNotNull(node.mailboxRouteEndpoint),
                            accessEndpoint = checkNotNull(node.mailboxAccessEndpoint),
                            expiresAtEpochMilliseconds = at + LIFETIME_MILLISECONDS
                        ).getOrThrow()
                store.replace(listOf(opened) + live)
                opened
            }
        }

    /** Every invitation mailbox that may still hold envelopes. */
    suspend fun receiving(): Result<List<LocalMailboxCredential>> =
        safeSuspendCall(logFailure = false) {
            val at = now()
            store.all().filter { mailbox -> mailbox.deliveryRoute.expiresAtEpochMilliseconds > at }
        }

    /** [nodes] null: the directory could not be fetched, so only the expiry decides. */
    private fun LocalMailboxCredential.staysUsable(
        at: Long,
        nodes: List<NodeEndpoint>?
    ): Boolean =
        deliveryRoute.expiresAtEpochMilliseconds > at + RENEWAL_WINDOW_MILLISECONDS &&
            (
                nodes == null ||
                    nodes.any { node ->
                        node.nodeId == deliveryRoute.nodeId &&
                            node.mailboxRouteEndpoint == deliveryRoute.nodeEndpoint &&
                            node.mailboxAccessEndpoint == accessEndpoint
                    }
            )

    private suspend fun mailboxNodes(): Result<List<NodeEndpoint>> =
        safeSuspendCall(logFailure = false) {
            nodeEndpointResolver
                .resolve(localRoutingIdProvider.getLocalRoutingId().getOrThrow())
                .getOrThrow()
                .filter { node -> node.mailboxRouteEndpoint != null && node.mailboxAccessEndpoint != null }
                .also { nodes -> check(nodes.isNotEmpty()) { "No mailbox-capable node is available" } }
        }

    companion object {
        /** Names the invitation mailboxes where a mailbox credential names its contact. */
        const val CONTACT_ID = "invitation-mailbox"
        private const val LIFETIME_MILLISECONDS = 30L * 24L * 60L * 60L * 1_000L
        private const val RENEWAL_WINDOW_MILLISECONDS = 7L * 24L * 60L * 60L * 1_000L
    }
}
