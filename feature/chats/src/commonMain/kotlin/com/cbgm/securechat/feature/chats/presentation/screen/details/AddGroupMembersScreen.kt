package com.cbgm.securechat.feature.chats.presentation.screen.details

import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.cbgm.securechat.core.ui.theme.SecureChatTheme
import com.cbgm.securechat.feature.chats.presentation.model.AddGroupMembersUiEvent
import com.cbgm.securechat.feature.chats.presentation.model.GroupMemberManagementUiState
import com.cbgm.securechat.feature.contacts.presentation.model.ContactsScreenMode
import com.cbgm.securechat.feature.contacts.presentation.model.ContactsUiEvent
import com.cbgm.securechat.feature.contacts.presentation.model.ContactsUiState
import com.cbgm.securechat.feature.contacts.presentation.screen.ContactsScreen
import com.cbgm.securechat.resources.Res
import com.cbgm.securechat.resources.feature_chats_group_add_members
import org.jetbrains.compose.resources.stringResource

@Composable
fun AddGroupMembersScreen(
    uiState: GroupMemberManagementUiState,
    onUiEvent: (AddGroupMembersUiEvent) -> Unit,
    modifier: Modifier = Modifier
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val title = stringResource(Res.string.feature_chats_group_add_members)

    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let { message ->
            snackbarHostState.showSnackbar(message)
        }
    }

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
        onUiEvent = { event -> handleUiEvent(event) },
        modifier = modifier,
        snackbarHostState = snackbarHostState
    )
}

private fun handleUiEvent(event: ContactsUiEvent) =
    when (event) {
        is ContactsUiEvent.SearchQueryChanged ->
            AddGroupMembersUiEvent.SearchQueryChanged(event.query)

        is ContactsUiEvent.ContactSelectionToggled ->
            AddGroupMembersUiEvent.ContactSelected(event.contactId)

        ContactsUiEvent.SelectionConfirmed -> AddGroupMembersUiEvent.AddMembersClicked
        ContactsUiEvent.BackClicked -> AddGroupMembersUiEvent.BackClicked
        else -> Unit
    }

@Preview
@Composable
private fun AddGroupMembersScreenPreview() {
    SecureChatTheme {
        AddGroupMembersScreen(
            uiState = GroupMemberManagementUiState(),
            onUiEvent = {}
        )
    }
}
