package com.cbgm.sparrow.core.protocol.invitation

import com.cbgm.sparrow.core.crypto.signature.DetachedSignatureCrypto
import com.cbgm.sparrow.core.crypto.util.ByteArrays
import com.cbgm.sparrow.core.protocol.packet.ContactInvitePacket
import com.cbgm.sparrow.core.protocol.profile.ProfilePictureMetadata
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertTrue

class ContactInvitationRequestProtocolTest {
    @Test
    fun verifiesTheOriginalSignedInviteBytesIncludingOptionalDisplayNameAndPicture() = runTest {
        val crypto = RecordingCrypto()
        val protocol = ContactInvitationRequestProtocol(crypto, ContactInvitationPayloadEncoder())
        val packet = invite()
        protocol.verifyPacket(packet, 1000L).getOrThrow()
        assertContentEquals(
            ByteArrays.concatenate(
                ByteArrays.withLengthPrefix("Sparrow.ContactInvite".encodeToByteArray()),
                ByteArrays.withLengthPrefix(packet.packetId.encodeToByteArray()),
                ByteArrays.withLengthPrefix(ByteArrays.encodeInt(packet.version)),
                ByteArrays.withLengthPrefix(packet.invitationId.encodeToByteArray()),
                ByteArrays.withLengthPrefix(ByteArrays.concatenate(byteArrayOf(1), "Alice".encodeToByteArray())),
                ByteArrays.withLengthPrefix(ByteArrays.encodeLong(packet.createdAtEpochMilliseconds)),
                ByteArrays.withLengthPrefix(ByteArrays.encodeLong(packet.expiresAtEpochMilliseconds)),
                ByteArrays.withLengthPrefix(
                    ByteArrays.concatenate(
                        ByteArrays.encodeLong(packet.profilePicture.changedAtEpochMilliseconds),
                        byteArrayOf(0),
                        ByteArrays.withLengthPrefix(byteArrayOf())
                    )
                ),
                ByteArrays.withLengthPrefix(packet.inviteChallenge),
                ByteArrays.withLengthPrefix(packet.encryptionPublicKey),
                ByteArrays.withLengthPrefix(packet.signingPublicKey)
            ),
            crypto.verifiedPayload
        )
    }

    @Test
    fun rejectsInvalidSignedFieldsBeforePeerMutation() = runTest {
        val crypto = RecordingCrypto()
        val protocol = ContactInvitationRequestProtocol(crypto, ContactInvitationPayloadEncoder())
        val packet = invite()
        assertTrue(protocol.verifyPacket(packet.copy(packetId = "wrong-id"), 1000L).isFailure)
        assertTrue(protocol.verifyPacket(packet.copy(expiresAtEpochMilliseconds = 1000L), 1000L).isFailure)
        assertTrue(
            protocol.verifyPacket(
                packet.copy(
                    createdAtEpochMilliseconds = 1000L + 5L * 60L * 1_000L + 1L,
                    expiresAtEpochMilliseconds = 1000L + 5L * 60L * 1_000L + 1000L
                ),
                1000L
            ).isFailure
        )
        crypto.failVerification = true
        assertTrue(protocol.verifyPacket(packet, 1000L).isFailure)
    }

    private fun invite() = ContactInvitePacket(
        packetId = "contact-invite-exchange-1",
        invitationId = "exchange-1",
        displayName = "Alice",
        createdAtEpochMilliseconds = 900L,
        expiresAtEpochMilliseconds = 2000L,
        profilePicture = ProfilePictureMetadata(),
        inviteChallenge = ByteArray(32) { 4 },
        encryptionPublicKey = ByteArray(32) { 5 },
        signingPublicKey = ByteArray(32) { 6 },
        signature = ByteArray(64) { 7 }
    )

    private class RecordingCrypto : DetachedSignatureCrypto {
        var verifiedPayload = byteArrayOf()
        var failVerification = false

        override suspend fun sign(payload: ByteArray, signingPrivateKey: ByteArray): Result<ByteArray> =
            Result.failure(UnsupportedOperationException("Only verification is used"))

        override suspend fun verify(
            payload: ByteArray,
            signingPublicKey: ByteArray,
            signature: ByteArray
        ): Result<Unit> {
            verifiedPayload = payload.copyOf()
            return if (failVerification) {
                Result.failure(IllegalArgumentException("Bad signature"))
            } else {
                Result.success(Unit)
            }
        }
    }
}
