package com.cbgm.sparrow.feature.transport.domain.usecase

import com.cbgm.sparrow.core.crypto.signature.DetachedSignatureCrypto
import com.cbgm.sparrow.core.util.result.safeSuspendCall
import com.cbgm.sparrow.feature.transport.mailbox.MailboxWakeTicket
import com.cbgm.sparrow.feature.transport.mailbox.mailboxWakeTicketPayload
import com.cbgm.sparrow.feature.transport.routing.LocalRoutingIdProvider
import com.cbgm.sparrow.protocol.identity.LocalSigningKeyPairProvider

/**
 * Signs the permission for one mailbox node to wake this device, valid until the mailbox expires.
 * It is handed to the mailbox node when the mailbox is created and is not kept on the device.
 */
class CreateMailboxWakeTicketUseCase(
    private val localRoutingIdProvider: LocalRoutingIdProvider,
    private val signingKeyPairProvider: LocalSigningKeyPairProvider,
    private val signatureCrypto: DetachedSignatureCrypto
) {
    suspend operator fun invoke(
        nodeId: String,
        expiresAtEpochMilliseconds: Long
    ): Result<MailboxWakeTicket> =
        safeSuspendCall {
            require(nodeId.isNotBlank()) { "Mailbox node ID must not be blank" }
            val routingId = localRoutingIdProvider.getLocalRoutingId().getOrThrow()
            val signingKeyPair = signingKeyPairProvider.getSigningKeyPair().getOrThrow()
            val payload = mailboxWakeTicketPayload(routingId, nodeId, expiresAtEpochMilliseconds)
            MailboxWakeTicket(
                routingId = routingId,
                nodeId = nodeId,
                expiresAtEpochMilliseconds = expiresAtEpochMilliseconds,
                clientSigningPublicKey = signingKeyPair.publicKey,
                clientSignature =
                    signatureCrypto
                        .sign(payload = payload, signingPrivateKey = signingKeyPair.privateKey)
                        .getOrThrow()
            )
        }
}
