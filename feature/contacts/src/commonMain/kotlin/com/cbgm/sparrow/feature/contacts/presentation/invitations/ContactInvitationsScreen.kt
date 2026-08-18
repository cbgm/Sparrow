package com.cbgm.sparrow.feature.contacts.presentation.invitations

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.MarkEmailUnread
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.cbgm.sparrow.core.ui.component.SparrowAvatar
import com.cbgm.sparrow.core.ui.component.SparrowLazyScaffold
import com.cbgm.sparrow.core.ui.component.SparrowSwipeRevealItem
import com.cbgm.sparrow.core.ui.component.SwipeRevealAction
import com.cbgm.sparrow.core.ui.theme.spacing
import com.cbgm.sparrow.feature.contacts.domain.model.PendingContactInvitation
import com.cbgm.sparrow.feature.contacts.presentation.invitations.model.ContactInvitationUiEvent
import com.cbgm.sparrow.feature.contacts.presentation.invitations.model.ContactInvitationUiState
import com.cbgm.sparrow.resources.Res
import com.cbgm.sparrow.resources.base_unknown
import com.cbgm.sparrow.resources.feature_contacts_accept_invitation
import com.cbgm.sparrow.resources.feature_contacts_block_invitation
import com.cbgm.sparrow.resources.feature_contacts_close_invitations
import com.cbgm.sparrow.resources.feature_contacts_decline_invitation
import com.cbgm.sparrow.resources.feature_contacts_invitations_empty
import com.cbgm.sparrow.resources.feature_contacts_invitations_title
import org.jetbrains.compose.resources.stringResource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContactInvitationsScreen(
    uiState: ContactInvitationUiState,
    snackbarHostState: SnackbarHostState,
    onUiEvent: (ContactInvitationUiEvent) -> Unit,
    modifier: Modifier = Modifier
) {
    SparrowLazyScaffold(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHostState = snackbarHostState,
        topBar = { containerColor ->
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(Res.string.feature_contacts_invitations_title),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { onUiEvent(ContactInvitationUiEvent.CloseClicked) }) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = stringResource(Res.string.feature_contacts_close_invitations)
                        )
                    }
                },
                colors =
                    TopAppBarDefaults.topAppBarColors(
                        containerColor = containerColor,
                        scrolledContainerColor = containerColor,
                        titleContentColor = MaterialTheme.colorScheme.onBackground,
                        navigationIconContentColor = MaterialTheme.colorScheme.onBackground
                    )
            )
        }
    ) { innerPadding, listState ->
        if (uiState.invitations.isEmpty()) {
            EmptyInvitations(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
            )
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
                    items = uiState.invitations,
                    key = PendingContactInvitation::invitationId
                ) { invitation ->
                    SparrowSwipeRevealItem(
                        enabled = uiState.processingInvitationId == null,
                        actions =
                            listOf(
                                SwipeRevealAction(
                                    backgroundColor =
                                        MaterialTheme.colorScheme.secondaryContainer,
                                    contentColor =
                                        MaterialTheme.colorScheme.onSecondaryContainer,
                                    onClick = {
                                        onUiEvent(
                                            ContactInvitationUiEvent.AcceptClicked(
                                                invitation.invitationId
                                            )
                                        )
                                    }
                                ) {
                                    InvitationSwipeActionContent(
                                        icon = Icons.Default.Check,
                                        label =
                                            stringResource(
                                                Res.string.feature_contacts_accept_invitation
                                            )
                                    )
                                },
                                SwipeRevealAction(
                                    backgroundColor = MaterialTheme.colorScheme.surfaceVariant,
                                    contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                    onClick = {
                                        onUiEvent(
                                            ContactInvitationUiEvent.DeclineClicked(
                                                invitation.invitationId
                                            )
                                        )
                                    }
                                ) {
                                    InvitationSwipeActionContent(
                                        icon = Icons.Default.Close,
                                        label =
                                            stringResource(
                                                Res.string.feature_contacts_decline_invitation
                                            )
                                    )
                                },
                                SwipeRevealAction(
                                    backgroundColor = MaterialTheme.colorScheme.error,
                                    contentColor = MaterialTheme.colorScheme.onError,
                                    onClick = {
                                        onUiEvent(
                                            ContactInvitationUiEvent.DeclineAndBlockClicked(
                                                invitation.invitationId
                                            )
                                        )
                                    }
                                ) {
                                    InvitationSwipeActionContent(
                                        icon = Icons.Default.Block,
                                        label =
                                            stringResource(
                                                Res.string.feature_contacts_block_invitation
                                            )
                                    )
                                }
                            )
                    ) {
                        InvitationRow(
                            invitation = invitation,
                            isProcessing = uiState.processingInvitationId == invitation.invitationId,
                            profilePictureBytes = uiState.profilePictures[invitation.contactId]
                        )
                        HorizontalDivider(
                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .padding(start = 80.dp),
                            color = MaterialTheme.colorScheme.onPrimary.copy(alpha = .05f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyInvitations(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Outlined.MarkEmailUnread,
            contentDescription = null,
            modifier = Modifier.size(40.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = stringResource(Res.string.feature_contacts_invitations_empty),
            modifier = Modifier.padding(top = MaterialTheme.spacing.small),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun InvitationRow(
    invitation: PendingContactInvitation,
    isProcessing: Boolean,
    profilePictureBytes: ByteArray?,
    modifier: Modifier = Modifier
) {
    val displayName =
        invitation.contactName
            ?: invitation.contactPhoneNumber
            ?: stringResource(Res.string.base_unknown)

    ListItem(
        modifier = modifier.fillMaxWidth(),
        leadingContent = {
            SparrowAvatar(name = displayName, pictureBytes = profilePictureBytes)
        },
        headlineContent = {
            Text(
                text = displayName,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        },
        supportingContent = {
            invitation.contactPhoneNumber
                ?.takeIf { invitation.contactName != null }
                ?.let { phoneNumber ->
                    Text(
                        text = phoneNumber,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimary.copy(alpha = .74f)
                    )
                }
        },
        trailingContent = {
            if (isProcessing) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    strokeWidth = 2.dp
                )
            }
        },
        colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.background)
    )
}

@Composable
private fun InvitationSwipeActionContent(
    icon: ImageVector,
    label: String
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(24.dp)
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            maxLines = 1
        )
    }
}
