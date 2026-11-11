package com.cbgm.sparrow.feature.conversationorchestration.delivery.runtime.mailbox

import com.cbgm.sparrow.core.util.logging.SparrowLog
import com.cbgm.sparrow.core.util.result.safeSuspendCall
import com.cbgm.sparrow.feature.transport.mailbox.InvitationMailboxRenewal
import com.cbgm.sparrow.feature.transport.mailbox.MailboxGateway
import com.cbgm.sparrow.protocol.mailbox.LocalMailboxCredential
import com.cbgm.sparrow.protocol.mailbox.MailboxRouteRepository
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlin.time.Duration.Companion.milliseconds

/**
 * Receives what contacts put into this device's mailboxes while the app is open, and what others put into its
 * invitation mailboxes: it keeps waiting at every node that holds some of them (a wait lasts a few seconds, then the
 * next one starts), and as soon as mailboxes hold envelopes they are fetched and processed. Senders put envelopes there
 * themselves, through their circuits, so this is how messages arrive live.
 */
class MailboxArrivalListener(
    private val mailboxGateway: MailboxGateway,
    private val mailboxRouteRepository: MailboxRouteRepository,
    private val pendingSynchronizer: MailboxPendingSynchronizer,
    private val invitationMailbox: InvitationMailboxRenewal,
    private val pauseMilliseconds: Long = DEFAULT_PAUSE_MILLISECONDS
) {
    private val logger = SparrowLog.withTag("MailboxArrivalListener")

    suspend fun listen() {
        while (currentCoroutineContext().isActive) {
            safeSuspendCall(logFailure = false) { receiveOnce() }
                .onSuccess { processed -> if (processed > 0) logger.debug { "Mailbox envelopes processed as they arrived: $processed" } }
                .onFailure { error ->
                    logger.info { "Waiting for mailbox envelopes failed (${error.message}); again in ${pauseMilliseconds / 1_000} s" }
                    delay(pauseMilliseconds.milliseconds)
                }
        }
    }

    /** Waits once at every node; how many envelopes arrived and were processed. */
    private suspend fun receiveOnce(): Int {
        val credentials = mailboxRouteRepository.receiving().getOrThrow() + invitationMailbox.receiving().getOrThrow()
        if (credentials.isEmpty()) {
            delay(pauseMilliseconds.milliseconds)
            return 0
        }
        val ready = readyMailboxes(credentials)
        if (ready.isEmpty()) return 0
        val processed = pendingSynchronizer.synchronize(credentials.filter { it.deliveryRoute.mailboxId in ready }).getOrThrow()
        // Envelopes that stay (an unknown sender) would make the next wait answer at once.
        if (processed == 0) delay(pauseMilliseconds.milliseconds)
        return processed
    }

    /** The mailboxes holding envelopes; a node that cannot be reached only fails the wait when every node failed. */
    private suspend fun readyMailboxes(credentials: List<LocalMailboxCredential>): Set<String> {
        val results =
            coroutineScope {
                credentials
                    .groupBy(LocalMailboxCredential::accessEndpoint)
                    .map { (accessEndpoint, atNode) -> async { mailboxGateway.ready(accessEndpoint, atNode) } }
                    .awaitAll()
            }
        results.firstNotNullOfOrNull { result -> result.exceptionOrNull() }?.takeIf { results.all { it.isFailure } }?.let { throw it }
        return results.flatMap { result -> result.getOrDefault(emptySet()) }.toSet()
    }

    private companion object {
        const val DEFAULT_PAUSE_MILLISECONDS = 5_000L
    }
}
