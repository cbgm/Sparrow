package com.cbgm.sparrow.feature.contacts.presentation.blocklist

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.cbgm.sparrow.core.ui.component.Avatar
import com.cbgm.sparrow.core.ui.component.SparrowLazyScaffold
import com.cbgm.sparrow.core.ui.theme.SparrowTheme
import com.cbgm.sparrow.core.ui.theme.spacing
import com.cbgm.sparrow.feature.contacts.domain.model.Contact
import com.cbgm.sparrow.feature.contacts.domain.model.DeviceContactLinkStatus
import com.cbgm.sparrow.feature.contacts.presentation.blocklist.components.AddBlockedContactDialog
import com.cbgm.sparrow.feature.contacts.presentation.blocklist.model.BlockedContactsUiEvent
import com.cbgm.sparrow.feature.contacts.presentation.blocklist.model.BlockedContactsUiState
import com.cbgm.sparrow.resources.Res
import com.cbgm.sparrow.resources.feature_contacts_add_blocked_contact
import com.cbgm.sparrow.resources.feature_contacts_blocked_contacts_empty
import com.cbgm.sparrow.resources.feature_contacts_blocked_contacts_title
import com.cbgm.sparrow.resources.feature_contacts_unblock_contact
import com.cbgm.sparrow.resources.feature_contacts_unnamed_contact
import org.jetbrains.compose.resources.stringResource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BlockedContactsScreen(
    uiState: BlockedContactsUiState,
    onUiEvent: (BlockedContactsUiEvent) -> Unit,
    modifier: Modifier = Modifier
) {
    SparrowLazyScaffold(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = stringResource(Res.string.feature_contacts_blocked_contacts_title),
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { onUiEvent(BlockedContactsUiEvent.BackClicked) }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = null
                        )
                    }
                },
                colors =
                    TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.background,
                        titleContentColor = MaterialTheme.colorScheme.onBackground,
                        navigationIconContentColor = MaterialTheme.colorScheme.onBackground
                    )
            )
        }
    ) { innerPadding, listState ->
        Box(
            modifier =
                Modifier
                    .fillMaxSize()
        ) {
            if (uiState.blockedContacts.isEmpty()) {
                Column(
                    modifier =
                        Modifier
                            .align(Alignment.Center)
                            .padding(MaterialTheme.spacing.medium),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.small)
                ) {
                    Icon(
                        imageVector = Icons.Default.Block,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.45f),
                        modifier = Modifier.size(40.dp)
                    )
                    Text(
                        text = stringResource(Res.string.feature_contacts_blocked_contacts_empty),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.65f)
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    state = listState,
                    contentPadding =
                        PaddingValues(
                            top = innerPadding.calculateTopPadding(),
                            bottom = innerPadding.calculateBottomPadding()
                        )
                ) {
                    items(
                        items = uiState.blockedContacts,
                        key = Contact::id
                    ) { contact ->
                        BlockedContactRow(
                            contact = contact,
                            profilePictureBytes = uiState.profilePictures[contact.id],
                            enabled = uiState.processingContactId == null,
                            onUnblock = { onUiEvent(BlockedContactsUiEvent.UnblockContactClicked(contact.id)) }
                        )

                        HorizontalDivider(
                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .padding(start = 80.dp),
                            color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.05f)
                        )
                    }
                }
            }

            FloatingActionButton(
                onClick = { onUiEvent(BlockedContactsUiEvent.AddContactClicked) },
                containerColor = MaterialTheme.colorScheme.secondary,
                contentColor = MaterialTheme.colorScheme.background,
                modifier =
                    Modifier
                        .padding(MaterialTheme.spacing.screenPadding)
                        .align(Alignment.BottomEnd)
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = stringResource(Res.string.feature_contacts_add_blocked_contact)
                )
            }
        }
    }

    if (uiState.showAddContacts) {
        AddBlockedContactDialog(
            phoneNumber = uiState.phoneNumber,
            phoneNumberError = uiState.phoneNumberError,
            contacts = uiState.availableContacts,
            profilePictures = uiState.profilePictures,
            enabled = uiState.processingContactId == null,
            onPhoneNumberChanged = { value ->
                onUiEvent(BlockedContactsUiEvent.PhoneNumberChanged(value))
            },
            onBlockPhoneNumber = { onUiEvent(BlockedContactsUiEvent.BlockPhoneNumberClicked) },
            onContactSelected = { contact ->
                onUiEvent(BlockedContactsUiEvent.BlockContactClicked(contact.id))
            },
            onDismiss = { onUiEvent(BlockedContactsUiEvent.AddContactsDismissed) }
        )
    }
}

@Composable
private fun BlockedContactRow(
    contact: Contact,
    profilePictureBytes: ByteArray?,
    enabled: Boolean,
    onUnblock: () -> Unit
) {
    Column {
        ListItem(
            leadingContent = {
                Avatar(
                    name = contact.displayName ?: contact.preferredPhoneNumber?.value ?: "?",
                    pictureBytes = profilePictureBytes
                )
            },
            headlineContent = {
                Text(
                    text =
                        contact.displayName ?: contact.preferredPhoneNumber?.value
                            ?: stringResource(Res.string.feature_contacts_unnamed_contact),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    fontWeight = FontWeight.Bold
                )
            },
            supportingContent = {
                contact.preferredPhoneNumber
                    ?.value
                    ?.takeIf { contact.displayName != null }
                    ?.let { phoneNumber ->
                        Text(
                            text = phoneNumber,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
            },
            trailingContent = {
                IconButton(
                    onClick = onUnblock,
                    enabled = enabled
                ) {
                    Icon(
                        imageVector = Icons.Default.LockOpen,
                        contentDescription = stringResource(Res.string.feature_contacts_unblock_contact)
                    )
                }
            },
            colors =
                ListItemDefaults.colors(
                    containerColor = MaterialTheme.colorScheme.background,
                    contentColor = MaterialTheme.colorScheme.onBackground,
                    trailingContentColor = MaterialTheme.colorScheme.onBackground
                ),
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Preview
@Composable
private fun BlockedContactsScreenPreview() {
    SparrowTheme {
        BlockedContactsScreen(
            uiState =
                BlockedContactsUiState(
                    blockedContacts =
                        listOf(
                            Contact(
                                id = "id",
                                displayName = "John Doe",
                                preferredPhoneNumberId = null,
                                sparrowIdentity = null,
                                phoneNumbers = emptyList(),
                                deviceContactId = "",
                                deviceContactLinkStatus = DeviceContactLinkStatus.MISSING,
                                createdAtEpochMilliseconds = System.currentTimeMillis(),
                                updatedAtEpochMilliseconds = System.currentTimeMillis()
                            )
                        )
                ),
            onUiEvent = {}
        )
    }
}
