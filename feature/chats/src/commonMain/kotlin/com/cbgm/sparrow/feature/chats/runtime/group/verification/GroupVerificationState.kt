package com.cbgm.sparrow.feature.chats.runtime.group.verification

import com.cbgm.sparrow.core.time.SystemClock
import com.cbgm.sparrow.data.database.entity.GroupVerificationPairEntity
import com.cbgm.sparrow.feature.chats.data.group.verification.GroupVerificationDataSource
import com.cbgm.sparrow.feature.conversationorchestration.domain.usecase.GroupVerificationInputsUseCase
import com.cbgm.sparrow.feature.membership.domain.model.GroupVerificationMemberKey

internal class GroupVerificationState(
    private val verificationDataSource: GroupVerificationDataSource,
    private val verificationInputs: GroupVerificationInputsUseCase
) {
    suspend fun ownsGroup(groupId: String): Boolean =
        verificationDataSource.findByGroupId(groupId).any { it.contactId != null } ||
            verificationInputs.membershipContext(groupId).ownsGroup

    suspend fun requireCurrentParticipant(groupId: String, contactId: String): GroupVerificationMemberKey =
        verificationInputs.membershipContext(groupId)
            .let { context ->
                check(context.security != null) { "Group security state was not found" }
                context.requireCurrentParticipant(contactId)
            }

    suspend fun requireCurrentRemoteAdmin(groupId: String, contactId: String): GroupVerificationMemberKey =
        verificationInputs.membershipContext(groupId)
            .let { context ->
                check(context.security != null) { "Group security state was not found" }
                context.requireCurrentRemoteAdmin(contactId)
            }

    suspend fun refreshOwnedState(groupId: String) {
        val existingRows = verificationDataSource.findByGroupId(groupId)
        val existingByContactId =
            existingRows.mapNotNull { row -> row.contactId?.let { it to row } }.toMap()
        val ownedState = verificationInputs.ownedState(groupId)
        val context = ownedState.membership
        val memberships = context.memberships
        val membershipByContactId = memberships.associateBy { it.contactId }
        val currentMemberKeys = context.memberKeys
        val now = SystemClock.nowEpochMilliseconds()

        val activeRows =
            currentMemberKeys.map { memberKey ->
                val displayName = ownedState.requireDisplayName(memberKey.contactId)
                val membership = membershipByContactId[memberKey.contactId]
                val previous = existingByContactId[memberKey.contactId]
                val sameIdentity = previous.matches(memberKey)
                GroupVerificationPairEntity(
                    groupId = groupId,
                    invitationId =
                        membership?.sourceInvitationId
                            ?: previous?.invitationId
                            ?: "member-${memberKey.contactId}",
                    contactId = memberKey.contactId,
                    displayName = displayName,
                    membershipStatus = GroupVerificationPairEntity.ACTIVE_STATUS,
                    participantEncryptionPublicKey = memberKey.encryptionPublicKey.copyOf(),
                    participantSigningPublicKey = memberKey.signingPublicKey.copyOf(),
                    adminVerifiedParticipant = sameIdentity && previous?.adminVerifiedParticipant == true,
                    participantVerifiedAdmin = sameIdentity && previous?.participantVerifiedAdmin == true,
                    updatedAtEpochMilliseconds =
                        maxOf(membership?.updatedAtEpochMilliseconds ?: 0L, now)
                )
            }

        val activeContactIds =
            currentMemberKeys.mapTo(mutableSetOf()) { memberKey -> memberKey.contactId }
        val pendingRows =
            memberships
                .filter { membership ->
                    membership.isOwner && membership.isVisiblePending &&
                        membership.contactId !in activeContactIds
                }.map { membership ->
                    val displayName = ownedState.requireDisplayName(membership.contactId)
                    val identity = ownedState.pendingIdentities[membership.contactId]
                    val previous = existingByContactId[membership.contactId]
                    GroupVerificationPairEntity(
                        groupId = groupId,
                        invitationId = membership.sourceInvitationId,
                        contactId = membership.contactId,
                        displayName = displayName,
                        membershipStatus = GroupVerificationPairEntity.PENDING_STATUS,
                        participantEncryptionPublicKey = identity?.encryptionPublicKey?.copyOf(),
                        participantSigningPublicKey = identity?.signingPublicKey?.copyOf(),
                        adminVerifiedParticipant = false,
                        participantVerifiedAdmin = false,
                        updatedAtEpochMilliseconds =
                            maxOf(
                                membership.updatedAtEpochMilliseconds,
                                previous?.updatedAtEpochMilliseconds ?: 0L
                            )
                    )
                }

        val authoritativeInvitationIds =
            (activeRows + pendingRows).mapTo(mutableSetOf()) { row -> row.invitationId }
        val remotePendingRows =
            existingRows.filter { row ->
                row.contactId == null &&
                    row.membershipStatus == GroupVerificationPairEntity.PENDING_STATUS &&
                    row.invitationId !in authoritativeInvitationIds
            }

        verificationDataSource.replaceGroup(
            groupId = groupId,
            rows = activeRows + pendingRows + remotePendingRows
        )
    }

    private fun GroupVerificationPairEntity?.matches(
        memberKey: GroupVerificationMemberKey
    ): Boolean {
        val previous = this ?: return false
        val encryptionPublicKey = previous.participantEncryptionPublicKey ?: return false
        val signingPublicKey = previous.participantSigningPublicKey ?: return false
        return encryptionPublicKey.contentEquals(memberKey.encryptionPublicKey) &&
            signingPublicKey.contentEquals(memberKey.signingPublicKey)
    }
}
