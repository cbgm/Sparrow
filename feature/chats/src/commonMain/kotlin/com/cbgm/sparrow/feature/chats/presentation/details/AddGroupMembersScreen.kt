package com.cbgm.sparrow.feature.chats.presentation.details

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.cbgm.sparrow.core.ui.theme.SparrowTheme
import com.cbgm.sparrow.feature.chats.presentation.details.model.AddGroupMembersUiEvent
import com.cbgm.sparrow.feature.chats.presentation.details.model.GroupMemberManagementUiState
import com.cbgm.sparrow.feature.contacts.presentation.overview.ContactsScreen
import com.cbgm.sparrow.feature.contacts.presentation.overview.model.ContactsScreenMode
import com.cbgm.sparrow.feature.contacts.presentation.overview.model.ContactsUiEvent
import com.cbgm.sparrow.feature.contacts.presentation.overview.model.ContactsUiState
import com.cbgm.sparrow.resources.Res
import com.cbgm.sparrow.resources.feature_chats_group_add_members
import org.jetbrains.compose.resources.stringResource

@Composable
fun AddGroupMembersScreen(
    uiState: GroupMemberManagementUiState,
    onUiEvent: (AddGroupMembersUiEvent) -> Unit,
    modifier: Modifier = Modifier
) {
    Content(
        uiState = uiState,
        onUiEvent = onUiEvent,
        modifier = modifier
    )
}

@Composable
private fun Content(
    uiState: GroupMemberManagementUiState,
    onUiEvent: (AddGroupMembersUiEvent) -> Unit,
    modifier: Modifier = Modifier
) {
    val title = stringResource(Res.string.feature_chats_group_add_members)

    ContactsScreen(
        uiState =
            ContactsUiState.Content(
                groups = uiState.availableContactGroups
            ),
        mode =
            ContactsScreenMode.MemberSelection(
                title = title,
                selectedContactIds = uiState.selectedContactIds,
                confirmEnabled = uiState.canAddSelected,
                confirming = uiState.isUpdating,
                searchQuery = uiState.searchQuery
            ),
        onUiEvent = { event ->
            handleUiEvent(
                event = event,
                onUiEvent = onUiEvent
            )
        },
        modifier = modifier
    )
}

private fun handleUiEvent(
    event: ContactsUiEvent,
    onUiEvent: (AddGroupMembersUiEvent) -> Unit
) {
    when (event) {
        is ContactsUiEvent.SearchQueryChanged ->
            onUiEvent(AddGroupMembersUiEvent.SearchQueryChanged(event.query))

        is ContactsUiEvent.ContactSelectionToggled ->
            onUiEvent(AddGroupMembersUiEvent.ContactSelected(event.contactId))

        ContactsUiEvent.SelectionConfirmed -> onUiEvent(AddGroupMembersUiEvent.AddMembersClicked)
        ContactsUiEvent.BackClicked -> onUiEvent(AddGroupMembersUiEvent.BackClicked)
        else -> Unit
    }
}

@Preview
@Composable
private fun AddGroupMembersScreenPreview() {
    SparrowTheme {
        AddGroupMembersScreen(
            uiState = GroupMemberManagementUiState(),
            onUiEvent = {}
        )
    }
}
