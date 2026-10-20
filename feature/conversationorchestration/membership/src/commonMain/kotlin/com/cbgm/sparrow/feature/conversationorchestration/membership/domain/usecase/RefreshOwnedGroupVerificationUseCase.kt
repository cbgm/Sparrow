package com.cbgm.sparrow.feature.conversationorchestration.membership.domain.usecase

import com.cbgm.sparrow.core.util.result.safeSuspendCall
import com.cbgm.sparrow.core.util.time.SystemClock
import com.cbgm.sparrow.feature.conversationorchestration.membership.domain.model.GroupVerificationRow
import com.cbgm.sparrow.feature.conversationorchestration.membership.domain.model.GroupVerificationStatus
import com.cbgm.sparrow.feature.conversationorchestration.membership.domain.model.OwnedGroupVerificationInputs
import com.cbgm.sparrow.feature.conversationorchestration.membership.domain.repository.GroupVerificationRepository
import com.cbgm.sparrow.feature.membership.domain.model.GroupVerificationMemberKey
import com.cbgm.sparrow.feature.membership.domain.model.GroupVerificationMembership

/**
 * Rebuilds the admin's verification rows from the current epoch: active members keep their verification
 * only while their keys are unchanged, visible pending invitations get pending rows.
 */
class RefreshOwnedGroupVerificationUseCase(
    private val verificationRepository: GroupVerificationRepository,
    private val verificationInputs: GroupVerificationInputsUseCase
) {
    suspend operator fun invoke(groupId: String): Result<Unit> =
        safeSuspendCall(logFailure = false) {
            require(groupId.isNotBlank()) { "Group ID must not be blank" }
            val existingRows = verificationRepository.findRows(groupId).getOrThrow()
            val existingByContactId = existingRows.mapNotNull { row -> row.contactId?.let { it to row } }.toMap()
            val inputs = verificationInputs.ownedState(groupId).getOrThrow()
            val now = SystemClock.nowEpochMilliseconds()

            val activeRows = activeRows(groupId, inputs, existingByContactId, now)
            val pendingRows = pendingRows(groupId, inputs, existingByContactId, activeRows)
            val authoritativeInvitationIds = (activeRows + pendingRows).mapTo(mutableSetOf()) { it.invitationId }
            val remotePendingRows =
                existingRows.filter { row ->
                    row.contactId == null &&
                        row.status == GroupVerificationStatus.PENDING &&
                        row.invitationId !in authoritativeInvitationIds
                }

            verificationRepository.replaceGroup(groupId, activeRows + pendingRows + remotePendingRows).getOrThrow()
        }

    private fun activeRows(
        groupId: String,
        inputs: OwnedGroupVerificationInputs,
        existingByContactId: Map<String, GroupVerificationRow>,
        now: Long
    ): List<GroupVerificationRow> {
        val membershipByContactId = inputs.membership.memberships.associateBy(GroupVerificationMembership::contactId)
        return inputs.membership.memberKeys.map { memberKey ->
            val membership = membershipByContactId[memberKey.contactId]
            val previous = existingByContactId[memberKey.contactId]
            val sameIdentity = previous.matches(memberKey)
            GroupVerificationRow(
                groupId = groupId,
                invitationId =
                    membership?.sourceInvitationId ?: previous?.invitationId ?: "member-${memberKey.contactId}",
                contactId = memberKey.contactId,
                displayName = inputs.requireDisplayName(memberKey.contactId),
                status = GroupVerificationStatus.ACTIVE,
                participantEncryptionPublicKey = memberKey.encryptionPublicKey.copyOf(),
                participantSigningPublicKey = memberKey.signingPublicKey.copyOf(),
                adminVerifiedParticipant = sameIdentity && previous?.adminVerifiedParticipant == true,
                participantVerifiedAdmin = sameIdentity && previous?.participantVerifiedAdmin == true,
                updatedAtEpochMilliseconds = maxOf(membership?.updatedAtEpochMilliseconds ?: 0L, now)
            )
        }
    }

    private fun pendingRows(
        groupId: String,
        inputs: OwnedGroupVerificationInputs,
        existingByContactId: Map<String, GroupVerificationRow>,
        activeRows: List<GroupVerificationRow>
    ): List<GroupVerificationRow> {
        val activeContactIds = activeRows.mapNotNullTo(mutableSetOf()) { it.contactId }
        return inputs.membership.memberships
            .filter { membership ->
                membership.isOwner && membership.isVisiblePending && membership.contactId !in activeContactIds
            }
            .map { membership ->
                val identity = inputs.pendingIdentities[membership.contactId]
                val previous = existingByContactId[membership.contactId]
                GroupVerificationRow(
                    groupId = groupId,
                    invitationId = membership.sourceInvitationId,
                    contactId = membership.contactId,
                    displayName = inputs.requireDisplayName(membership.contactId),
                    status = GroupVerificationStatus.PENDING,
                    participantEncryptionPublicKey = identity?.encryptionPublicKey?.copyOf(),
                    participantSigningPublicKey = identity?.signingPublicKey?.copyOf(),
                    adminVerifiedParticipant = false,
                    participantVerifiedAdmin = false,
                    updatedAtEpochMilliseconds =
                        maxOf(membership.updatedAtEpochMilliseconds, previous?.updatedAtEpochMilliseconds ?: 0L)
                )
            }
    }

    private fun GroupVerificationRow?.matches(memberKey: GroupVerificationMemberKey): Boolean =
        this?.hasIdentity(memberKey.encryptionPublicKey, memberKey.signingPublicKey) == true
}
