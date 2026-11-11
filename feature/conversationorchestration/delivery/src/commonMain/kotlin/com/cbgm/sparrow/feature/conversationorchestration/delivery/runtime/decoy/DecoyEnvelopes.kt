package com.cbgm.sparrow.feature.conversationorchestration.delivery.runtime.decoy

import com.cbgm.sparrow.core.util.logging.SparrowLog
import com.cbgm.sparrow.core.util.result.safeSuspendCall
import com.cbgm.sparrow.core.util.time.SystemClock
import com.cbgm.sparrow.feature.transport.circuit.DecoyCountStore
import com.cbgm.sparrow.feature.transport.gateway.model.FederatedEnvelope
import com.cbgm.sparrow.feature.transport.mailbox.InvitationMailboxRenewal
import com.cbgm.sparrow.feature.transport.mailbox.MailboxGateway
import com.cbgm.sparrow.protocol.mailbox.MailboxRouteRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.getAndUpdate
import kotlinx.coroutines.launch
import kotlin.random.Random

/**
 * Decoy envelopes for a send, as many as the user chose ([DecoyCountStore], off by default): each holds a cover
 * packet for another contact picked at random ([DecoySealer]) and goes into that contact's mailbox at the same moment as
 * the real one ([depositAlongside]). A mailbox node then cannot tell from timing which mailboxes a send was for, for
 * example the members of a group. Decoys go out only through a protecting circuit (outside one the mailbox node would
 * see the device's address anyway), and only one burst per [BURST_WINDOW_MILLISECONDS], so a group fan-out gets one
 * set rather than one per member. Nothing is queued or retried.
 */
class DecoyEnvelopes(
    private val decoyCountStore: DecoyCountStore,
    private val hasProtectingCircuit: () -> Boolean,
    private val mailboxRouteRepository: MailboxRouteRepository,
    private val sealer: DecoySealer,
    private val mailboxGateway: MailboxGateway,
    private val random: Random = Random.Default,
    private val now: () -> Long = SystemClock::nowEpochMilliseconds,
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
) {
    private val logger = SparrowLog.withTag("DecoyEnvelopes")
    private val lastBurstAt = MutableStateFlow(NO_BURST)

    /** The decoys for a send to [recipientContactId], ready to go out; empty when decoys are off or not due. */
    suspend fun prepare(recipientContactId: String): List<FederatedEnvelope> =
        safeSuspendCall(logFailure = false) {
            val count = decoyCountStore.get()
            if (count == 0 || !hasProtectingCircuit() || !startsBurst()) return@safeSuspendCall emptyList()
            val createdAt = now()
            mailboxRouteRepository
                .allLocal()
                .getOrThrow()
                .map { credential -> credential.contactId }
                .filter { contactId -> contactId != recipientContactId && contactId != InvitationMailboxRenewal.CONTACT_ID }
                .distinct()
                .shuffled(random)
                .take(count * CANDIDATES_PER_DECOY)
                .mapNotNull { contactId -> sealer.seal(contactId, createdAt) }
                .take(count)
        }.onFailure { error -> logger.info { "No decoys for this send: ${error.message}" } }
            .getOrDefault(emptyList())

    /** Puts [decoys] into their mailboxes now, next to the real envelope; failures are only logged. */
    fun depositAlongside(decoys: List<FederatedEnvelope>) {
        decoys.forEach { decoy ->
            scope.launch {
                mailboxGateway.deliver(decoy).onFailure { error -> logger.debug { "Decoy not deposited: ${error.message}" } }
            }
        }
    }

    private fun startsBurst(): Boolean {
        val time = now()
        val previous = lastBurstAt.getAndUpdate { last -> if (time - last < BURST_WINDOW_MILLISECONDS) last else time }
        return time - previous >= BURST_WINDOW_MILLISECONDS
    }

    private companion object {
        const val BURST_WINDOW_MILLISECONDS = 1_000L
        const val NO_BURST = Long.MIN_VALUE / 2

        /** Some contacts have no route yet; a few spare candidates keep the count without sealing for all of them. */
        const val CANDIDATES_PER_DECOY = 3
    }
}
