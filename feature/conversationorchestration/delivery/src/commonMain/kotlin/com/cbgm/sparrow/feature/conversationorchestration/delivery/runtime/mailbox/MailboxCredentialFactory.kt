package com.cbgm.sparrow.feature.conversationorchestration.delivery.runtime.mailbox

import com.cbgm.sparrow.core.crypto.signature.DetachedSignatureCrypto
import com.cbgm.sparrow.feature.messaging.runtime.mailbox.MailboxRoutePayloadEncoder
import com.cbgm.sparrow.feature.transport.domain.repository.MailboxRepository
import com.cbgm.sparrow.protocol.identity.LocalSigningKeyPairSource
import com.cbgm.sparrow.protocol.mailbox.LocalMailboxCredential

class MailboxCredentialFactory(
    private val mailboxRepository: MailboxRepository,
    private val signingKeyPairSource: LocalSigningKeyPairSource,
    private val signatureCrypto: DetachedSignatureCrypto,
    private val payloadEncoder: MailboxRoutePayloadEncoder
) {
    suspend fun create(
        contactId: String,
        nodeId: String,
        routeEndpoint: String,
        accessEndpoint: String,
        sequence: Long,
        expiresAtEpochMilliseconds: Long
    ): LocalMailboxCredential {
        val created =
            mailboxRepository
                .create(
                    contactId = contactId,
                    nodeId = nodeId,
                    routeEndpoint = routeEndpoint,
                    accessEndpoint = accessEndpoint,
                    sequence = sequence,
                    expiresAtEpochMilliseconds = expiresAtEpochMilliseconds
                ).getOrThrow()
        val keyPair = signingKeyPairSource.getSigningKeyPair().getOrThrow()
        val unsignedRoute = created.deliveryRoute.copy(identitySignature = byteArrayOf())
        val signature =
            signatureCrypto
                .sign(payloadEncoder.encode(unsignedRoute), keyPair.privateKey)
                .getOrThrow()
        return created.copy(
            deliveryRoute = unsignedRoute.copy(identitySignature = signature)
        )
    }
}
