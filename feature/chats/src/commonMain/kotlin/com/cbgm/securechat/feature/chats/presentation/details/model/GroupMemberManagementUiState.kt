package com.cbgm.securechat.feature.chats.presentation.details.model

import com.cbgm.securechat.feature.contacts.presentation.model.ContactGroupEntity

data class GroupMemberManagementUiState(
    val availableContactGroups: List<ContactGroupEntity> = emptyList(),
    val selectedContactIds: Set<String> = emptySet(),
    val searchQuery: String = "",
    val removalCandidate: GroupMemberVerificationUiState? = null,
    val promotionCandidate: GroupMemberVerificationUiState? = null,
    val promotionRequiredForLeave: Boolean = false,
    val isUpdating: Boolean = false,
    val errorMessage: String? = null,
    val completedRevision: Int = 0
) {
    val canAddSelected: Boolean
        get() = selectedContactIds.isNotEmpty() && !isUpdating
}
