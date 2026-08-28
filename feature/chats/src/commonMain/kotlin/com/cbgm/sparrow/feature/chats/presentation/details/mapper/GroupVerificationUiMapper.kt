package com.cbgm.sparrow.feature.chats.presentation.details.mapper

import com.cbgm.sparrow.feature.chats.domain.model.group.GroupVerificationMembershipStatus
import com.cbgm.sparrow.feature.chats.domain.model.group.GroupVerificationPair
import com.cbgm.sparrow.feature.chats.presentation.details.model.GroupAvatarUiState
import com.cbgm.sparrow.feature.chats.presentation.details.model.GroupLeaveUiState
import com.cbgm.sparrow.feature.chats.presentation.details.model.GroupMemberManagementUiState
import com.cbgm.sparrow.feature.chats.presentation.details.model.GroupMemberVerificationState
import com.cbgm.sparrow.feature.chats.presentation.details.model.GroupMemberVerificationUiState
import com.cbgm.sparrow.feature.chats.presentation.details.model.GroupVerificationSummaryUiState
import com.cbgm.sparrow.feature.chats.presentation.details.model.GroupVerificationUiState
import com.cbgm.sparrow.feature.contacts.domain.model.Contact
import com.cbgm.sparrow.feature.contacts.presentation.overview.mapper.filterContacts
import com.cbgm.sparrow.feature.contacts.presentation.overview.mapper.groupContactsByInitial

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
                        row.toGroupMemberVerificationState()
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
                        ownPair?.toGroupMemberVerificationState()
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

private fun GroupVerificationPair.toGroupMemberVerificationState(): GroupMemberVerificationState =
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

internal fun toGroupAvatarUiState(
    title: String,
    avatarBytes: ByteArray?,
    canEdit: Boolean,
    isSaving: Boolean,
    errorMessage: String?
): GroupAvatarUiState =
    GroupAvatarUiState(
        title = title,
        avatarBytes = avatarBytes,
        canEdit = canEdit,
        isSaving = isSaving,
        errorMessage = errorMessage
    )

internal fun toGroupVerificationUiState(
    summary: GroupVerificationSummaryUiState,
    groupAvatar: GroupAvatarUiState,
    contacts: List<Contact>,
    profilePictures: Map<String, ByteArray?>,
    selectedContactId: String?,
    safetyNumber: String,
    isLoadingSafetyNumber: Boolean,
    isVerifying: Boolean,
    verificationError: String?,
    selectedContactIds: Set<String>,
    searchQuery: String,
    removalCandidateContactId: String?,
    promotionCandidateContactId: String?,
    isUpdatingMembers: Boolean,
    memberManagementError: String?,
    completedRevision: Int,
    leave: GroupLeaveUiState
): GroupVerificationUiState {
    val blockedContactIds =
        summary.currentMemberContactIds.toMutableSet().also { blocked ->
            summary.members
                .filterNot(GroupMemberVerificationUiState::isActive)
                .filter { member -> member.state == GroupMemberVerificationState.INVITATION_PENDING }
                .mapNotNullTo(blocked, GroupMemberVerificationUiState::contactId)
        }
    val availableContacts = contacts.filterNot { contact -> contact.id in blockedContactIds }

    return GroupVerificationUiState(
        summary = summary,
        selectedMember =
            selectedContactId?.let { contactId ->
                summary.members.firstOrNull { member ->
                    member.contactId == contactId && member.canVerify
                }
            },
        safetyNumber = safetyNumber,
        isLoadingSafetyNumber = isLoadingSafetyNumber,
        isVerifying = isVerifying,
        errorMessage = verificationError,
        groupAvatar = groupAvatar,
        memberManagement =
            GroupMemberManagementUiState(
                availableContactGroups =
                    availableContacts
                        .filterContacts(searchQuery)
                        .groupContactsByInitial(),
                profilePictures = profilePictures,
                selectedContactIds =
                    selectedContactIds.filterTo(mutableSetOf()) { contactId ->
                        availableContacts.any { contact -> contact.id == contactId }
                    },
                searchQuery = searchQuery,
                removalCandidate =
                    removalCandidateContactId?.let { contactId ->
                        summary.members.firstOrNull { member ->
                            !member.isGroupAdmin && member.contactId == contactId
                        }
                    },
                promotionCandidate =
                    promotionCandidateContactId?.let { contactId ->
                        summary.members.firstOrNull { member ->
                            !member.isGroupAdmin && member.contactId == contactId
                        }
                    },
                promotionRequiredForLeave = summary.requiresAdminPromotionBeforeLeave,
                isUpdating = isUpdatingMembers,
                errorMessage = memberManagementError,
                completedRevision = completedRevision
            ),
        leave = leave
    )
}
