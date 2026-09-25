package com.cbgm.sparrow.core.protocol.invitation

import com.cbgm.sparrow.core.crypto.signature.DetachedSignatureCrypto
import com.cbgm.sparrow.core.protocol.identity.LocalSigningKeyPair
import com.cbgm.sparrow.core.protocol.profile.ProfilePictureMetadata
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** Locks down the existing packet IDs and signed bytes during extraction from Identity. */
class ContactInvitationPacketCreationTest {
    private val encoder = ContactInvitationPayloadEncoder()
    private val keys = LocalSigningKeyPair(ByteArray(32) { 2 }, ByteArray(64) { 3 })
    private val challenge = ByteArray(32) { 4 }
    private val response = ByteArray(32) { 5 }
    private val encryption = ByteArray(32) { 6 }
    private val inviterEncryption = ByteArray(32) { 7 }
    private val inviterSigning = ByteArray(32) { 8 }
    private val picture = ProfilePictureMetadata()

    @Test
    fun invitationUsesOriginalSignedFieldsAndPacketId() = runTest {
        val crypto = RecordingCrypto()
        val protocol = ContactInvitationRequestProtocol(crypto, encoder)
        val packet = protocol.createPacket(
            invitationId = "id-1",
            displayName = "+4912345",
            createdAtEpochMilliseconds = 100L,
            expiresAtEpochMilliseconds = 200L,
            profilePicture = picture,
            inviteChallenge = challenge,
            encryptionPublicKey = encryption,
            signingKeyPair = keys
        ).getOrThrow()
        assertEquals("contact-invite-id-1", packet.packetId)
        assertContentEquals(
            encoder.encodeInvite(
                packet.packetId,
                packet.version,
                packet.invitationId,
                packet.displayName,
                packet.createdAtEpochMilliseconds,
                packet.expiresAtEpochMilliseconds,
                packet.profilePicture,
                packet.inviteChallenge,
                packet.encryptionPublicKey,
                packet.signingPublicKey
            ),
            crypto.signedPayload
        )
        assertContentEquals(keys.publicKey, packet.signingPublicKey)
        assertContentEquals(crypto.signature, packet.signature)
        protocol.verifyPacket(packet, 100L).getOrThrow()
    }

    @Test
    fun acceptanceAndReadyPreserveSignedBytesAndOriginalPacketIds() = runTest {
        val crypto = RecordingCrypto()
        val protocol = ContactInvitationHandshakeProtocol(crypto, encoder)
        val accepted = protocol.createAccepted(
            invitationId = "id-2",
            acceptedAtEpochMilliseconds = 110L,
            profilePicture = picture,
            inviteChallenge = challenge,
            responseChallenge = response,
            inviterEncryptionPublicKey = inviterEncryption,
            inviterSigningPublicKey = inviterSigning,
            responderEncryptionPublicKey = encryption,
            signingKeyPair = keys
        ).getOrThrow()
        assertEquals("contact-invite-accepted-id-2", accepted.packetId)
        assertContentEquals(
            encoder.encodeAccepted(
                accepted.packetId,
                accepted.version,
                accepted.invitationId,
                accepted.acceptedAtEpochMilliseconds,
                accepted.profilePicture,
                accepted.inviteChallenge,
                accepted.responseChallenge,
                accepted.inviterEncryptionPublicKey,
                accepted.inviterSigningPublicKey,
                accepted.responderEncryptionPublicKey,
                accepted.responderSigningPublicKey
            ),
            crypto.signedPayload
        )
        assertContentEquals(keys.publicKey, accepted.responderSigningPublicKey)
        assertContentEquals(crypto.signature, accepted.signature)
        protocol.verifyAccepted(accepted, receivedAtEpochMilliseconds = 110L).getOrThrow()
        assertContentEquals(accepted.responderSigningPublicKey, crypto.verifiedKey)

        val ready = protocol.createReady(
            invitationId = "id-2",
            readyAtEpochMilliseconds = 120L,
            responseChallenge = response,
            acceptedResponderEncryptionPublicKey = encryption,
            acceptedResponderSigningPublicKey = keys.publicKey,
            senderEncryptionPublicKey = inviterEncryption,
            signingKeyPair = keys
        ).getOrThrow()
        assertEquals("contact-ready-id-2", ready.packetId)
        assertContentEquals(
            encoder.encodeReady(
                ready.packetId,
                ready.version,
                ready.invitationId,
                ready.readyAtEpochMilliseconds,
                ready.responseChallenge,
                ready.acceptedResponderEncryptionPublicKey,
                ready.acceptedResponderSigningPublicKey,
                ready.senderEncryptionPublicKey,
                ready.senderSigningPublicKey
            ),
            crypto.signedPayload
        )
        assertContentEquals(keys.publicKey, ready.senderSigningPublicKey)
        assertContentEquals(crypto.signature, ready.signature)
        protocol.verifyReady(
            ready,
            remoteSigningPublicKey = keys.publicKey,
            receivedAtEpochMilliseconds = 120L
        ).getOrThrow()
        assertContentEquals(keys.publicKey, crypto.verifiedKey)
    }

    @Test
    fun acceptanceAndReadyRejectWrongPacketIdsAndFutureTimestamps() = runTest {
        val protocol = ContactInvitationHandshakeProtocol(RecordingCrypto(), encoder)
        val accepted = protocol.createAccepted(
            "id-3",
            600_001L,
            picture,
            challenge,
            response,
            inviterEncryption,
            inviterSigning,
            encryption,
            keys
        ).getOrThrow()
        assertTrue(protocol.verifyAccepted(accepted.copy(packetId = "invalid"), 0L).isFailure)
        assertTrue(protocol.verifyAccepted(accepted, 0L).isFailure)
        val ready = protocol.createReady(
            "id-3",
            600_001L,
            response,
            encryption,
            keys.publicKey,
            inviterEncryption,
            keys
        ).getOrThrow()
        assertTrue(protocol.verifyReady(ready.copy(packetId = "invalid"), keys.publicKey, 0L).isFailure)
        assertTrue(protocol.verifyReady(ready, keys.publicKey, 0L).isFailure)
    }

    private class RecordingCrypto : DetachedSignatureCrypto {
        var signedPayload = byteArrayOf()
        val signature = ByteArray(64) { 9 }
        var verifiedKey = byteArrayOf()

        override suspend fun sign(payload: ByteArray, signingPrivateKey: ByteArray): Result<ByteArray> {
            signedPayload = payload.copyOf()
            return Result.success(signature.copyOf())
        }

        override suspend fun verify(
            payload: ByteArray,
            signingPublicKey: ByteArray,
            signature: ByteArray
        ): Result<Unit> {
            verifiedKey = signingPublicKey.copyOf()
            return Result.success(Unit)
        }
    }
}
