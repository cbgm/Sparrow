package com.cbgm.sparrow.feature.conversationorchestration.membership.domain.model

/**
 * One member's mutual verification with the group admin. [contactId] is null for rows that only
 * the admin's snapshot knows about.
 */
class GroupVerificationRow(
    val groupId: String,
    val invitationId: String,
    val contactId: String?,
    val displayName: String,
    val status: GroupVerificationStatus,
    val participantEncryptionPublicKey: ByteArray?,
    val participantSigningPublicKey: ByteArray?,
    val adminVerifiedParticipant: Boolean,
    val participantVerifiedAdmin: Boolean,
    val updatedAtEpochMilliseconds: Long
) {
    fun hasIdentity(encryptionPublicKey: ByteArray, signingPublicKey: ByteArray): Boolean =
        participantEncryptionPublicKey?.contentEquals(encryptionPublicKey) == true &&
            participantSigningPublicKey?.contentEquals(signingPublicKey) == true
}

enum class GroupVerificationStatus {
    ACTIVE,
    PENDING
}
