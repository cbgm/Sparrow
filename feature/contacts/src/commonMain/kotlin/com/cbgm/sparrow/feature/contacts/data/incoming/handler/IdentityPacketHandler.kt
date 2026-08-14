package com.cbgm.sparrow.feature.contacts.data.incoming.handler

import com.cbgm.sparrow.core.crypto.identity.IdentityAcknowledgementCrypto
import com.cbgm.sparrow.core.id.IdGenerator
import com.cbgm.sparrow.core.protocol.handler.IncomingPacketContext
import com.cbgm.sparrow.core.protocol.handler.TypedProtocolPacketHandler
import com.cbgm.sparrow.core.protocol.identity.LocalSigningKeyPairProvider
import com.cbgm.sparrow.core.protocol.outbox.ProtocolOutbox
import com.cbgm.sparrow.core.protocol.packet.IdentityAcknowledgementPacket
import com.cbgm.sparrow.core.protocol.packet.IdentityPacket
import com.cbgm.sparrow.core.protocol.packet.SparrowPacket
import com.cbgm.sparrow.feature.contacts.domain.model.KeyExchangeStatus
import com.cbgm.sparrow.feature.contacts.domain.model.RemoteIdentityOrigin
import com.cbgm.sparrow.feature.contacts.domain.repository.ContactKeyExchangeRepository
import com.cbgm.sparrow.feature.contacts.domain.repository.ContactRepository
import com.cbgm.sparrow.feature.contacts.domain.repository.ContactVerificationRepository

class IdentityPacketHandler(
    private val contactRepository: ContactRepository,
    private val contactKeyExchangeRepository: ContactKeyExchangeRepository,
    private val localSigningKeyPairProvider: LocalSigningKeyPairProvider,
    private val identityAcknowledgementCrypto: IdentityAcknowledgementCrypto,
    private val protocolOutbox: ProtocolOutbox,
    private val contactVerificationRepository: ContactVerificationRepository
) : TypedProtocolPacketHandler {
    override fun canHandle(packet: SparrowPacket): Boolean = packet is IdentityPacket

    override suspend fun handle(
        context: IncomingPacketContext,
        packet: SparrowPacket
    ): Result<Unit> =
        runCatching {
            val identityPacket =
                packet as? IdentityPacket
                    ?: error("IdentityPacketHandler received an incompatible packet")

            val pinnedIdentity =
                contactRepository
                    .getContact(context.contactId)
                    .getOrThrow()
                    ?.sparrowIdentity
                    ?: return@runCatching

            if (!pinnedIdentity.locallyImported) {
                return@runCatching
            }

            check(pinnedIdentity.encryptionPublicKey.contentEquals(identityPacket.encryptionPublicKey)) {
                "Manual identity packet does not match the imported encryption key"
            }
            check(pinnedIdentity.signingPublicKey.contentEquals(identityPacket.signingPublicKey)) {
                "Manual identity packet does not match the imported signing key"
            }

            val update =
                contactKeyExchangeRepository
                    .storeRemoteIdentity(
                        contactId = context.contactId,
                        encryptionPublicKey = identityPacket.encryptionPublicKey,
                        signingPublicKey = identityPacket.signingPublicKey,
                        origin = RemoteIdentityOrigin.REMOTE_PACKET
                    ).getOrThrow()

            val localSigningKeyPair = localSigningKeyPairProvider.getSigningKeyPair().getOrThrow()

            val signature =
                identityAcknowledgementCrypto
                    .sign(
                        acknowledgedEncryptionPublicKey = identityPacket.encryptionPublicKey,
                        acknowledgedSigningPublicKey = identityPacket.signingPublicKey,
                        senderSigningPublicKey = localSigningKeyPair.publicKey,
                        senderSigningPrivateKey = localSigningKeyPair.privateKey
                    ).getOrThrow()

            val acknowledgement =
                IdentityAcknowledgementPacket(
                    packetId = IdGenerator.generate(),
                    senderSigningPublicKey = localSigningKeyPair.publicKey.copyOf(),
                    acknowledgedEncryptionPublicKey = identityPacket.encryptionPublicKey.copyOf(),
                    acknowledgedSigningPublicKey = identityPacket.signingPublicKey.copyOf(),
                    signature = signature.copyOf()
                )

            protocolOutbox
                .enqueue(
                    contactId = context.contactId,
                    packet = acknowledgement
                ).getOrThrow()

            if (update.keyExchangeStatus == KeyExchangeStatus.MUTUAL) {
                contactVerificationRepository
                    .sendReceiptIfLocallyVerified(context.contactId)
                    .getOrThrow()
            }
        }
}
