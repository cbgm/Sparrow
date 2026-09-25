package com.cbgm.sparrow.feature.chats.presentation.create

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.cbgm.sparrow.core.ui.theme.SparrowTheme
import com.cbgm.sparrow.feature.chats.presentation.create.model.CreateGroupConversationUiState
import com.cbgm.sparrow.feature.chats.presentation.create.model.CreateGroupUiEvent
import com.cbgm.sparrow.feature.contacts.presentation.overview.ContactsScreen
import com.cbgm.sparrow.feature.contacts.presentation.overview.model.ContactsScreenMode
import com.cbgm.sparrow.feature.contacts.presentation.overview.model.ContactsUiEvent
import com.cbgm.sparrow.feature.contacts.presentation.overview.model.ContactsUiState

@Composable
fun CreateGroupScreen(
    uiState: CreateGroupConversationUiState,
    onUiEvent: (CreateGroupUiEvent) -> Unit,
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
    uiState: CreateGroupConversationUiState,
    onUiEvent: (CreateGroupUiEvent) -> Unit,
    modifier: Modifier = Modifier
) {
    ContactsScreen(
        uiState = ContactsUiState.Content(
            groups = uiState.contactGroups
        ),
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
        modifier = modifier
    )
}

@Preview
@Composable
private fun CreateGroupScreenPreview() {
    SparrowTheme {
        CreateGroupScreen(
            uiState = CreateGroupConversationUiState(),
            onUiEvent = {}
        )
    }
}
