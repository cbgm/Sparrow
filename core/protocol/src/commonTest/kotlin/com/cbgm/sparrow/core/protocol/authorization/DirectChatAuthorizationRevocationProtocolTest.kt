package com.cbgm.sparrow.core.protocol.authorization

import com.cbgm.sparrow.core.crypto.signature.DetachedSignatureCrypto
import com.cbgm.sparrow.core.crypto.util.ByteArrays
import com.cbgm.sparrow.core.protocol.identity.LocalSigningKeyPair
import com.cbgm.sparrow.core.protocol.packet.DirectChatAuthorizationRevokedPacket
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class DirectChatAuthorizationRevocationProtocolTest {
    private val keyPair = LocalSigningKeyPair(
        publicKey = ByteArray(32) { 3 },
        privateKey = ByteArray(64) { 5 }
    )
    private val challenge = ByteArray(32) { 7 }

    @Test
    fun createAndVerifyPreserveTheExistingSignedWirePayload() = kotlinx.coroutines.test.runTest {
        val crypto = RecordingCrypto()
        val protocol = DirectChatAuthorizationRevocationProtocol(crypto)
        val packet = protocol.createPacket("exchange-1", challenge, 123456L, keyPair).getOrThrow()

        assertEquals("direct-chat-authorization-revoked-exchange-1", packet.packetId)
        assertContentEquals(
            ByteArrays.concatenate(
                ByteArrays.withLengthPrefix("Sparrow.DirectChatAuthorizationRevoked".encodeToByteArray()),
                ByteArrays.withLengthPrefix(packet.packetId.encodeToByteArray()),
                ByteArrays.withLengthPrefix(ByteArrays.encodeInt(packet.version)),
                ByteArrays.withLengthPrefix(packet.invitationId.encodeToByteArray()),
                ByteArrays.withLengthPrefix(ByteArrays.encodeLong(123456L)),
                ByteArrays.withLengthPrefix(challenge),
                ByteArrays.withLengthPrefix(keyPair.publicKey)
            ),
            crypto.signedPayload
        )
        protocol.verifyPacket(packet).getOrThrow()
        assertContentEquals(crypto.signedPayload, crypto.verifiedPayload)
        assertContentEquals(keyPair.publicKey, crypto.verifiedKey)
    }

    @Test
    fun rejectsUnexpectedRevocationPacketId() = kotlinx.coroutines.test.runTest {
        val protocol = DirectChatAuthorizationRevocationProtocol(RecordingCrypto())
        val packet = DirectChatAuthorizationRevokedPacket(
            packetId = "wrong-id",
            invitationId = "exchange-1",
            revokedAtEpochMilliseconds = 123456L,
            inviteChallenge = challenge,
            revokerSigningPublicKey = keyPair.publicKey,
            signature = ByteArray(64)
        )
        assertTrue(protocol.verifyPacket(packet).isFailure)
    }

    private class RecordingCrypto : DetachedSignatureCrypto {
        var signedPayload: ByteArray = byteArrayOf()
        var verifiedPayload: ByteArray = byteArrayOf()
        var verifiedKey: ByteArray = byteArrayOf()

        override suspend fun sign(payload: ByteArray, signingPrivateKey: ByteArray): Result<ByteArray> {
            signedPayload = payload.copyOf()
            return Result.success(ByteArray(64) { 9 })
        }

        override suspend fun verify(
            payload: ByteArray,
            signingPublicKey: ByteArray,
            signature: ByteArray
        ): Result<Unit> {
            verifiedPayload = payload.copyOf()
            verifiedKey = signingPublicKey.copyOf()
            return Result.success(Unit)
        }
    }
}
