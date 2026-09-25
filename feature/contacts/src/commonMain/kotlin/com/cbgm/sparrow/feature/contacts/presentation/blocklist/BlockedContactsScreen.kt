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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cbgm.sparrow.core.ui.component.SparrowLazyScaffold
import com.cbgm.sparrow.core.ui.theme.Alpha
import com.cbgm.sparrow.core.ui.theme.Dimens
import com.cbgm.sparrow.core.ui.theme.SparrowTheme
import com.cbgm.sparrow.core.ui.theme.spacing
import com.cbgm.sparrow.feature.avatar.domain.model.AvatarTarget
import com.cbgm.sparrow.feature.avatar.presentation.component.SparrowAvatar
import com.cbgm.sparrow.feature.contacts.presentation.blocklist.components.AddBlockedContactDialog
import com.cbgm.sparrow.feature.contacts.presentation.blocklist.model.BlockedContactsUiEvent
import com.cbgm.sparrow.feature.contacts.presentation.blocklist.model.BlockedContactsUiState
import com.cbgm.sparrow.feature.contacts.presentation.overview.model.ContactUi
import com.cbgm.sparrow.resources.Res
import com.cbgm.sparrow.resources.feature_contacts_blocked_contacts_empty
import com.cbgm.sparrow.resources.feature_contacts_blocked_contacts_title
import com.cbgm.sparrow.resources.feature_contacts_unblock_contact
import com.cbgm.sparrow.resources.feature_contacts_unnamed_contact
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.jetbrains.compose.resources.stringResource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BlockedContactsScreen(
    blockedContacts: StateFlow<List<ContactUi>>,
    processingContactId: StateFlow<String?>,
    addDialogVisible: StateFlow<Boolean>,
    dialogState: StateFlow<BlockedContactsUiState>,
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
                        style = MaterialTheme.typography.titleSmall
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
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { onUiEvent(BlockedContactsUiEvent.AddContactClicked) },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                shape = CircleShape
            ) {
                Icon(imageVector = Icons.Default.Add, contentDescription = null)
            }
        }
    ) { innerPadding, listState ->
        Box(
            modifier =
                Modifier
                    .fillMaxSize()
        ) {
            BlockedContactsList(
                blockedContacts = blockedContacts,
                processingContactId = processingContactId,
                innerPadding = innerPadding,
                listState = listState,
                onUiEvent = onUiEvent
            )
        }
    }

    BlockedContactsDialogHost(
        addDialogVisible = addDialogVisible,
        dialogState = dialogState,
        onUiEvent = onUiEvent
    )
}

@Composable
private fun BlockedContactsList(
    blockedContacts: StateFlow<List<ContactUi>>,
    processingContactId: StateFlow<String?>,
    innerPadding: PaddingValues,
    listState: androidx.compose.foundation.lazy.LazyListState,
    onUiEvent: (BlockedContactsUiEvent) -> Unit
) {
    val contacts by blockedContacts.collectAsStateWithLifecycle()
    val processing by processingContactId.collectAsStateWithLifecycle()
    Box(modifier = Modifier.fillMaxSize()) {
        if (contacts.isEmpty()) {
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
                    tint = MaterialTheme.colorScheme.onBackground.copy(alpha = Alpha.BlockedContactsScreen.icon),
                    modifier = Modifier.size(Dimens.BlockedContactsScreen.avatarSize)
                )
                Text(
                    text = stringResource(Res.string.feature_contacts_blocked_contacts_empty),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = Alpha.OpaqueText)
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
                items(items = contacts, key = ContactUi::id) { contact ->
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(
                                horizontal = MaterialTheme.spacing.screenPadding,
                                vertical = MaterialTheme.spacing.micro
                            ),
                        shape = MaterialTheme.shapes.medium,
                        color = MaterialTheme.colorScheme.background
                    ) {
                        BlockedContactRow(
                            contact = contact,
                            enabled = processing == null,
                            onUnblock = {
                                onUiEvent(BlockedContactsUiEvent.UnblockContactClicked(contact.id))
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun BlockedContactsDialogHost(
    addDialogVisible: StateFlow<Boolean>,
    dialogState: StateFlow<BlockedContactsUiState>,
    onUiEvent: (BlockedContactsUiEvent) -> Unit
) {
    val visible by addDialogVisible.collectAsStateWithLifecycle()
    if (!visible) return
    val state by dialogState.collectAsStateWithLifecycle()
    AddBlockedContactDialog(
        isVisible = true,
        phoneNumber = state.phoneNumber,
        phoneNumberError = state.phoneNumberError,
        contacts = state.availableContacts,
        enabled = state.processingContactId == null,
        onPhoneNumberChanged = { value -> onUiEvent(BlockedContactsUiEvent.PhoneNumberChanged(value)) },
        onBlockPhoneNumber = { onUiEvent(BlockedContactsUiEvent.BlockPhoneNumberClicked) },
        onContactSelected = { contact -> onUiEvent(BlockedContactsUiEvent.BlockContactClicked(contact.id)) },
        onDismiss = { onUiEvent(BlockedContactsUiEvent.AddContactsDismissed) }
    )
}

@Composable
private fun BlockedContactRow(
    contact: ContactUi,
    enabled: Boolean,
    onUnblock: () -> Unit
) {
    Column {
        ListItem(
            leadingContent = {
                SparrowAvatar(
                    name = contact.displayName ?: contact.preferredPhoneNumber ?: "?",
                    target = AvatarTarget.User(contact.id)
                )
            },
            headlineContent = {
                Text(
                    text =
                        contact.displayName ?: contact.preferredPhoneNumber
                            ?: stringResource(Res.string.feature_contacts_unnamed_contact),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    fontWeight = FontWeight.Bold
                )
            },
            supportingContent = {
                contact.preferredPhoneNumber
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
                    trailingContentColor = MaterialTheme.colorScheme.primary
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
            blockedContacts = MutableStateFlow(
                listOf(
                    ContactUi(
                        id = "id",
                        displayName = "John Doe",
                        preferredPhoneNumber = null,
                        phoneNumbers = emptyList(),
                        hasSparrowIdentity = false,
                        deviceContactMissing = true
                    )
                )
            ),
            processingContactId = MutableStateFlow(null),
            addDialogVisible = MutableStateFlow(false),
            dialogState = MutableStateFlow(BlockedContactsUiState()),
            onUiEvent = {}
        )
    }
}
