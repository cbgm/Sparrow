package com.cbgm.sparrow.core.protocol.authorization

import com.cbgm.sparrow.core.crypto.signature.DetachedSignatureCrypto
import com.cbgm.sparrow.core.crypto.util.ByteArrays
import com.cbgm.sparrow.core.protocol.identity.LocalSigningKeyPair
import com.cbgm.sparrow.core.protocol.packet.DirectChatAuthorizationRevokedPacket
import com.cbgm.sparrow.core.protocol.version.ProtocolVersion

/** Owns the signed wire representation, not the identity or conversation lifecycle. */
class DirectChatAuthorizationRevocationProtocol(
    private val signatureCrypto: DetachedSignatureCrypto
) {
    suspend fun createPacket(
        invitationId: String,
        inviteChallenge: ByteArray,
        revokedAtEpochMilliseconds: Long,
        signingKeyPair: LocalSigningKeyPair
    ): Result<DirectChatAuthorizationRevokedPacket> =
        runCatching {
            val packetId = "direct-chat-authorization-revoked-$invitationId"
            val version = ProtocolVersion.CURRENT
            val signingPublicKey = signingKeyPair.publicKey
            val signature =
                signatureCrypto.sign(
                    encodeRevoked(
                        packetId = packetId,
                        version = version,
                        invitationId = invitationId,
                        revokedAtEpochMilliseconds = revokedAtEpochMilliseconds,
                        inviteChallenge = inviteChallenge,
                        revokerSigningPublicKey = signingPublicKey
                    ),
                    signingKeyPair.privateKey
                ).getOrThrow()

            DirectChatAuthorizationRevokedPacket(
                packetId = packetId,
                version = version,
                invitationId = invitationId,
                revokedAtEpochMilliseconds = revokedAtEpochMilliseconds,
                inviteChallenge = inviteChallenge.copyOf(),
                revokerSigningPublicKey = signingPublicKey.copyOf(),
                signature = signature.copyOf()
            )
        }

    suspend fun verifyPacket(packet: DirectChatAuthorizationRevokedPacket): Result<Unit> =
        runCatching {
            check(packet.packetId == "direct-chat-authorization-revoked-${packet.invitationId}") {
                "Packet ID does not match the invitation transition"
            }
            signatureCrypto.verify(
                encodeRevoked(
                    packetId = packet.packetId,
                    version = packet.version,
                    invitationId = packet.invitationId,
                    revokedAtEpochMilliseconds = packet.revokedAtEpochMilliseconds,
                    inviteChallenge = packet.inviteChallenge,
                    revokerSigningPublicKey = packet.revokerSigningPublicKey
                ),
                packet.revokerSigningPublicKey,
                packet.signature
            ).getOrThrow()
        }

    private fun encodeRevoked(
        packetId: String,
        version: Int,
        invitationId: String,
        revokedAtEpochMilliseconds: Long,
        inviteChallenge: ByteArray,
        revokerSigningPublicKey: ByteArray
    ): ByteArray =
        ByteArrays.concatenate(
            ByteArrays.withLengthPrefix("Sparrow.DirectChatAuthorizationRevoked".encodeToByteArray()),
            ByteArrays.withLengthPrefix(packetId.encodeToByteArray()),
            ByteArrays.withLengthPrefix(ByteArrays.encodeInt(version)),
            ByteArrays.withLengthPrefix(invitationId.encodeToByteArray()),
            ByteArrays.withLengthPrefix(ByteArrays.encodeLong(revokedAtEpochMilliseconds)),
            ByteArrays.withLengthPrefix(inviteChallenge),
            ByteArrays.withLengthPrefix(revokerSigningPublicKey)
        )
}
