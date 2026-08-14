package com.cbgm.sparrow.feature.chats.presentation.details

import com.cbgm.sparrow.feature.chats.presentation.details.model.GroupMemberVerificationState
import com.cbgm.sparrow.feature.chats.presentation.details.model.GroupMemberVerificationUiState
import com.cbgm.sparrow.feature.chats.presentation.details.model.GroupVerificationSummaryUiState

internal object GroupDetailsPreviewData {
    val admin =
        GroupMemberVerificationUiState(
            invitationId = "admin",
            displayName = "Chris",
            state = GroupMemberVerificationState.GROUP_ADMIN,
            canVerify = false,
            contactId = "admin-contact",
            isGroupAdmin = true,
            isActive = true
        )

    val participant =
        GroupMemberVerificationUiState(
            invitationId = "participant",
            displayName = "Alex",
            state = GroupMemberVerificationState.UNVERIFIED,
            canVerify = true,
            contactId = "participant-contact",
            isGroupAdmin = false,
            isActive = true
        )

    val summary =
        GroupVerificationSummaryUiState(
            hasAuthoritativeState = true,
            isLocalAdmin = true,
            members =
                listOf(
                    admin,
                    participant,
                    GroupMemberVerificationUiState(
                        invitationId = "pending",
                        displayName = "Sam",
                        state = GroupMemberVerificationState.INVITATION_PENDING,
                        canVerify = false,
                        contactId = "pending-contact",
                        isGroupAdmin = false,
                        isActive = false
                    )
                ),
            totalMemberCount = 3,
            mutuallyVerifiedParticipantCount = 0,
            activeParticipantCount = 2
        )
}
