package com.cbgm.sparrow.server.security

import com.cbgm.sparrow.server.protocol.MailboxWakeTicket
import com.cbgm.sparrow.server.protocol.serverJson

/**
 * Smoke tests: a wake ticket from a fresh device key that lets node [nodeId] wake it. The device has no push
 * registration, so the push API answers 404 for a correctly signed node request.
 */
object MailboxWakeTicketCli {
    @JvmStatic
    fun main(arguments: Array<String>) {
        require(arguments.size == 1) { "Usage: <node-id>" }
        val device = NodeIdentity.generate()
        val routingId = ClientRoutingIds.fromSigningPublicKey(device.encodedPublicKey)
        val expiresAt = System.currentTimeMillis() + TICKET_LIFETIME_MILLISECONDS
        val signature =
            Signatures.sign(ProtocolSignatures.mailboxWakeContent(routingId, arguments[0], expiresAt), device.privateKey)
        val ticket = MailboxWakeTicket(routingId, arguments[0], expiresAt, device.encodedPublicKey, signature)

        CommandLineOutput.write(listOf("routingId=$routingId", "ticket=${serverJson.encodeToString(ticket)}"))
    }

    private const val TICKET_LIFETIME_MILLISECONDS = 10L * 60L * 1_000L
}
