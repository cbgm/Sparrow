package com.cbgm.sparrow.feature.contacts.presentation.overview

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.cbgm.sparrow.core.ui.component.SparrowLazyScaffold
import com.cbgm.sparrow.core.ui.theme.SparrowTheme
import com.cbgm.sparrow.core.ui.theme.spacing
import com.cbgm.sparrow.feature.contacts.presentation.overview.components.ContactSelectionCircle
import com.cbgm.sparrow.feature.contacts.presentation.overview.components.ContactStatus
import com.cbgm.sparrow.feature.contacts.presentation.overview.components.ContactsErrorContent
import com.cbgm.sparrow.feature.contacts.presentation.overview.components.ContactsFloatingActionButton
import com.cbgm.sparrow.feature.contacts.presentation.overview.components.CreateGroupListItem
import com.cbgm.sparrow.feature.contacts.presentation.overview.components.EmptyContactsContent
import com.cbgm.sparrow.feature.contacts.presentation.overview.components.GroupSelectionContactsTopBar
import com.cbgm.sparrow.feature.contacts.presentation.overview.components.ImportContactBottomSheet
import com.cbgm.sparrow.feature.contacts.presentation.overview.components.LoadingContactsContent
import com.cbgm.sparrow.feature.contacts.presentation.overview.components.MemberSelectionContactsTopBar
import com.cbgm.sparrow.feature.contacts.presentation.overview.components.OverviewContactsTopBar
import com.cbgm.sparrow.feature.contacts.presentation.overview.components.contactGroups
import com.cbgm.sparrow.feature.contacts.presentation.overview.model.ContactGroupEntity
import com.cbgm.sparrow.feature.contacts.presentation.overview.model.ContactsScreenMode
import com.cbgm.sparrow.feature.contacts.presentation.overview.model.ContactsUiEvent
import com.cbgm.sparrow.feature.contacts.presentation.overview.model.ContactsUiState
import com.cbgm.sparrow.resources.Res
import com.cbgm.sparrow.resources.base_import_contact
import org.jetbrains.compose.resources.stringResource

@Composable
fun ContactsScreen(
    uiState: ContactsUiState,
    mode: ContactsScreenMode,
    onUiEvent: (ContactsUiEvent) -> Unit,
    modifier: Modifier = Modifier,
    snackbarHostState: SnackbarHostState? = null
) {
    var showImportSheet by rememberSaveable {
        mutableStateOf(false)
    }

    SparrowLazyScaffold(
        modifier = modifier,
        snackbarHostState = snackbarHostState,
        containerColor = MaterialTheme.colorScheme.background,
        topBar = { containerColor ->
            when (mode) {
                is ContactsScreenMode.Overview -> {
                    OverviewContactsTopBar(
                        containerColor = containerColor,
                        searchQuery = mode.searchQuery,
                        onSearchQueryChanged = { query ->
                            onUiEvent(ContactsUiEvent.SearchQueryChanged(query))
                        },
                        onBack = { onUiEvent(ContactsUiEvent.BackClicked) }
                    )
                }

                is ContactsScreenMode.GroupSelection -> {
                    GroupSelectionContactsTopBar(
                        title = mode.title,
                        searchQuery = mode.searchQuery,
                        confirmEnabled = mode.confirmEnabled,
                        confirming = mode.confirming,
                        containerColor = containerColor,
                        onBack = { onUiEvent(ContactsUiEvent.BackClicked) },
                        onTitleChanged = { title ->
                            onUiEvent(ContactsUiEvent.SelectionTitleChanged(title))
                        },
                        onSearchQueryChanged = { query ->
                            onUiEvent(ContactsUiEvent.SearchQueryChanged(query))
                        },
                        onConfirmed = { onUiEvent(ContactsUiEvent.SelectionConfirmed) }
                    )
                }

                is ContactsScreenMode.MemberSelection -> {
                    MemberSelectionContactsTopBar(
                        title = mode.title,
                        searchQuery = mode.searchQuery,
                        confirmEnabled = mode.confirmEnabled,
                        confirming = mode.confirming,
                        containerColor = containerColor,
                        onBack = { onUiEvent(ContactsUiEvent.BackClicked) },
                        onSearchQueryChanged = { query ->
                            onUiEvent(ContactsUiEvent.SearchQueryChanged(query))
                        },
                        onConfirmed = { onUiEvent(ContactsUiEvent.SelectionConfirmed) }
                    )
                }
            }
        },
        floatingActionButton = {
            if (
                mode is ContactsScreenMode.Overview &&
                (uiState is ContactsUiState.Empty || uiState is ContactsUiState.Content)
            ) {
                ContactsFloatingActionButton(
                    onClick = {
                        showImportSheet = true
                    }
                )
            }
        }
    ) { innerPadding, listState ->
        ContactsContent(
            uiState = uiState,
            mode = mode,
            innerPadding = innerPadding,
            listState = listState,
            onUiEvent = onUiEvent
        )
    }

    if (showImportSheet && mode is ContactsScreenMode.Overview) {
        ImportContactBottomSheet(
            onDismiss = {
                showImportSheet = false
            },
            onImportContact = {
                showImportSheet = false
                onUiEvent(ContactsUiEvent.ImportContactClicked)
            },
            onImportDeviceContacts = {
                showImportSheet = false
                onUiEvent(ContactsUiEvent.ImportDeviceContacts)
            }
        )
    }
}

