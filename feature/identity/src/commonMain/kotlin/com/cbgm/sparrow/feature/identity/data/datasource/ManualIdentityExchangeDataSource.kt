package com.cbgm.sparrow.feature.identity.data.datasource

import com.cbgm.sparrow.core.crypto.identity.IdentityAcknowledgementCrypto
import com.cbgm.sparrow.core.id.IdGenerator
import com.cbgm.sparrow.core.protocol.handler.IncomingPacketContext
import com.cbgm.sparrow.core.protocol.identity.LocalPublicIdentityProvider
import com.cbgm.sparrow.core.protocol.identity.LocalSigningKeyPairProvider
import com.cbgm.sparrow.core.protocol.outbox.ProtocolOutbox
import com.cbgm.sparrow.core.protocol.packet.IdentityAcknowledgementPacket
import com.cbgm.sparrow.core.protocol.packet.IdentityPacket
import com.cbgm.sparrow.core.result.safeSuspendCall
import com.cbgm.sparrow.feature.identity.data.model.StoredKeyExchangeStatusDto

internal class ManualIdentityExchangeDataSource(
    private val remoteIdentityDataSource: RemoteIdentityDataSource,
    private val localSigningKeyPairProvider: LocalSigningKeyPairProvider,
    private val localPublicIdentityProvider: LocalPublicIdentityProvider,
    private val identityAcknowledgementCrypto: IdentityAcknowledgementCrypto,
    private val protocolOutbox: ProtocolOutbox
) {
    suspend fun receiveIdentity(
        context: IncomingPacketContext,
        packet: IdentityPacket
    ): Result<Boolean> =
        safeSuspendCall {
            val pinnedIdentity = remoteIdentityDataSource.findByPeerId(context.contactId) ?: return@safeSuspendCall false
            if (!pinnedIdentity.locallyImported) return@safeSuspendCall false

            check(pinnedIdentity.encryptionPublicKey.contentEquals(packet.encryptionPublicKey)) {
                "Manual identity packet does not match the imported encryption key"
            }
            check(pinnedIdentity.signingPublicKey.contentEquals(packet.signingPublicKey)) {
                "Manual identity packet does not match the imported signing key"
            }

            val keyExchangeStatus =
                remoteIdentityDataSource.recordRemotePacket(
                    peerId = context.contactId,
                    encryptionPublicKey = packet.encryptionPublicKey,
                    signingPublicKey = packet.signingPublicKey
                )

            val localSigningKeyPair = localSigningKeyPairProvider.getSigningKeyPair().getOrThrow()
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

            keyExchangeStatus == StoredKeyExchangeStatusDto.MUTUAL
        }

    suspend fun receiveAcknowledgement(
        context: IncomingPacketContext,
        packet: IdentityAcknowledgementPacket
    ): Result<Boolean> =
        safeSuspendCall {
            val remoteIdentity = remoteIdentityDataSource.findByPeerId(context.contactId) ?: return@safeSuspendCall false
            if (!remoteIdentity.locallyImported) return@safeSuspendCall false

            check(packet.senderSigningPublicKey.contentEquals(remoteIdentity.signingPublicKey)) {
                "Acknowledgement sender signing key does not match the imported remote identity"
            }

            val localIdentity = localPublicIdentityProvider.getLocalPublicIdentity().getOrThrow()
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

            remoteIdentityDataSource.markMutual(
                peerId = context.contactId,
                encryptionPublicKey = remoteIdentity.encryptionPublicKey,
                signingPublicKey = remoteIdentity.signingPublicKey
            )
            true
        }
}
