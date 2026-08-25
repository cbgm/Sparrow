package com.cbgm.sparrow.feature.messaging.runtime.mailbox

import com.cbgm.sparrow.core.crypto.signature.DetachedSignatureCrypto
import com.cbgm.sparrow.core.protocol.identity.LocalSigningKeyPairProvider
import com.cbgm.sparrow.core.protocol.mailbox.LocalMailboxCredential
import com.cbgm.sparrow.feature.transport.mailbox.MailboxGateway

class MailboxCredentialFactory(
    private val mailboxGateway: MailboxGateway,
    private val signingKeyPairProvider: LocalSigningKeyPairProvider,
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
            mailboxGateway
                .create(
                    contactId = contactId,
                    nodeId = nodeId,
                    routeEndpoint = routeEndpoint,
                    accessEndpoint = accessEndpoint,
                    sequence = sequence,
                    expiresAtEpochMilliseconds = expiresAtEpochMilliseconds
                ).getOrThrow()
        val keyPair = signingKeyPairProvider.getSigningKeyPair().getOrThrow()
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
