package com.cbgm.sparrow.feature.conversationorchestration.membership.data.mapper

import com.cbgm.sparrow.data.database.entity.GroupVerificationPairEntity
import com.cbgm.sparrow.feature.conversationorchestration.membership.domain.model.GroupVerificationRow
import com.cbgm.sparrow.feature.conversationorchestration.membership.domain.model.GroupVerificationStatus
import com.cbgm.sparrow.protocol.packet.GroupVerificationMemberPayload

internal fun GroupVerificationPairEntity.toGroupVerificationRow(): GroupVerificationRow =
    GroupVerificationRow(
        groupId = groupId,
        invitationId = invitationId,
        contactId = contactId,
        displayName = displayName,
        status = membershipStatus.toGroupVerificationStatus(),
        participantEncryptionPublicKey = participantEncryptionPublicKey?.copyOf(),
        participantSigningPublicKey = participantSigningPublicKey?.copyOf(),
        adminVerifiedParticipant = adminVerifiedParticipant,
        participantVerifiedAdmin = participantVerifiedAdmin,
        updatedAtEpochMilliseconds = updatedAtEpochMilliseconds
    )

internal fun GroupVerificationRow.toGroupVerificationPairEntity(): GroupVerificationPairEntity =
    GroupVerificationPairEntity(
        groupId = groupId,
        invitationId = invitationId,
        contactId = contactId,
        displayName = displayName,
        membershipStatus = status.toMembershipStatus(),
        participantEncryptionPublicKey = participantEncryptionPublicKey?.copyOf(),
        participantSigningPublicKey = participantSigningPublicKey?.copyOf(),
        adminVerifiedParticipant = adminVerifiedParticipant,
        participantVerifiedAdmin = participantVerifiedAdmin,
        updatedAtEpochMilliseconds = updatedAtEpochMilliseconds
    )

internal fun String.toGroupVerificationStatus(): GroupVerificationStatus =
    when (this) {
        GroupVerificationPairEntity.ACTIVE_STATUS -> GroupVerificationStatus.ACTIVE
        GroupVerificationPairEntity.PENDING_STATUS -> GroupVerificationStatus.PENDING
        else -> error("Unsupported group verification membership status: $this")
    }

internal fun GroupVerificationStatus.toMembershipStatus(): String =
    when (this) {
        GroupVerificationStatus.ACTIVE -> GroupVerificationPairEntity.ACTIVE_STATUS
        GroupVerificationStatus.PENDING -> GroupVerificationPairEntity.PENDING_STATUS
    }

internal fun GroupVerificationRow.toGroupVerificationMemberPayload(): GroupVerificationMemberPayload =
    GroupVerificationMemberPayload(
        invitationId = invitationId,
        displayName = displayName,
        membershipStatus = status.toMembershipStatus(),
        adminVerifiedParticipant = adminVerifiedParticipant,
        participantVerifiedAdmin = participantVerifiedAdmin
    )

/** Rows of an admin snapshot never carry a contact or keys; this device only learns their status. */
internal fun GroupVerificationMemberPayload.toGroupVerificationRow(
    groupId: String,
    generatedAtEpochMilliseconds: Long
): GroupVerificationRow =
    GroupVerificationRow(
        groupId = groupId,
        invitationId = invitationId,
        contactId = null,
        displayName = displayName,
        status = membershipStatus.toGroupVerificationStatus(),
        participantEncryptionPublicKey = null,
        participantSigningPublicKey = null,
        adminVerifiedParticipant = adminVerifiedParticipant,
        participantVerifiedAdmin = participantVerifiedAdmin,
        updatedAtEpochMilliseconds = generatedAtEpochMilliseconds
    )
