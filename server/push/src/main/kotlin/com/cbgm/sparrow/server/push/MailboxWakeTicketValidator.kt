package com.cbgm.sparrow.server.push

import com.cbgm.sparrow.server.protocol.MailboxWakeTicket
import com.cbgm.sparrow.server.security.ProtocolSignatures

/**
 * Decides whether a mailbox node may wake a device. Anyone can run a mailbox node, so the node must
 * show the device's own signed permission for exactly this node, and it only lasts as long as the
 * mailbox it was given for.
 */
internal object MailboxWakeTicketValidator {
    fun allows(
        ticket: MailboxWakeTicket,
        recipientId: String,
        nodeId: String?,
        nowEpochMilliseconds: Long
    ): Boolean =
        ticket.routingId == recipientId &&
            ticket.nodeId == nodeId &&
            ticket.expiresAtEpochMilliseconds > nowEpochMilliseconds &&
            ProtocolSignatures.verifyMailboxWakeTicket(ticket)
}
