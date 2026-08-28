package com.cbgm.sparrow.feature.contacts.util

import com.cbgm.sparrow.core.crypto.util.ByteArrays
import com.cbgm.sparrow.core.protocol.profile.ProfilePictureMetadata

class IdentityInvitationPayloadEncoder {
    fun encodeInvite(
        packetId: String,
        version: Int,
        invitationId: String,
        displayName: String?,
        createdAtEpochMilliseconds: Long,
        expiresAtEpochMilliseconds: Long,
        profilePicture: ProfilePictureMetadata,
        inviteChallenge: ByteArray,
        encryptionPublicKey: ByteArray,
        signingPublicKey: ByteArray
    ): ByteArray =
        encode(
            domainSeparator = "Sparrow.ContactInvite",
            packetId.encodeToByteArray(),
            ByteArrays.encodeInt(version),
            invitationId.encodeToByteArray(),
            encodeNullableString(displayName),
            ByteArrays.encodeLong(createdAtEpochMilliseconds),
            ByteArrays.encodeLong(expiresAtEpochMilliseconds),
            encodeProfilePicture(profilePicture),
            inviteChallenge,
            encryptionPublicKey,
            signingPublicKey
        )

    fun encodeAccepted(
        packetId: String,
        version: Int,
        invitationId: String,
        acceptedAtEpochMilliseconds: Long,
        profilePicture: ProfilePictureMetadata,
        inviteChallenge: ByteArray,
        responseChallenge: ByteArray,
        inviterEncryptionPublicKey: ByteArray,
        inviterSigningPublicKey: ByteArray,
        responderEncryptionPublicKey: ByteArray,
        responderSigningPublicKey: ByteArray
    ): ByteArray =
        encode(
            domainSeparator = "Sparrow.ContactInviteAccepted",
            packetId.encodeToByteArray(),
            ByteArrays.encodeInt(version),
            invitationId.encodeToByteArray(),
            ByteArrays.encodeLong(acceptedAtEpochMilliseconds),
            encodeProfilePicture(profilePicture),
            inviteChallenge,
            responseChallenge,
            inviterEncryptionPublicKey,
            inviterSigningPublicKey,
            responderEncryptionPublicKey,
            responderSigningPublicKey
        )

    fun encodeReady(
        packetId: String,
        version: Int,
        invitationId: String,
        readyAtEpochMilliseconds: Long,
        responseChallenge: ByteArray,
        acceptedResponderEncryptionPublicKey: ByteArray,
        acceptedResponderSigningPublicKey: ByteArray,
        senderEncryptionPublicKey: ByteArray,
        senderSigningPublicKey: ByteArray
    ): ByteArray =
        encode(
            domainSeparator = "Sparrow.ContactReady",
            packetId.encodeToByteArray(),
            ByteArrays.encodeInt(version),
            invitationId.encodeToByteArray(),
            ByteArrays.encodeLong(readyAtEpochMilliseconds),
            responseChallenge,
            acceptedResponderEncryptionPublicKey,
            acceptedResponderSigningPublicKey,
            senderEncryptionPublicKey,
            senderSigningPublicKey
        )

    fun encodeDeclined(
        packetId: String,
        version: Int,
        invitationId: String,
        declinedAtEpochMilliseconds: Long,
        inviteChallenge: ByteArray,
        declinerSigningPublicKey: ByteArray
    ): ByteArray =
        encode(
            domainSeparator = "Sparrow.ContactInviteDeclined",
            packetId.encodeToByteArray(),
            ByteArrays.encodeInt(version),
            invitationId.encodeToByteArray(),
            ByteArrays.encodeLong(declinedAtEpochMilliseconds),
            inviteChallenge,
            declinerSigningPublicKey
        )

    fun encodeDirectChatAuthorizationRevoked(
        packetId: String,
        version: Int,
        invitationId: String,
        revokedAtEpochMilliseconds: Long,
        inviteChallenge: ByteArray,
        revokerSigningPublicKey: ByteArray
    ): ByteArray =
        encode(
            domainSeparator = "Sparrow.DirectChatAuthorizationRevoked",
            packetId.encodeToByteArray(),
            ByteArrays.encodeInt(version),
            invitationId.encodeToByteArray(),
            ByteArrays.encodeLong(revokedAtEpochMilliseconds),
            inviteChallenge,
            revokerSigningPublicKey
        )

    private fun encode(
        domainSeparator: String,
        vararg fields: ByteArray
    ): ByteArray =
        ByteArrays.concatenate(
            ByteArrays.withLengthPrefix(domainSeparator.encodeToByteArray()),
            *fields.map { field -> ByteArrays.withLengthPrefix(field) }.toTypedArray()
        )

    private fun encodeProfilePicture(metadata: ProfilePictureMetadata): ByteArray =
        ByteArrays.concatenate(
            ByteArrays.encodeLong(metadata.changedAtEpochMilliseconds),
            byteArrayOf(if (metadata.hasPicture) 1 else 0),
            ByteArrays.withLengthPrefix(metadata.payload?.bytes ?: byteArrayOf())
        )

    private fun encodeNullableString(value: String?): ByteArray =
        if (value == null) {
            byteArrayOf(0)
        } else {
            ByteArrays.concatenate(byteArrayOf(1), value.encodeToByteArray())
        }
}
