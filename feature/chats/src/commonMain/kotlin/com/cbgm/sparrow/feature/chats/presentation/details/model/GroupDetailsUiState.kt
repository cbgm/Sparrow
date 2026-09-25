package com.cbgm.sparrow.feature.chats.presentation.details.model

sealed interface GroupDetailsUiState {
    data object Loading : GroupDetailsUiState

    data class Content(
        val summary: GroupVerificationSummaryUiState,
        val groupAvatar: GroupAvatarUiState = GroupAvatarUiState(),
        val groupTitle: GroupTitleUiState = GroupTitleUiState(),
        val groupDescription: GroupDescriptionUiState = GroupDescriptionUiState()
    ) : GroupDetailsUiState

    data class Error(
        val message: String
    ) : GroupDetailsUiState
}
