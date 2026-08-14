package com.cbgm.sparrow.feature.contacts.data.incoming.handler

import com.cbgm.sparrow.core.crypto.identity.IdentityAcknowledgementCrypto
import com.cbgm.sparrow.core.protocol.handler.IncomingPacketContext
import com.cbgm.sparrow.core.protocol.handler.TypedProtocolPacketHandler
import com.cbgm.sparrow.core.protocol.identity.LocalPublicIdentityProvider
import com.cbgm.sparrow.core.protocol.packet.IdentityAcknowledgementPacket
import com.cbgm.sparrow.core.protocol.packet.SparrowPacket
import com.cbgm.sparrow.feature.contacts.domain.repository.ContactKeyExchangeRepository
import com.cbgm.sparrow.feature.contacts.domain.repository.ContactRepository
import com.cbgm.sparrow.feature.contacts.domain.repository.ContactVerificationRepository

class IdentityAcknowledgementPacketHandler(
    private val contactRepository: ContactRepository,
    private val contactKeyExchangeRepository: ContactKeyExchangeRepository,
    private val localPublicIdentityProvider: LocalPublicIdentityProvider,
    private val identityAcknowledgementCrypto: IdentityAcknowledgementCrypto,
    private val contactVerificationRepository: ContactVerificationRepository
) : TypedProtocolPacketHandler {
    override fun canHandle(packet: SparrowPacket): Boolean = packet is IdentityAcknowledgementPacket

    override suspend fun handle(
        context: IncomingPacketContext,
        packet: SparrowPacket
    ): Result<Unit> =
        runCatching {
            val acknowledgement =
                packet as? IdentityAcknowledgementPacket
                    ?: error("IdentityAcknowledgementPacketHandler received an incompatible packet")

            val remoteIdentity =
                contactRepository
                    .getContact(context.contactId)
                    .getOrThrow()
                    ?.sparrowIdentity
                    ?: return@runCatching

            if (!remoteIdentity.locallyImported) {
                return@runCatching
            }

            check(acknowledgement.senderSigningPublicKey.contentEquals(remoteIdentity.signingPublicKey)) {
                "Acknowledgement sender signing key does not match the imported contact identity"
            }

            val localIdentity = localPublicIdentityProvider.getLocalPublicIdentity().getOrThrow()

            check(acknowledgement.acknowledgedEncryptionPublicKey.contentEquals(localIdentity.encryptionPublicKey)) {
                "Acknowledgement refers to a different local encryption key"
            }

            check(acknowledgement.acknowledgedSigningPublicKey.contentEquals(localIdentity.signingPublicKey)) {
                "Acknowledgement refers to a different local signing key"
            }

            identityAcknowledgementCrypto
                .verify(
                    acknowledgedEncryptionPublicKey = acknowledgement.acknowledgedEncryptionPublicKey,
                    acknowledgedSigningPublicKey = acknowledgement.acknowledgedSigningPublicKey,
                    senderSigningPublicKey = remoteIdentity.signingPublicKey,
                    signature = acknowledgement.signature
                ).getOrThrow()

            contactKeyExchangeRepository
                .markMutual(
                    contactId = context.contactId,
                    expectedRemoteEncryptionPublicKey = remoteIdentity.encryptionPublicKey,
                    expectedRemoteSigningPublicKey = remoteIdentity.signingPublicKey
                ).getOrThrow()

            contactVerificationRepository
                .sendReceiptIfLocallyVerified(context.contactId)
                .getOrThrow()
        }
}
