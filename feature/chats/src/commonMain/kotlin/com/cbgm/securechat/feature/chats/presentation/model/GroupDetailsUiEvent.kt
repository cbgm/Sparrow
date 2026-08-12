package com.cbgm.securechat.feature.chats.presentation.model

sealed interface GroupDetailsUiEvent {
    data object BackClicked : GroupDetailsUiEvent

    data class VerifyMemberClicked(
        val contactId: String
    ) : GroupDetailsUiEvent

    data object AddMembersClicked : GroupDetailsUiEvent

    data class RemoveMemberClicked(
        val contactId: String
    ) : GroupDetailsUiEvent

    data class PromoteMemberClicked(
        val contactId: String
    ) : GroupDetailsUiEvent

    data class PromoteMemberAndLeaveClicked(
        val contactId: String
    ) : GroupDetailsUiEvent

    data object LeaveGroupClicked : GroupDetailsUiEvent

    data object VerifySelectedMemberClicked : GroupDetailsUiEvent

    data class ScanMemberQrClicked(
        val contactId: String
    ) : GroupDetailsUiEvent

    data object VerificationBackClicked : GroupDetailsUiEvent

    data object MemberRemovalConfirmed : GroupDetailsUiEvent

    data object MemberRemovalDismissed : GroupDetailsUiEvent

    data object MemberPromotionConfirmed : GroupDetailsUiEvent

    data object MemberPromotionDismissed : GroupDetailsUiEvent

    data object LeaveGroupConfirmed : GroupDetailsUiEvent

    data object LeaveGroupDismissed : GroupDetailsUiEvent
}

sealed interface AddGroupMembersUiEvent {
    data class SearchQueryChanged(
        val query: String
    ) : AddGroupMembersUiEvent

    data class ContactSelected(
        val contactId: String
    ) : AddGroupMembersUiEvent

    data object AddMembersClicked : AddGroupMembersUiEvent

    data object BackClicked : AddGroupMembersUiEvent
}
