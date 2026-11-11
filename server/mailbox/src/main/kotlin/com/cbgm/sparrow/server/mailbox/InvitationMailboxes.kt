package com.cbgm.sparrow.server.mailbox

/**
 * The limits of an invitation mailbox ([com.cbgm.sparrow.server.protocol.CreateMailboxRequest.invitation]). Its send
 * capability is public, so every deposit is paid with a proof of work, an envelope is small (a first invitation or
 * the packets before a contact's own mailbox route arrived) and the mailbox holds only a few of them.
 */
internal object InvitationMailboxes {
    const val MAXIMUM_ENVELOPES = 50
    const val MAXIMUM_ENVELOPE_BYTES = 64L * 1_024L

    /** Why a deposit is refused, or null when it may be stored. */
    fun rejection(
        storedEnvelopes: Int,
        payloadBytes: Long,
        depositPaid: Boolean
    ): String? =
        when {
            !depositPaid -> "DEPOSIT_WORK_REQUIRED"
            payloadBytes > MAXIMUM_ENVELOPE_BYTES -> "ENVELOPE_TOO_LARGE"
            storedEnvelopes >= MAXIMUM_ENVELOPES -> "MAILBOX_QUOTA_EXCEEDED"
            else -> null
        }
}
