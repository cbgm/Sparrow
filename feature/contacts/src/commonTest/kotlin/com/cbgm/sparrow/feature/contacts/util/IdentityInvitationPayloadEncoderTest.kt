package com.cbgm.sparrow.feature.contacts.util

import com.cbgm.sparrow.core.protocol.profile.ProfilePictureMetadata
import com.cbgm.sparrow.core.protocol.profile.ProfilePicturePayload
import kotlin.test.Test
import kotlin.test.assertFalse

class IdentityInvitationPayloadEncoderTest {
    private val encoder = IdentityInvitationPayloadEncoder()

    @Test
    fun inviteEncodingBindsProfilePicture() {
        val withoutPicture = encodeInvite(ProfilePictureMetadata())
        val withPicture =
            encodeInvite(
                ProfilePictureMetadata(
                    changedAtEpochMilliseconds = 10L,
                    hasPicture = true,
                    payload = ProfilePicturePayload(byteArrayOf(1, 2, 3))
                )
            )

        assertFalse(withoutPicture.contentEquals(withPicture))
    }

    @Test
    fun acceptanceEncodingBindsProfilePicture() {
        val withoutPicture = encodeAccepted(ProfilePictureMetadata())
        val removedPicture =
            encodeAccepted(
                ProfilePictureMetadata(
                    changedAtEpochMilliseconds = 20L,
                    hasPicture = false
                )
            )

        assertFalse(withoutPicture.contentEquals(removedPicture))
    }

    private fun encodeInvite(profilePicture: ProfilePictureMetadata): ByteArray =
        encoder.encodeInvite(
            packetId = "packet-1",
            version = 1,
            invitationId = "invitation-1",
            displayName = "Alice",
            createdAtEpochMilliseconds = 1L,
            expiresAtEpochMilliseconds = 2L,
            profilePicture = profilePicture,
            inviteChallenge = byteArrayOf(1),
            encryptionPublicKey = byteArrayOf(2),
            signingPublicKey = byteArrayOf(3)
        )

    private fun encodeAccepted(profilePicture: ProfilePictureMetadata): ByteArray =
        encoder.encodeAccepted(
            packetId = "packet-2",
            version = 1,
            invitationId = "invitation-1",
            acceptedAtEpochMilliseconds = 2L,
            profilePicture = profilePicture,
            inviteChallenge = byteArrayOf(1),
            responseChallenge = byteArrayOf(2),
            inviterEncryptionPublicKey = byteArrayOf(3),
            inviterSigningPublicKey = byteArrayOf(4),
            responderEncryptionPublicKey = byteArrayOf(5),
            responderSigningPublicKey = byteArrayOf(6)
        )
}
