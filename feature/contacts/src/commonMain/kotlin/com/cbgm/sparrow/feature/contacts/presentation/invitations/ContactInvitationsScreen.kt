package com.cbgm.sparrow.feature.contacts.presentation.invitations

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.outlined.MarkEmailUnread
import androidx.compose.material3.Badge
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import com.cbgm.sparrow.core.ui.component.SparrowAvatar
import com.cbgm.sparrow.core.ui.component.SparrowLazyScaffold
import com.cbgm.sparrow.core.ui.component.SparrowSwipeRevealItem
import com.cbgm.sparrow.core.ui.component.SwipeRevealAction
import com.cbgm.sparrow.core.ui.theme.Alpha
import com.cbgm.sparrow.core.ui.theme.Dimens
import com.cbgm.sparrow.core.ui.theme.SparrowTheme
import com.cbgm.sparrow.core.ui.theme.spacing
import com.cbgm.sparrow.feature.contacts.domain.model.ContactInvitation
import com.cbgm.sparrow.feature.contacts.domain.model.ContactInvitationStatus
import com.cbgm.sparrow.feature.contacts.domain.model.IdentityInvitationDirection
import com.cbgm.sparrow.feature.contacts.presentation.invitations.model.ContactInvitationTab
import com.cbgm.sparrow.feature.contacts.presentation.invitations.model.ContactInvitationUiEvent
import com.cbgm.sparrow.feature.contacts.presentation.invitations.model.ContactInvitationUiState
import com.cbgm.sparrow.resources.Res
import com.cbgm.sparrow.resources.base_unknown
import com.cbgm.sparrow.resources.feature_contacts_accept_invitation
import com.cbgm.sparrow.resources.feature_contacts_block_invitation
import com.cbgm.sparrow.resources.feature_contacts_close_invitations
import com.cbgm.sparrow.resources.feature_contacts_decline_invitation
import com.cbgm.sparrow.resources.feature_contacts_delete_outgoing_invitation
import com.cbgm.sparrow.resources.feature_contacts_invitation_status_declined
import com.cbgm.sparrow.resources.feature_contacts_invitation_status_expired
import com.cbgm.sparrow.resources.feature_contacts_invitation_status_failed
import com.cbgm.sparrow.resources.feature_contacts_invitation_status_pending
import com.cbgm.sparrow.resources.feature_contacts_invitations_incoming
import com.cbgm.sparrow.resources.feature_contacts_invitations_incoming_empty
import com.cbgm.sparrow.resources.feature_contacts_invitations_outgoing
import com.cbgm.sparrow.resources.feature_contacts_invitations_outgoing_empty
import com.cbgm.sparrow.resources.feature_contacts_invitations_title
import org.jetbrains.compose.resources.StringResource
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
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(top = innerPadding.calculateTopPadding())
        ) {
            InvitationTabs(
                selectedTab = uiState.selectedTab,
                hasUnreadIncomingUpdates = uiState.hasUnreadIncomingUpdates,
                hasUnreadOutgoingUpdates = uiState.hasUnreadOutgoingUpdates,
                onTabSelected = { tab ->
                    onUiEvent(ContactInvitationUiEvent.TabSelected(tab))
                }
            )

            if (uiState.selectedInvitations.isEmpty()) {
                EmptyInvitations(
                    selectedTab = uiState.selectedTab,
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .padding(bottom = innerPadding.calculateBottomPadding())
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    state = listState,
                    contentPadding =
                        PaddingValues(
                            bottom = innerPadding.calculateBottomPadding()
                        )
                ) {
                    items(
                        items = uiState.selectedInvitations,
                        key = ContactInvitation::invitationId
                    ) { invitation ->
                        InvitationItem(
                            invitation = invitation,
                            isProcessing = uiState.processingInvitationId == invitation.invitationId,
                            profilePictureBytes = uiState.profilePictures[invitation.contactId],
                            actionsEnabled = uiState.processingInvitationId == null,
                            onUiEvent = onUiEvent
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun InvitationTabs(
    selectedTab: ContactInvitationTab,
    hasUnreadIncomingUpdates: Boolean,
    hasUnreadOutgoingUpdates: Boolean,
    onTabSelected: (ContactInvitationTab) -> Unit
) {
    PrimaryTabRow(
        selectedTabIndex = selectedTab.ordinal,
        indicator = {
            Box(
                modifier = Modifier
                    .tabIndicatorOffset(selectedTabIndex = selectedTab.ordinal)
                    .fillMaxWidth()
                    .height(Dimens.ContactInvitationsScreen.tabIndicatorHeight)
                    .background(MaterialTheme.colorScheme.onSurfaceVariant) // Set your indicator color
            )
        }
    ) {
        InvitationTab(
            title = stringResource(Res.string.feature_contacts_invitations_incoming),
            selected = selectedTab == ContactInvitationTab.INCOMING,
            hasUnreadUpdate = hasUnreadIncomingUpdates,
            onClick = { onTabSelected(ContactInvitationTab.INCOMING) }
        )
        InvitationTab(
            title = stringResource(Res.string.feature_contacts_invitations_outgoing),
            selected = selectedTab == ContactInvitationTab.OUTGOING,
            hasUnreadUpdate = hasUnreadOutgoingUpdates,
            onClick = { onTabSelected(ContactInvitationTab.OUTGOING) }
        )
    }
}

@Composable
private fun InvitationTab(
    title: String,
    selected: Boolean,
    hasUnreadUpdate: Boolean,
    onClick: () -> Unit
) {
    Tab(
        selected = selected,
        onClick = onClick,
        text = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = title,
                    color = MaterialTheme.colorScheme.onSurface,
                    style = MaterialTheme.typography.bodyLarge
                )
                if (hasUnreadUpdate) {
                    Badge(modifier = Modifier.padding(start = MaterialTheme.spacing.micro))
                }
            }
        }
    )
}

@Composable
private fun InvitationItem(
    invitation: ContactInvitation,
    isProcessing: Boolean,
    profilePictureBytes: ByteArray?,
    actionsEnabled: Boolean,
    onUiEvent: (ContactInvitationUiEvent) -> Unit
) {
    when (invitation.direction) {
        IdentityInvitationDirection.INCOMING ->
            IncomingInvitationItem(
                invitation = invitation,
                isProcessing = isProcessing,
                profilePictureBytes = profilePictureBytes,
                actionsEnabled = actionsEnabled,
                onUiEvent = onUiEvent
            )

        IdentityInvitationDirection.OUTGOING ->
            OutgoingInvitationItem(
                invitation = invitation,
                isProcessing = isProcessing,
                profilePictureBytes = profilePictureBytes,
                actionsEnabled = actionsEnabled,
                onUiEvent = onUiEvent
            )
    }
}

@Composable
private fun IncomingInvitationItem(
    invitation: ContactInvitation,
    isProcessing: Boolean,
    profilePictureBytes: ByteArray?,
    actionsEnabled: Boolean,
    onUiEvent: (ContactInvitationUiEvent) -> Unit
) {
    SparrowSwipeRevealItem(
        enabled = actionsEnabled,
        actions =
            listOf(
                SwipeRevealAction(
                    backgroundColor = MaterialTheme.colorScheme.tertiaryContainer,
                    contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
                    onClick = {
                        onUiEvent(ContactInvitationUiEvent.AcceptClicked(invitation.invitationId))
                    }
                ) {
                    InvitationSwipeActionContent(
                        icon = Icons.Default.Check,
                        label = stringResource(Res.string.feature_contacts_accept_invitation)
                    )
                },
                SwipeRevealAction(
                    backgroundColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                    contentColor = MaterialTheme.colorScheme.onSurface,
                    onClick = {
                        onUiEvent(ContactInvitationUiEvent.DeclineClicked(invitation.invitationId))
                    }
                ) {
                    InvitationSwipeActionContent(
                        icon = Icons.Default.Close,
                        label = stringResource(Res.string.feature_contacts_decline_invitation)
                    )
                },
                SwipeRevealAction(
                    backgroundColor = MaterialTheme.colorScheme.error,
                    contentColor = MaterialTheme.colorScheme.onError,
                    onClick = {
                        onUiEvent(ContactInvitationUiEvent.DeclineAndBlockClicked(invitation.invitationId))
                    }
                ) {
                    InvitationSwipeActionContent(
                        icon = Icons.Default.Block,
                        label = stringResource(Res.string.feature_contacts_block_invitation)
                    )
                }
            )
    ) {
        InvitationRow(
            invitation = invitation,
            isProcessing = isProcessing,
            profilePictureBytes = profilePictureBytes
        )
        InvitationDivider()
    }
}

@Composable
private fun OutgoingInvitationItem(
    invitation: ContactInvitation,
    isProcessing: Boolean,
    profilePictureBytes: ByteArray?,
    actionsEnabled: Boolean,
    onUiEvent: (ContactInvitationUiEvent) -> Unit
) {
    if (invitation.status == ContactInvitationStatus.DECLINED) {
        SparrowSwipeRevealItem(
            enabled = actionsEnabled,
            actions =
                listOf(
                    SwipeRevealAction(
                        backgroundColor = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onError,
                        onClick = {
                            onUiEvent(
                                ContactInvitationUiEvent.DeleteDeclinedOutgoingClicked(
                                    invitation.invitationId
                                )
                            )
                        }
                    ) {
                        InvitationSwipeActionContent(
                            icon = Icons.Default.DeleteOutline,
                            label = stringResource(Res.string.feature_contacts_delete_outgoing_invitation)
                        )
                    }
                )
        ) {
            InvitationRow(
                invitation = invitation,
                isProcessing = isProcessing,
                profilePictureBytes = profilePictureBytes
            )
            InvitationDivider()
        }
    } else {
        InvitationRow(
            invitation = invitation,
            isProcessing = isProcessing,
            profilePictureBytes = profilePictureBytes
        )
        InvitationDivider()
    }
}

@Composable
private fun InvitationDivider() {
    HorizontalDivider(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(start = MaterialTheme.spacing.listDividerStart),
        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = Alpha.itemDivider)
    )
}

@Composable
private fun EmptyInvitations(
    selectedTab: ContactInvitationTab,
    modifier: Modifier = Modifier
) {
    val message =
        when (selectedTab) {
            ContactInvitationTab.INCOMING -> Res.string.feature_contacts_invitations_incoming_empty
            ContactInvitationTab.OUTGOING -> Res.string.feature_contacts_invitations_outgoing_empty
        }

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Outlined.MarkEmailUnread,
            contentDescription = null,
            modifier = Modifier.size(Dimens.ContactInvitationsScreen.avatarSize),
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = stringResource(message),
            modifier = Modifier.padding(top = MaterialTheme.spacing.small),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun InvitationRow(
    invitation: ContactInvitation,
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
                color = MaterialTheme.colorScheme.onBackground
            )
        },
        supportingContent = {
            Column {
                invitation.contactPhoneNumber
                    ?.takeIf { invitation.contactName != null }
                    ?.let { phoneNumber ->
                        Text(
                            text = phoneNumber,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                if (invitation.direction == IdentityInvitationDirection.OUTGOING) {
                    InvitationStatus(invitation.status)
                }
            }
        },
        trailingContent = {
            if (isProcessing) {
                CircularProgressIndicator(
                    modifier = Modifier.size(Dimens.ContactInvitationsScreen.progressSize),
                    strokeWidth = Dimens.Base.progressIndicatorStrokeWidth
                )
            }
        },
        colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.background)
    )
}

