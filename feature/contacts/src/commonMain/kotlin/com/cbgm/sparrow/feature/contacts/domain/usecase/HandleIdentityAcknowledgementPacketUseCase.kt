package com.cbgm.sparrow.feature.contacts.domain.usecase

import com.cbgm.sparrow.core.crypto.identity.IdentityAcknowledgementCrypto
import com.cbgm.sparrow.core.protocol.handler.IncomingPacketContext
import com.cbgm.sparrow.core.protocol.identity.LocalPublicIdentityProvider
import com.cbgm.sparrow.core.protocol.packet.IdentityAcknowledgementPacket
import com.cbgm.sparrow.feature.contacts.domain.repository.ContactKeyExchangeRepository
import com.cbgm.sparrow.feature.contacts.domain.repository.ContactRepository
import com.cbgm.sparrow.feature.contacts.domain.repository.ContactVerificationRepository

class HandleIdentityAcknowledgementPacketUseCase(
    private val contactRepository: ContactRepository,
    private val contactKeyExchangeRepository: ContactKeyExchangeRepository,
    private val localPublicIdentityProvider: LocalPublicIdentityProvider,
    private val identityAcknowledgementCrypto: IdentityAcknowledgementCrypto,
    private val contactVerificationRepository: ContactVerificationRepository
) {
    suspend operator fun invoke(
        context: IncomingPacketContext,
        packet: IdentityAcknowledgementPacket
    ): Result<Unit> =
        runCatching {
            val remoteIdentity =
                contactRepository
                    .getContact(context.contactId)
                    .getOrThrow()
                    ?.sparrowIdentity
                    ?: return@runCatching

            if (!remoteIdentity.locallyImported) {
                return@runCatching
            }

            check(packet.senderSigningPublicKey.contentEquals(remoteIdentity.signingPublicKey)) {
                "Acknowledgement sender signing key does not match the imported contact identity"
            }

            val localIdentity =
                localPublicIdentityProvider
                    .getLocalPublicIdentity()
                    .getOrThrow()

            check(packet.acknowledgedEncryptionPublicKey.contentEquals(localIdentity.encryptionPublicKey)) {
                "Acknowledgement refers to a different local encryption key"
            }
            check(packet.acknowledgedSigningPublicKey.contentEquals(localIdentity.signingPublicKey)) {
                "Acknowledgement refers to a different local signing key"
            }

            identityAcknowledgementCrypto
                .verify(
                    acknowledgedEncryptionPublicKey = packet.acknowledgedEncryptionPublicKey,
                    acknowledgedSigningPublicKey = packet.acknowledgedSigningPublicKey,
                    senderSigningPublicKey = remoteIdentity.signingPublicKey,
                    signature = packet.signature
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
