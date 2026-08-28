package com.cbgm.sparrow.feature.contacts.presentation.overview

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cbgm.sparrow.feature.contacts.domain.model.Contact
import com.cbgm.sparrow.feature.contacts.presentation.overview.model.ContactsScreenMode
import com.cbgm.sparrow.feature.contacts.presentation.overview.model.ContactsUiEvent
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun ContactAttachmentSelectionRoute(
    onContactSelected: (Contact) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ContactsViewModel = koinViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    ContactsScreen(
        uiState = uiState,
        mode = ContactsScreenMode.AttachmentSelection(searchQuery = uiState.searchQuery),
        onUiEvent = { event ->
            when (event) {
                is ContactsUiEvent.SearchQueryChanged -> viewModel.onUiEvent(event)
                ContactsUiEvent.BackClicked -> onBack()
                else -> Unit
            }
        },
        onContactSelected = onContactSelected,
        modifier = modifier
    )
}