@Composable
private fun InvitationStatus(status: ContactInvitationStatus) {
    val presentation = statusPresentation(status)
    Text(
        text = stringResource(presentation.label),
        modifier = Modifier.padding(top = MaterialTheme.spacing.micro),
        style = MaterialTheme.typography.labelSmall,
        color = presentation.color
    )
}

@Composable
private fun statusPresentation(status: ContactInvitationStatus): InvitationStatusPresentation =
    when (status) {
        ContactInvitationStatus.PENDING ->
            InvitationStatusPresentation(
                label = Res.string.feature_contacts_invitation_status_pending,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

        ContactInvitationStatus.DECLINED ->
            InvitationStatusPresentation(
                label = Res.string.feature_contacts_invitation_status_declined,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

        ContactInvitationStatus.EXPIRED ->
            InvitationStatusPresentation(
                label = Res.string.feature_contacts_invitation_status_expired,
                color = MaterialTheme.colorScheme.secondary
            )

        ContactInvitationStatus.FAILED ->
            InvitationStatusPresentation(
                label = Res.string.feature_contacts_invitation_status_failed,
                color = MaterialTheme.colorScheme.error
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
            modifier = Modifier.size(Dimens.ContactInvitationsScreen.actionIconSize)
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            maxLines = 1
        )
    }
}

private data class InvitationStatusPresentation(
    val label: StringResource,
    val color: Color
)

@Preview
@Composable
private fun ContactInvitationsScreenPreview() {
    SparrowTheme {
        ContactInvitationsScreen(
            uiState =
                ContactInvitationUiState(
                    selectedTab = ContactInvitationTab.OUTGOING,
                    outgoingInvitations =
                        listOf(
                            ContactInvitation(
                                invitationId = "pending",
                                contactId = "alice",
                                contactName = "Alice",
                                contactPhoneNumber = "+49 123 456",
                                direction = IdentityInvitationDirection.OUTGOING,
                                status = ContactInvitationStatus.PENDING,
                                expiresAtEpochMilliseconds = Long.MAX_VALUE,
                                updatedAtEpochMilliseconds = 1,
                                hasUnreadUpdate = false
                            ),
                            ContactInvitation(
                                invitationId = "declined",
                                contactId = "bob",
                                contactName = "Bob",
                                contactPhoneNumber = null,
                                direction = IdentityInvitationDirection.OUTGOING,
                                status = ContactInvitationStatus.DECLINED,
                                expiresAtEpochMilliseconds = Long.MAX_VALUE,
                                updatedAtEpochMilliseconds = 2,
                                hasUnreadUpdate = true
                            )
                        )
                ),
            snackbarHostState = SnackbarHostState(),
            onUiEvent = {}
        )
    }
}
