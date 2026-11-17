package com.cbgm.sparrow.server.mailbox

import com.cbgm.sparrow.server.protocol.MailboxWaitEntry
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onSubscription
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.time.Duration.Companion.milliseconds

/**
 * Lets an owner wait for envelopes instead of asking again and again: [stored] is told about every new envelope,
 * [ready] answers as soon as one of the watched mailboxes holds one. Only envelopes stored by this process wake a
 * waiter early; with several mailbox processes the others are seen when the wait ends and the owner asks again.
 */
internal class MailboxArrivals {
    private val arrivals = MutableSharedFlow<String>(extraBufferCapacity = BUFFERED_ARRIVALS, onBufferOverflow = BufferOverflow.DROP_OLDEST)

    fun stored(mailboxId: String) {
        arrivals.tryEmit(mailboxId)
    }

    /** The mailboxes of [entries] holding envelopes, waiting up to [waitMilliseconds] for one; mailboxes the capability does not open never count. */
    suspend fun ready(
        store: MailboxStorage,
        entries: List<MailboxWaitEntry>,
        waitMilliseconds: Long
    ): List<String> {
        val watched = entries.mapTo(HashSet(), MailboxWaitEntry::mailboxId)
        return withTimeoutOrNull(waitMilliseconds.milliseconds) {
            arrivals
                // Checked once subscribed, so an envelope stored just before the wait is not missed.
                .onSubscription { emit(CHECK_NOW) }
                .filter { mailboxId -> mailboxId == CHECK_NOW || mailboxId in watched }
                .map { holding(store, entries) }
                .first { ready -> ready.isNotEmpty() }
        } ?: emptyList()
    }

    private suspend fun holding(
        store: MailboxStorage,
        entries: List<MailboxWaitEntry>
    ): List<String> =
        entries
            .filter { entry -> store.pending(entry.mailboxId, entry.retrievalCapability)?.isNotEmpty() == true }
            .map(MailboxWaitEntry::mailboxId)

    private companion object {
        const val BUFFERED_ARRIVALS = 1_024
        const val CHECK_NOW = ""
    }
}