@Composable
private fun ContactsContent(
    uiState: ContactsUiState,
    mode: ContactsScreenMode,
    innerPadding: PaddingValues,
    listState: LazyListState,
    onUiEvent: (ContactsUiEvent) -> Unit
) {
    when (uiState) {
        ContactsUiState.Loading -> {
            LoadingContactsContent(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
            )
        }

        ContactsUiState.Empty -> {
            EmptyContactsContent(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .padding(MaterialTheme.spacing.medium)
            )
        }

        is ContactsUiState.Content -> {
            ContactsList(
                groups = uiState.groups,
                mode = mode,
                innerPadding = innerPadding,
                listState = listState,
                onUiEvent = onUiEvent
            )
        }

        is ContactsUiState.Error -> {
            val isOverview = mode is ContactsScreenMode.Overview

            ContactsErrorContent(
                message = uiState.message,
                actionText =
                    if (isOverview) {
                        stringResource(Res.string.base_import_contact)
                    } else {
                        null
                    },
                onAction = {
                    if (isOverview) {
                        onUiEvent(ContactsUiEvent.ImportContactClicked)
                    }
                },
                modifier =
                    Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .padding(MaterialTheme.spacing.medium)
            )
        }
    }
}

@Composable
private fun ContactsList(
    groups: List<ContactGroupEntity>,
    mode: ContactsScreenMode,
    innerPadding: PaddingValues,
    listState: LazyListState,
    onUiEvent: (ContactsUiEvent) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        state = listState,
        contentPadding =
            PaddingValues(
                start = MaterialTheme.spacing.medium,
                top = innerPadding.calculateTopPadding(),
                end = MaterialTheme.spacing.medium,
                bottom = innerPadding.calculateBottomPadding()
            ),
        verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.medium)
    ) {
        if (mode is ContactsScreenMode.Overview) {
            item(key = "create_group") {
                CreateGroupListItem(
                    onClick = { onUiEvent(ContactsUiEvent.CreateGroupClicked) }
                )
            }
        }

        contactGroups(
            groups = groups,
            onContactClick = { contact ->
                when (mode) {
                    is ContactsScreenMode.Overview -> {
                        onUiEvent(
                            ContactsUiEvent.ContactClicked(
                                contactId = contact.id,
                                contactName = contact.displayName.orEmpty()
                            )
                        )
                    }

                    is ContactsScreenMode.GroupSelection,
                    is ContactsScreenMode.MemberSelection -> {
                        onUiEvent(ContactsUiEvent.ContactSelectionToggled(contact.id))
                    }
                }
            },
            trailingContent = { contact ->
                when (mode) {
                    is ContactsScreenMode.Overview -> {
                        ContactStatus(contact = contact)
                    }

                    is ContactsScreenMode.GroupSelection -> {
                        ContactSelectionCircle(
                            selected = contact.id in mode.selectedContactIds
                        )
                    }

                    is ContactsScreenMode.MemberSelection -> {
                        ContactSelectionCircle(
                            selected = contact.id in mode.selectedContactIds
                        )
                    }
                }
            }
        )
    }
}

@Preview
@Composable
fun ContactsScreenPreview() {
    SparrowTheme {
        ContactsScreen(
            uiState =
                ContactsUiState.Content(
                    groups = listOf()
                ),
            mode = ContactsScreenMode.Overview(searchQuery = ""),
            onUiEvent = {}
        )
    }
}
