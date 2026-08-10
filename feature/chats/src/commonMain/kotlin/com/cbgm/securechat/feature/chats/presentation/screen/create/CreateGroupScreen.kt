package com.cbgm.securechat.feature.chats.presentation.screen

import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.cbgm.securechat.feature.chats.presentation.model.CreateGroupUiEvent
import com.cbgm.securechat.feature.chats.presentation.model.CreateGroupUiState
import com.cbgm.securechat.feature.contacts.presentation.model.ContactsScreenMode
import com.cbgm.securechat.feature.contacts.presentation.model.ContactsUiEvent
import com.cbgm.securechat.feature.contacts.presentation.model.ContactsUiState
import com.cbgm.securechat.feature.contacts.presentation.screen.ContactsScreen

@Composable
fun CreateGroupScreen(
    uiState: CreateGroupUiState,
    onUiEvent: (CreateGroupUiEvent) -> Unit,
    modifier: Modifier = Modifier
) {
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let { message ->
            snackbarHostState.showSnackbar(message)
        }
    }

    ContactsScreen(
        uiState = ContactsUiState.Content(groups = uiState.contactGroups),
        mode =
            ContactsScreenMode.GroupSelection(
                title = uiState.title,
                selectedContactIds = uiState.selectedContactIds,
                confirmEnabled = uiState.canCreate,
                confirming = uiState.isCreating,
                searchQuery = uiState.searchQuery
            ),
        onUiEvent = { event ->
            when (event) {
                is ContactsUiEvent.SearchQueryChanged ->
                    onUiEvent(CreateGroupUiEvent.SearchQueryChanged(event.query))
                is ContactsUiEvent.SelectionTitleChanged ->
                    onUiEvent(CreateGroupUiEvent.TitleChanged(event.title))
                is ContactsUiEvent.ContactSelectionToggled ->
                    onUiEvent(CreateGroupUiEvent.ContactSelected(event.contactId))
                ContactsUiEvent.SelectionConfirmed -> onUiEvent(CreateGroupUiEvent.CreateClicked)
                ContactsUiEvent.BackClicked -> onUiEvent(CreateGroupUiEvent.BackClicked)
                else -> Unit
            }
        },
        modifier = modifier,
        snackbarHostState = snackbarHostState
    )
}
