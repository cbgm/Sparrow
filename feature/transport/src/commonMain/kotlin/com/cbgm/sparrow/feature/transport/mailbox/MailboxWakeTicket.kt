package com.cbgm.sparrow.feature.transport.mailbox

import com.cbgm.sparrow.feature.transport.signing.lengthPrefixed
import com.cbgm.sparrow.feature.transport.signing.toBigEndianBytes
import kotlinx.serialization.Serializable

/**
 * Lets exactly one mailbox node wake this device through push, for as long as the mailbox lives.
 * Anyone can run a mailbox node, so push refuses wake-ups from a node without such a ticket.
 */
@Serializable
class MailboxWakeTicket(
    val routingId: String,
    val nodeId: String,
    val expiresAtEpochMilliseconds: Long,
    val clientSigningPublicKey: ByteArray,
    val clientSignature: ByteArray
)

/**
 * Bytes signed for a [MailboxWakeTicket]. Must match the server's ProtocolSignatures.mailboxWakeContent
 * byte for byte: length-prefixed fields, its own domain string, expiry as 8-byte big-endian.
 */
fun mailboxWakeTicketPayload(
    routingId: String,
    nodeId: String,
    expiresAtEpochMilliseconds: Long
): ByteArray =
    lengthPrefixed(
        MAILBOX_WAKE_DOMAIN.encodeToByteArray(),
        routingId.encodeToByteArray(),
        nodeId.encodeToByteArray(),
        expiresAtEpochMilliseconds.toBigEndianBytes()
    )

private const val MAILBOX_WAKE_DOMAIN = "sparrow-mailbox-wake-v1"
