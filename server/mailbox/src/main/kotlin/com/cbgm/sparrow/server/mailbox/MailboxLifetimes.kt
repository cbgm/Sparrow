package com.cbgm.sparrow.server.mailbox

import com.cbgm.sparrow.server.protocol.CreateMailboxRequest
import com.cbgm.sparrow.server.protocol.FederatedEnvelope

/**
 * The requester chooses how long a mailbox and an envelope live, but never beyond these caps: otherwise a few
 * clients could hold mailboxes or envelopes forever. Apps ask for 30 days per mailbox and the gateway for 7 days
 * per envelope, so both stay unchanged.
 */
internal object MailboxLifetimes {
    const val MAXIMUM_MAILBOX_LIFETIME_MILLISECONDS = 31L * 24L * 60L * 60L * 1_000L
    const val MAXIMUM_ENVELOPE_LIFETIME_MILLISECONDS = 7L * 24L * 60L * 60L * 1_000L

    fun capped(request: CreateMailboxRequest, nowEpochMilliseconds: Long): CreateMailboxRequest =
        request.copy(
            expiresAtEpochMilliseconds =
                minOf(request.expiresAtEpochMilliseconds, nowEpochMilliseconds + MAXIMUM_MAILBOX_LIFETIME_MILLISECONDS)
        )

    fun capped(envelope: FederatedEnvelope, nowEpochMilliseconds: Long): FederatedEnvelope =
        envelope.copy(
            expiresAtEpochMilliseconds =
                minOf(envelope.expiresAtEpochMilliseconds, nowEpochMilliseconds + MAXIMUM_ENVELOPE_LIFETIME_MILLISECONDS)
        )
}
