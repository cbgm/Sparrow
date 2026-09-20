package com.cbgm.sparrow.core.protocol.invitation

import com.cbgm.sparrow.core.crypto.signature.DetachedSignatureCrypto
import com.cbgm.sparrow.core.crypto.util.ByteArrays
import com.cbgm.sparrow.core.protocol.identity.LocalSigningKeyPair
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ContactInvitationDeclineProtocolTest {
    private val signingKeys = LocalSigningKeyPair(ByteArray(32) { 3 }, ByteArray(64) { 5 })
    private val challenge = ByteArray(32) { 7 }

    @Test
    fun preservesExistingSignedPayloadAndPacketId() = runTest {
        val crypto = RecordingCrypto()
        val protocol = ContactInvitationDeclineProtocol(crypto, ContactInvitationPayloadEncoder())
        val packet = protocol.createPacket("exchange-1", challenge, 1234L, signingKeys).getOrThrow()

        assertEquals("contact-invite-declined-exchange-1", packet.packetId)
        assertContentEquals(
            ByteArrays.concatenate(
                ByteArrays.withLengthPrefix("Sparrow.ContactInviteDeclined".encodeToByteArray()),
                ByteArrays.withLengthPrefix(packet.packetId.encodeToByteArray()),
                ByteArrays.withLengthPrefix(ByteArrays.encodeInt(packet.version)),
                ByteArrays.withLengthPrefix(packet.invitationId.encodeToByteArray()),
                ByteArrays.withLengthPrefix(ByteArrays.encodeLong(packet.declinedAtEpochMilliseconds)),
                ByteArrays.withLengthPrefix(challenge),
                ByteArrays.withLengthPrefix(signingKeys.publicKey)
            ),
            crypto.signedPayload
        )
        protocol.verifyPacket(packet, receivedAtEpochMilliseconds = 1234L).getOrThrow()
        assertContentEquals(crypto.signedPayload, crypto.verifiedPayload)
        assertContentEquals(signingKeys.publicKey, crypto.verifiedKey)
    }

    @Test
    fun rejectsWrongPacketIdAndFutureTimestamp() = runTest {
        val protocol = ContactInvitationDeclineProtocol(RecordingCrypto(), ContactInvitationPayloadEncoder())
        val packet = protocol.createPacket("exchange-1", challenge, 600_001L, signingKeys).getOrThrow()
        assertTrue(protocol.verifyPacket(packet.copy(packetId = "wrong-id"), 0L).isFailure)
        assertTrue(protocol.verifyPacket(packet, 0L).isFailure)
    }

    @Test
    fun rejectsBadSignatureBeforeIdentityStateIsTouched() = runTest {
        val crypto = RecordingCrypto()
        val protocol = ContactInvitationDeclineProtocol(crypto, ContactInvitationPayloadEncoder())
        val packet = protocol.createPacket("exchange-1", challenge, 1000L, signingKeys).getOrThrow()
        crypto.failVerification = true
        assertTrue(protocol.verifyPacket(packet, 1000L).isFailure)
    }

    private class RecordingCrypto : DetachedSignatureCrypto {
        var signedPayload = byteArrayOf()
        var verifiedPayload = byteArrayOf()
        var verifiedKey = byteArrayOf()
        var failVerification = false

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
            return if (failVerification) {
                Result.failure(IllegalArgumentException("Bad signature"))
            } else {
                Result.success(Unit)
            }
        }
    }
}
