package com.cbgm.sparrow.feature.contacts.domain.usecase

import com.cbgm.sparrow.core.crypto.identity.IdentityAcknowledgementCrypto
import com.cbgm.sparrow.core.id.IdGenerator
import com.cbgm.sparrow.core.protocol.handler.IncomingPacketContext
import com.cbgm.sparrow.core.protocol.identity.LocalSigningKeyPairProvider
import com.cbgm.sparrow.core.protocol.outbox.ProtocolOutbox
import com.cbgm.sparrow.core.protocol.packet.IdentityAcknowledgementPacket
import com.cbgm.sparrow.core.protocol.packet.IdentityPacket
import com.cbgm.sparrow.feature.contacts.domain.model.KeyExchangeStatus
import com.cbgm.sparrow.feature.contacts.domain.model.RemoteIdentityOrigin
import com.cbgm.sparrow.feature.contacts.domain.repository.ContactKeyExchangeRepository
import com.cbgm.sparrow.feature.contacts.domain.repository.ContactRepository
import com.cbgm.sparrow.feature.contacts.domain.repository.ContactVerificationRepository

class HandleIdentityPacketUseCase(
    private val contactRepository: ContactRepository,
    private val contactKeyExchangeRepository: ContactKeyExchangeRepository,
    private val localSigningKeyPairProvider: LocalSigningKeyPairProvider,
    private val identityAcknowledgementCrypto: IdentityAcknowledgementCrypto,
    private val protocolOutbox: ProtocolOutbox,
    private val contactVerificationRepository: ContactVerificationRepository
) {
    suspend operator fun invoke(
        context: IncomingPacketContext,
        packet: IdentityPacket
    ): Result<Unit> =
        runCatching {
            val pinnedIdentity =
                contactRepository
                    .getContact(context.contactId)
                    .getOrThrow()
                    ?.sparrowIdentity
                    ?: return@runCatching

            if (!pinnedIdentity.locallyImported) {
                return@runCatching
            }

            check(pinnedIdentity.encryptionPublicKey.contentEquals(packet.encryptionPublicKey)) {
                "Manual identity packet does not match the imported encryption key"
            }
            check(pinnedIdentity.signingPublicKey.contentEquals(packet.signingPublicKey)) {
                "Manual identity packet does not match the imported signing key"
            }

            val update =
                contactKeyExchangeRepository
                    .storeRemoteIdentity(
                        contactId = context.contactId,
                        encryptionPublicKey = packet.encryptionPublicKey,
                        signingPublicKey = packet.signingPublicKey,
                        origin = RemoteIdentityOrigin.REMOTE_PACKET
                    ).getOrThrow()

            val localSigningKeyPair =
                localSigningKeyPairProvider
                    .getSigningKeyPair()
                    .getOrThrow()

            val signature =
                identityAcknowledgementCrypto
                    .sign(
                        acknowledgedEncryptionPublicKey = packet.encryptionPublicKey,
                        acknowledgedSigningPublicKey = packet.signingPublicKey,
                        senderSigningPublicKey = localSigningKeyPair.publicKey,
                        senderSigningPrivateKey = localSigningKeyPair.privateKey
                    ).getOrThrow()

            protocolOutbox
                .enqueue(
                    contactId = context.contactId,
                    packet =
                        IdentityAcknowledgementPacket(
                            packetId = IdGenerator.generate(),
                            senderSigningPublicKey = localSigningKeyPair.publicKey.copyOf(),
                            acknowledgedEncryptionPublicKey = packet.encryptionPublicKey.copyOf(),
                            acknowledgedSigningPublicKey = packet.signingPublicKey.copyOf(),
                            signature = signature.copyOf()
                        )
                ).getOrThrow()

            if (update.keyExchangeStatus == KeyExchangeStatus.MUTUAL) {
                contactVerificationRepository
                    .sendReceiptIfLocallyVerified(context.contactId)
                    .getOrThrow()
            }
        }
}
