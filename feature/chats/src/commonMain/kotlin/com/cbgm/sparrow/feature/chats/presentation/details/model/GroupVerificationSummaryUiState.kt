package com.cbgm.sparrow.feature.chats.presentation.details.model

enum class GroupMemberVerificationState {
    GROUP_ADMIN,
    MUTUALLY_VERIFIED,
    ADMIN_VERIFIED_PARTICIPANT,
    PARTICIPANT_VERIFIED_ADMIN,
    UNVERIFIED,
    UNAVAILABLE,
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
    val canLeaveGroup: Boolean = false,
    val adminCount: Int = 0,
    val currentMemberContactIds: Set<String> = emptySet(),
    val requiresAdminPromotionBeforeLeave: Boolean = false,
    val promotableContactIds: Set<String> = emptySet()
) {
    val isFullyVerified: Boolean
        get() =
            hasAuthoritativeState &&
                activeParticipantCount > 0 &&
                mutuallyVerifiedParticipantCount == activeParticipantCount
}
