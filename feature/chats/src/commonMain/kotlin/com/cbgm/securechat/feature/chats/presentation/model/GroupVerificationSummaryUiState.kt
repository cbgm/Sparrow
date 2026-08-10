package com.cbgm.securechat.feature.chats.presentation.model

import com.cbgm.securechat.feature.chats.domain.model.GroupVerificationMembershipStatus
import com.cbgm.securechat.feature.chats.domain.model.GroupVerificationPair

enum class GroupMemberVerificationState {
    GROUP_ADMIN,
    MUTUALLY_VERIFIED,
    ADMIN_VERIFIED_PARTICIPANT,
    PARTICIPANT_VERIFIED_ADMIN,
    UNVERIFIED,
    INVITATION_PENDING
}

data class GroupMemberVerificationUiState(
    val invitationId: String?,
    val contactId: String?,
    val displayName: String,
    val isGroupAdmin: Boolean,
    val isActive: Boolean,
    val state: GroupMemberVerificationState,
    val canVerify: Boolean
) {
    val stableKey: String
        get() =
            buildString {
                append(if (isGroupAdmin) "admin" else "participant")
                append(':')
                append(invitationId ?: "without-invitation")
                append(':')
                append(contactId ?: "without-contact")
            }
}

data class GroupVerificationSummaryUiState(
    val hasAuthoritativeState: Boolean = false,
    val isLocalAdmin: Boolean = false,
    val mutuallyVerifiedParticipantCount: Int = 0,
    val activeParticipantCount: Int = 0,
    val totalMemberCount: Int = 0,
    val members: List<GroupMemberVerificationUiState> = emptyList(),
    val canLeaveGroup: Boolean = false
) {
    val isFullyVerified: Boolean
        get() =
            hasAuthoritativeState &&
                activeParticipantCount > 0 &&
                mutuallyVerifiedParticipantCount == activeParticipantCount
}

internal fun buildGroupVerificationSummary(
    isLocalAdmin: Boolean,
    ownerContactId: String?,
    ownerDisplayName: String,
    ownInvitationId: String?,
    rows: List<GroupVerificationPair>,
    isLeavePending: Boolean = false
): GroupVerificationSummaryUiState {
    val participantRows =
        rows
            .distinctBy(GroupVerificationPair::invitationId)
            .sortedBy { row -> row.displayName.lowercase() }

    val participantMembers =
        participantRows.map { row ->
            val isActive =
                row.membershipStatus == GroupVerificationMembershipStatus.ACTIVE

            GroupMemberVerificationUiState(
                invitationId = row.invitationId,
                contactId = if (isLocalAdmin) row.contactId else null,
                displayName = row.displayName,
                isGroupAdmin = false,
                isActive = isActive,
                state = row.toVerificationState(),
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
    val adminMember =
        GroupMemberVerificationUiState(
            invitationId = ownInvitationId,
            contactId = ownerContactId,
            displayName = ownerDisplayName,
            isGroupAdmin = true,
            isActive = isLocalAdmin || ownPair?.isActive() == true,
            state =
                if (isLocalAdmin) {
                    GroupMemberVerificationState.GROUP_ADMIN
                } else {
                    ownPair?.toVerificationState()
                        ?: GroupMemberVerificationState.INVITATION_PENDING
                },
            canVerify =
                !isLocalAdmin &&
                    ownerContactId != null &&
                    ownPair != null &&
                    ownPair.isActive() &&
                    !ownPair.participantVerifiedAdmin
        )

    val activeRows = participantRows.filter { row -> row.isActive() }

    return GroupVerificationSummaryUiState(
        hasAuthoritativeState = participantRows.isNotEmpty(),
        isLocalAdmin = isLocalAdmin,
        mutuallyVerifiedParticipantCount =
            activeRows.count { row ->
                row.adminVerifiedParticipant && row.participantVerifiedAdmin
            },
        activeParticipantCount = activeRows.size,
        totalMemberCount = participantRows.size + 1,
        canLeaveGroup =
            !isLocalAdmin &&
                !isLeavePending &&
                ownPair?.isActive() == true,
        members =
            buildList {
                add(adminMember)
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
