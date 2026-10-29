package com.cbgm.sparrow.server.push

import com.cbgm.sparrow.server.protocol.MailboxWakeTicket
import com.cbgm.sparrow.server.security.ClientRoutingIds
import com.cbgm.sparrow.server.security.NodeIdentity
import com.cbgm.sparrow.server.security.ProtocolSignatures
import com.cbgm.sparrow.server.security.Signatures
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class MailboxWakeTicketValidatorTest {
    private val device = NodeIdentity.generate()
    private val devicePublicKey = device.encodedPublicKey.takeLast(RAW_KEY_BYTES).toByteArray()
    private val routingId = ClientRoutingIds.fromSigningPublicKey(devicePublicKey)

    @Test
    fun theNamedMailboxNodeMayWakeTheDevice() {
        assertTrue(allows(ticket()))
    }

    @Test
    fun anotherNodeCannotUseTheTicket() {
        assertFalse(allows(ticket(), nodeId = "other-node"))
    }

    @Test
    fun ticketOnlyWakesItsOwnDevice() {
        val other = ClientRoutingIds.fromSigningPublicKey(ByteArray(RAW_KEY_BYTES) { 7 })
        assertFalse(allows(ticket(), recipientId = other))
    }

    @Test
    fun expiredTicketIsRefused() {
        assertFalse(allows(ticket(expiresAt = NOW)))
    }

    @Test
    fun ticketSignedByAnotherKeyIsRefused() {
        val forged = ticket(signer = NodeIdentity.generate())
        assertFalse(allows(forged))
    }

    @Test
    fun ticketForAnotherNodeCannotBeRelabelled() {
        val signed = ticket()
        val relabelled =
            MailboxWakeTicket(routingId, "other-node", signed.expiresAtEpochMilliseconds, devicePublicKey, signed.clientSignature)
        assertFalse(allows(relabelled, nodeId = "other-node"))
    }

    private fun allows(
        ticket: MailboxWakeTicket,
        recipientId: String = routingId,
        nodeId: String = NODE_ID
    ) = MailboxWakeTicketValidator.allows(ticket, recipientId, nodeId, NOW)

    private fun ticket(
        expiresAt: Long = NOW + 60_000L,
        signer: NodeIdentity = device
    ) = MailboxWakeTicket(
        routingId = routingId,
        nodeId = NODE_ID,
        expiresAtEpochMilliseconds = expiresAt,
        clientSigningPublicKey = devicePublicKey,
        clientSignature =
            Signatures.sign(ProtocolSignatures.mailboxWakeContent(routingId, NODE_ID, expiresAt), signer.privateKey)
    )

    private companion object {
        const val RAW_KEY_BYTES = 32
        const val NODE_ID = "mailbox-node"
        const val NOW = 1_000_000L
    }
}
