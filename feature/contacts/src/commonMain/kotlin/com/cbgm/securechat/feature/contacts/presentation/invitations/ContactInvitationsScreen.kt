package com.cbgm.securechat.feature.contacts.presentation.invitations

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.cbgm.securechat.core.ui.component.ContactAvatar
import com.cbgm.securechat.core.ui.component.SecureChatLazyScaffold
import com.cbgm.securechat.core.ui.theme.spacing
import com.cbgm.securechat.feature.contacts.domain.model.PendingContactInvitation
import com.cbgm.securechat.feature.contacts.presentation.invitations.model.ContactInvitationUiEvent
import com.cbgm.securechat.resources.Res
import com.cbgm.securechat.resources.base_unknown
import com.cbgm.securechat.resources.feature_contacts_accept_invitation
import com.cbgm.securechat.resources.feature_contacts_block_invitation
import com.cbgm.securechat.resources.feature_contacts_close_invitations
import com.cbgm.securechat.resources.feature_contacts_decline_invitation
import com.cbgm.securechat.resources.feature_contacts_invitations_empty
import com.cbgm.securechat.resources.feature_contacts_invitations_title
import org.jetbrains.compose.resources.stringResource
import kotlin.math.roundToInt

private val invitationActionWidth = 80.dp
private const val INVITATION_ACTION_COUNT = 3
private const val REVEAL_THRESHOLD_FRACTION = 0.5f

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContactInvitationsScreen(
    invitations: List<PendingContactInvitation>,
    processingInvitationId: String?,
    snackbarHostState: SnackbarHostState,
    onUiEvent: (ContactInvitationUiEvent) -> Unit,
    modifier: Modifier = Modifier
) {
    SecureChatLazyScaffold(
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
        if (invitations.isEmpty()) {
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
                    items = invitations,
                    key = PendingContactInvitation::invitationId
                ) { invitation ->
                    SwipeRevealInvitationActions(
                        actionsEnabled = processingInvitationId == null,
                        onAccept = {
                            onUiEvent(ContactInvitationUiEvent.AcceptClicked(invitation.invitationId))
                        },
                        onDecline = {
                            onUiEvent(ContactInvitationUiEvent.DeclineClicked(invitation.invitationId))
                        },
                        onBlock = {
                            onUiEvent(
                                ContactInvitationUiEvent.DeclineAndBlockClicked(invitation.invitationId)
                            )
                        }
                    ) {
                        InvitationRow(
                            invitation = invitation,
                            isProcessing = processingInvitationId == invitation.invitationId
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
    modifier: Modifier = Modifier
) {
    val displayName =
        invitation.contactName
            ?: invitation.contactPhoneNumber
            ?: stringResource(Res.string.base_unknown)

    ListItem(
        modifier = modifier.fillMaxWidth(),
        leadingContent = {
            ContactAvatar(name = displayName)
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
private fun SwipeRevealInvitationActions(
    actionsEnabled: Boolean,
    onAccept: () -> Unit,
    onDecline: () -> Unit,
    onBlock: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val density = LocalDensity.current
    val revealWidth = invitationActionWidth * INVITATION_ACTION_COUNT
    val revealWidthPx = with(density) { revealWidth.toPx() }
    var offset by remember { mutableFloatStateOf(0f) }

    Box(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.matchParentSize(),
            horizontalArrangement = Arrangement.End
        ) {
            InvitationAction(
                label = stringResource(Res.string.feature_contacts_accept_invitation),
                backgroundColor = MaterialTheme.colorScheme.secondaryContainer,
                contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                enabled = actionsEnabled,
                onClick = {
                    offset = 0f
                    onAccept()
                }
            ) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    modifier = Modifier.size(24.dp),
                    tint = MaterialTheme.colorScheme.onSecondaryContainer
                )
            }
            InvitationAction(
                label = stringResource(Res.string.feature_contacts_decline_invitation),
                backgroundColor = MaterialTheme.colorScheme.surfaceVariant,
                contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                enabled = actionsEnabled,
                onClick = {
                    offset = 0f
                    onDecline()
                }
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = null,
                    modifier = Modifier.size(24.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            InvitationAction(
                label = stringResource(Res.string.feature_contacts_block_invitation),
                backgroundColor = MaterialTheme.colorScheme.error,
                contentColor = MaterialTheme.colorScheme.onError,
                enabled = actionsEnabled,
                onClick = {
                    offset = 0f
                    onBlock()
                }
            ) {
                Icon(
                    imageVector = Icons.Default.Block,
                    contentDescription = null,
                    modifier = Modifier.size(24.dp),
                    tint = MaterialTheme.colorScheme.onError
                )
            }
        }

        Box(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .offset { IntOffset(offset.roundToInt(), 0) }
                    .draggable(
                        enabled = actionsEnabled,
                        orientation = Orientation.Horizontal,
                        state =
                            rememberDraggableState { delta ->
                                offset = (offset + delta).coerceIn(-revealWidthPx, 0f)
                            },
                        onDragStopped = {
                            offset =
                                if (offset <= -revealWidthPx * REVEAL_THRESHOLD_FRACTION) {
                                    -revealWidthPx
                                } else {
                                    0f
                                }
                        }
                    )
        ) {
            content()
        }
    }
}

@Composable
private fun InvitationAction(
    label: String,
    backgroundColor: Color,
    contentColor: Color,
    enabled: Boolean,
    onClick: () -> Unit,
    icon: @Composable () -> Unit
) {
    Column(
        modifier =
            Modifier
                .width(invitationActionWidth)
                .fillMaxHeight()
                .background(backgroundColor)
                .clickable(
                    enabled = enabled,
                    onClick = onClick
                ),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier.size(32.dp),
            contentAlignment = Alignment.Center
        ) {
            icon()
        }
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = contentColor,
            maxLines = 1
        )
    }
}
