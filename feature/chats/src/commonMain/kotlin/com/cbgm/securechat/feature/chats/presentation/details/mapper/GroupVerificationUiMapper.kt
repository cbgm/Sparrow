package com.cbgm.securechat.feature.chats.presentation.details.mapper

import com.cbgm.securechat.feature.chats.domain.model.group.GroupVerificationMembershipStatus
import com.cbgm.securechat.feature.chats.domain.model.group.GroupVerificationPair
import com.cbgm.securechat.feature.chats.presentation.details.model.GroupMemberVerificationState
import com.cbgm.securechat.feature.chats.presentation.details.model.GroupMemberVerificationUiState
import com.cbgm.securechat.feature.chats.presentation.details.model.GroupVerificationSummaryUiState

internal fun buildGroupVerificationSummary(
    isLocalAdmin: Boolean,
    isLocalMemberActive: Boolean = true,
    ownerContactId: String?,
    ownerDisplayName: String,
    ownInvitationId: String?,
    rows: List<GroupVerificationPair>,
    isLeavePending: Boolean = false,
    remoteAdminContactIds: Set<String> = emptySet(),
    currentMemberContactIds: Set<String> = emptySet(),
    promotableContactIds: Set<String> = emptySet(),
    isOrphaned: Boolean = false,
    requiresAdminPromotionBeforeLeave: Boolean = false
): GroupVerificationSummaryUiState {
    val participantRows =
        rows
            .distinctBy(GroupVerificationPair::invitationId)
            .filter { row ->
                row.membershipStatus != GroupVerificationMembershipStatus.ACTIVE ||
                    row.contactId == null ||
                    row.contactId in currentMemberContactIds
            }
            .sortedBy { row -> row.displayName.lowercase() }

    val participantMembers =
        participantRows.map { row ->
            val isActive =
                row.membershipStatus == GroupVerificationMembershipStatus.ACTIVE &&
                    (row.contactId == null || row.contactId in currentMemberContactIds)

            GroupMemberVerificationUiState(
                invitationId = row.invitationId,
                contactId = if (isLocalAdmin) row.contactId else null,
                displayName = row.displayName,
                isGroupAdmin = row.contactId in remoteAdminContactIds,
                isActive = isActive,
                state =
                    if (!isActive && row.membershipStatus == GroupVerificationMembershipStatus.ACTIVE) {
                        GroupMemberVerificationState.UNAVAILABLE
                    } else {
                        row.toVerificationState()
                    },
                canVerify =
                    isLocalAdmin &&
                        isActive &&
                        row.contactId != null &&
                        !row.adminVerifiedParticipant
            )
        }

    val ownPair =
        participantRows.firstOrNull { row ->
            row.invitationId == ownInvitationId
        }
    val isReferenceAdminCurrent =
        ownerContactId != null &&
            ownerContactId in remoteAdminContactIds &&
            ownerContactId in currentMemberContactIds
    val adminMember =
        if (isLocalAdmin || isReferenceAdminCurrent) {
            GroupMemberVerificationUiState(
                invitationId = ownInvitationId,
                contactId = ownerContactId,
                displayName = ownerDisplayName,
                isGroupAdmin = true,
                isActive = true,
                state =
                    if (isLocalAdmin) {
                        GroupMemberVerificationState.GROUP_ADMIN
                    } else {
                        ownPair?.toVerificationState()
                            ?: GroupMemberVerificationState.UNVERIFIED
                    },
                canVerify =
                    !isLocalAdmin &&
                        ownPair != null &&
                        ownPair.isActive() &&
                        !ownPair.participantVerifiedAdmin
            )
        } else {
            null
        }

    val activeRows =
        participantRows.filter { row ->
            row.isActive() &&
                (row.contactId == null || row.contactId in currentMemberContactIds)
        }

    return GroupVerificationSummaryUiState(
        hasAuthoritativeState = isLocalAdmin || participantRows.isNotEmpty(),
        isLocalAdmin = isLocalAdmin,
        mutuallyVerifiedParticipantCount =
            activeRows.count { row ->
                row.adminVerifiedParticipant && row.participantVerifiedAdmin
            },
        activeParticipantCount = activeRows.size,
        totalMemberCount = participantRows.size + if (adminMember != null) 1 else 0,
        canLeaveGroup = !isLeavePending && isLocalMemberActive,
        isOrphaned = isOrphaned,
        adminCount = remoteAdminContactIds.size + if (isLocalAdmin) 1 else 0,
        currentMemberContactIds = currentMemberContactIds,
        requiresAdminPromotionBeforeLeave = requiresAdminPromotionBeforeLeave,
        promotableContactIds = promotableContactIds,
        members =
            buildList {
                adminMember?.let(::add)
                addAll(participantMembers)
            }
    )
}

private fun GroupVerificationPair.isActive(): Boolean =
    membershipStatus == GroupVerificationMembershipStatus.ACTIVE

private fun GroupVerificationPair.toVerificationState(): GroupMemberVerificationState =
    when {
        !isActive() -> GroupMemberVerificationState.INVITATION_PENDING
        adminVerifiedParticipant && participantVerifiedAdmin ->
            GroupMemberVerificationState.MUTUALLY_VERIFIED
        adminVerifiedParticipant ->
            GroupMemberVerificationState.ADMIN_VERIFIED_PARTICIPANT
        participantVerifiedAdmin ->
            GroupMemberVerificationState.PARTICIPANT_VERIFIED_ADMIN
        else -> GroupMemberVerificationState.UNVERIFIED
    }
