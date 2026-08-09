package com.cbgm.securechat.feature.chats.presentation.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.cbgm.securechat.core.security.DirectIdentitySetupMode
import com.cbgm.securechat.core.ui.component.ContactAvatar
import com.cbgm.securechat.core.ui.component.PatternBackground
import com.cbgm.securechat.core.ui.component.SecureChatLazyScaffold
import com.cbgm.securechat.core.ui.theme.SecureChatTheme
import com.cbgm.securechat.core.ui.theme.spacing
import com.cbgm.securechat.feature.chats.domain.model.ChatMessage
import com.cbgm.securechat.feature.chats.domain.model.ChatMessageType
import com.cbgm.securechat.feature.chats.domain.model.ContactSecurityState
import com.cbgm.securechat.feature.chats.domain.model.GroupConversationState
import com.cbgm.securechat.feature.chats.domain.model.GroupMemberInvitationStatus
import com.cbgm.securechat.feature.chats.domain.model.MessageContentStatus
import com.cbgm.securechat.feature.chats.domain.model.MessageDeliveryStatus
import com.cbgm.securechat.feature.chats.domain.model.MessageSecurity
import com.cbgm.securechat.feature.chats.presentation.model.ChatUiState
import com.cbgm.securechat.feature.chats.presentation.screen.chat.component.DeliveryLabel
import com.cbgm.securechat.feature.chats.presentation.screen.chat.component.GroupConversationDeletedHint
import com.cbgm.securechat.feature.chats.presentation.screen.chat.component.GroupInvitationHint
import com.cbgm.securechat.feature.chats.presentation.screen.chat.component.GroupMembershipLeavingHint
import com.cbgm.securechat.feature.chats.presentation.screen.chat.component.GroupMembershipRemovedHint
import com.cbgm.securechat.feature.chats.presentation.screen.chat.component.GroupMembershipSystemMessage
import com.cbgm.securechat.feature.contacts.domain.model.IdentityHandshakeState
import com.cbgm.securechat.resources.Res
import com.cbgm.securechat.resources.base_verify
import com.cbgm.securechat.resources.feature_chats_chat_key_exchange_incomplete_description
import com.cbgm.securechat.resources.feature_chats_chat_key_exchange_incomplete_title
import com.cbgm.securechat.resources.feature_chats_chat_no_keys_description
import com.cbgm.securechat.resources.feature_chats_chat_one_way_keys_description
import com.cbgm.securechat.resources.feature_chats_chat_typing
import com.cbgm.securechat.resources.feature_chats_chat_unencrypted_description
import com.cbgm.securechat.resources.feature_chats_chat_unencrypted_title
import com.cbgm.securechat.resources.feature_chats_chat_unverified_description
import com.cbgm.securechat.resources.feature_chats_chat_unverified_keys_description
import com.cbgm.securechat.resources.feature_chats_chat_unverified_title
import com.cbgm.securechat.resources.feature_chats_chat_verified_by_contact_description
import com.cbgm.securechat.resources.feature_chats_chat_verified_by_contact_keys_description
import com.cbgm.securechat.resources.feature_chats_chat_verified_by_contact_title
import com.cbgm.securechat.resources.feature_chats_chat_verified_by_me_description
import com.cbgm.securechat.resources.feature_chats_chat_verified_by_me_keys_description
import com.cbgm.securechat.resources.feature_chats_chat_verified_by_me_title
import com.cbgm.securechat.resources.feature_chats_chat_verified_e2ee
import com.cbgm.securechat.resources.feature_chats_chat_verified_keys_description
import com.cbgm.securechat.resources.feature_chats_contact_invitation_declined_description
import com.cbgm.securechat.resources.feature_chats_contact_invitation_declined_title
import com.cbgm.securechat.resources.feature_chats_contact_invitation_finishing_description
import com.cbgm.securechat.resources.feature_chats_contact_invitation_finishing_title
import com.cbgm.securechat.resources.feature_chats_contact_invitation_received_description
import com.cbgm.securechat.resources.feature_chats_contact_invitation_received_title
import com.cbgm.securechat.resources.feature_chats_contact_invitation_sent_description
import com.cbgm.securechat.resources.feature_chats_contact_invitation_sent_title
import com.cbgm.securechat.resources.feature_chats_decryption_failed
import com.cbgm.securechat.resources.feature_chats_delivered
import com.cbgm.securechat.resources.feature_chats_direct_chat_reinvite_required_description
import com.cbgm.securechat.resources.feature_chats_direct_chat_reinvite_required_title
import com.cbgm.securechat.resources.feature_chats_encrypted
import com.cbgm.securechat.resources.feature_chats_failed
import com.cbgm.securechat.resources.feature_chats_group_deleted_status
import com.cbgm.securechat.resources.feature_chats_group_member_accepted
import com.cbgm.securechat.resources.feature_chats_group_member_active
import com.cbgm.securechat.resources.feature_chats_group_member_count
import com.cbgm.securechat.resources.feature_chats_group_member_declined
import com.cbgm.securechat.resources.feature_chats_group_member_expired
import com.cbgm.securechat.resources.feature_chats_group_member_failed
import com.cbgm.securechat.resources.feature_chats_group_member_invited
import com.cbgm.securechat.resources.feature_chats_group_member_key_sent
import com.cbgm.securechat.resources.feature_chats_group_message_queued
import com.cbgm.securechat.resources.feature_chats_group_status_declined
import com.cbgm.securechat.resources.feature_chats_group_status_distributing
import com.cbgm.securechat.resources.feature_chats_group_status_expired
import com.cbgm.securechat.resources.feature_chats_group_status_failed
import com.cbgm.securechat.resources.feature_chats_group_status_invited
import com.cbgm.securechat.resources.feature_chats_group_status_joining
import com.cbgm.securechat.resources.feature_chats_group_status_leaving
import com.cbgm.securechat.resources.feature_chats_group_status_partial
import com.cbgm.securechat.resources.feature_chats_group_status_removed
import com.cbgm.securechat.resources.feature_chats_group_status_waiting
import com.cbgm.securechat.resources.feature_chats_invalid_message_packet
import com.cbgm.securechat.resources.feature_chats_invalid_packet
import com.cbgm.securechat.resources.feature_chats_invalid_plaintext
import com.cbgm.securechat.resources.feature_chats_loading_chat
import com.cbgm.securechat.resources.feature_chats_manual_identity_incomplete_description
import com.cbgm.securechat.resources.feature_chats_manual_identity_incomplete_title
import com.cbgm.securechat.resources.feature_chats_manual_identity_required_description
import com.cbgm.securechat.resources.feature_chats_manual_identity_required_title
import com.cbgm.securechat.resources.feature_chats_manual_identity_setup_action
import com.cbgm.securechat.resources.feature_chats_not_encrypted
import com.cbgm.securechat.resources.feature_chats_queued
import com.cbgm.securechat.resources.feature_chats_read
import com.cbgm.securechat.resources.feature_chats_sender_not_in_contacts
import com.cbgm.securechat.resources.feature_chats_sending
import com.cbgm.securechat.resources.feature_chats_sent
import com.cbgm.securechat.resources.feature_chats_start_conversation_with
import com.cbgm.securechat.resources.feature_chats_unable_decrypt_secure_message
import com.cbgm.securechat.resources.feature_chats_unable_read_plaintext
import org.jetbrains.compose.resources.stringResource

private val Field = Color(0xFF102A46)
private val IncomingBubbleColor = Color(0xFF17324D)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    uiState: ChatUiState,
    onMessageTextChanged: (String) -> Unit,
    onSendClick: () -> Unit,
    onClickHeader: () -> Unit,
    onRetryMessage: (String) -> Unit,
    onVerifyIdentity: () -> Unit,
    onManualIdentitySetup: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    onAcceptGroupInvitation: () -> Unit = {},
    onDeclineGroupInvitation: () -> Unit = {}
) {
    SecureChatLazyScaffold(
        modifier = modifier,
        barColor = MaterialTheme.colorScheme.background,
        background = {
            PatternBackground(
                modifier = Modifier.fillMaxSize(),
                backgroundColor = MaterialTheme.colorScheme.background,
                alpha = 0.04f
            )
        },
        topBar = { containerColor ->
            ChatTopBar(
                uiState = uiState,
                containerColor = containerColor,
                onClickHeader = onClickHeader,
                onVerifyIdentity = onVerifyIdentity,
                onManualIdentitySetup = onManualIdentitySetup,
                onAcceptGroupInvitation = onAcceptGroupInvitation,
                onDeclineGroupInvitation = onDeclineGroupInvitation,
                onBack = onBack
            )
        },
        bottomBar = { containerColor ->
            ChatBottomBar(
                uiState = uiState,
                containerColor = containerColor,
                onMessageTextChanged = onMessageTextChanged,
                onSendClick = onSendClick
            )
        }
    ) { innerPadding, listState ->
        ChatContent(
            uiState = uiState,
            listState = listState,
            innerPadding = innerPadding,
            onRetryMessage = onRetryMessage
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ChatTopBar(
    uiState: ChatUiState,
    containerColor: Color,
    onClickHeader: () -> Unit,
    onVerifyIdentity: () -> Unit,
    onManualIdentitySetup: () -> Unit,
    onAcceptGroupInvitation: () -> Unit,
    onDeclineGroupInvitation: () -> Unit,
    onBack: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        TopAppBar(
            colors =
                TopAppBarDefaults.topAppBarColors(
                    containerColor = containerColor,
                    scrolledContainerColor = containerColor,
                    titleContentColor = MaterialTheme.colorScheme.onBackground,
                    navigationIconContentColor =
                        MaterialTheme.colorScheme.onBackground,
                    actionIconContentColor =
                        MaterialTheme.colorScheme.onBackground
                ),
            title = {
                Row(
                    modifier =
                        Modifier.clickable(
                            onClick = onClickHeader
                        ),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (uiState.isGroup) {
                        GroupAvatar()
                    } else {
                        ContactAvatar(
                            name = uiState.contactName,
                            size = 36.dp
                        )
                    }

                    Spacer(modifier = Modifier.width(MaterialTheme.spacing.small))

                    Column {
                        Text(
                            text = uiState.contactName,
                            style = MaterialTheme.typography.titleSmall,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )

                        val subtitle =
                            if (uiState.isGroup) {
                                groupSubtitle(uiState)
                            } else {
                                uiState.subtitle
                            }
                        if (subtitle.isNotBlank()) {
                            Text(
                                text = subtitle,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector =
                            Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = null
                    )
                }
            }
        )

        if (!uiState.isGroup) {
            SecurityBanner(
                securityState = uiState.contactSecurityState,
                identityHandshakeState = uiState.identityHandshakeState,
                directIdentitySetupMode = uiState.directIdentitySetupMode,
                isChatAuthorized = uiState.isMessageInputEnabled,
                onVerifyIdentity = onVerifyIdentity,
                onManualIdentitySetup = onManualIdentitySetup
            )
        }

        uiState.errorMessage?.let { message ->
            ErrorMessage(message = message)
        }

        if (uiState.showGroupInvitationActions) {
            GroupInvitationHint(
                onAccept = onAcceptGroupInvitation,
                onDecline = onDeclineGroupInvitation
            )
        } else if (uiState.groupState == GroupConversationState.DELETED) {
            GroupConversationDeletedHint()
        } else if (
            uiState.groupState == GroupConversationState.REMOVED ||
            (
                uiState.groupState == GroupConversationState.DECLINED &&
                    uiState.messages.isNotEmpty()
            )
        ) {
            GroupMembershipRemovedHint()
        } else if (uiState.groupState == GroupConversationState.LEAVING) {
            GroupMembershipLeavingHint()
        } else if (
            uiState.isGroup &&
            uiState.groupState != GroupConversationState.READY &&
            uiState.isMessageInputEnabled
        ) {
            PendingGroupMessageHint(uiState)
        }
    }
}

@Composable
private fun groupSubtitle(uiState: ChatUiState): String = groupStateSubtitle(uiState)

@Composable
private fun groupStateSubtitle(uiState: ChatUiState): String =
    when (uiState.groupState) {
        GroupConversationState.READY ->
            stringResource(Res.string.feature_chats_group_member_count, uiState.groupMemberCount)
        GroupConversationState.INVITED ->
            stringResource(Res.string.feature_chats_group_status_invited)
        GroupConversationState.JOINING ->
            stringResource(Res.string.feature_chats_group_status_joining)
        GroupConversationState.WAITING_FOR_MEMBERS ->
            if (uiState.groupReadyMemberCount > 0) {
                stringResource(
                    Res.string.feature_chats_group_status_partial,
                    uiState.groupReadyMemberCount,
                    uiState.groupPendingCount
                )
            } else {
                stringResource(
                    Res.string.feature_chats_group_status_waiting,
                    uiState.groupPendingCount
                )
            }
        GroupConversationState.DISTRIBUTING_KEYS ->
            if (uiState.groupReadyMemberCount > 0) {
                stringResource(
                    Res.string.feature_chats_group_status_partial,
                    uiState.groupReadyMemberCount,
                    uiState.groupPendingCount
                )
            } else {
                stringResource(
                    Res.string.feature_chats_group_status_distributing,
                    uiState.groupPendingCount
                )
            }
        GroupConversationState.LEAVING ->
            stringResource(Res.string.feature_chats_group_status_leaving)
        GroupConversationState.REMOVED ->
            stringResource(Res.string.feature_chats_group_status_removed)
        GroupConversationState.DELETED ->
            stringResource(Res.string.feature_chats_group_deleted_status)
        GroupConversationState.DECLINED ->
            stringResource(Res.string.feature_chats_group_status_declined)
        GroupConversationState.EXPIRED ->
            stringResource(Res.string.feature_chats_group_status_expired)
        GroupConversationState.FAILED ->
            stringResource(Res.string.feature_chats_group_status_failed)
    }

@Composable
private fun PendingGroupMessageHint(uiState: ChatUiState) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant,
        contentColor = MaterialTheme.colorScheme.onSurfaceVariant
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(
                        horizontal = MaterialTheme.spacing.medium,
                        vertical = MaterialTheme.spacing.small
                    )
        ) {
            Text(
                text = stringResource(Res.string.feature_chats_group_message_queued),
                style = MaterialTheme.typography.bodySmall
            )
            uiState.groupMemberProgress.forEach { member ->
                Text(
                    text = "${member.displayName} · ${groupMemberStatus(member.status)}",
                    style = MaterialTheme.typography.labelSmall
                )
            }
        }
    }
}

@Composable
private fun groupMemberStatus(status: GroupMemberInvitationStatus): String =
    when (status) {
        GroupMemberInvitationStatus.INVITED ->
            stringResource(Res.string.feature_chats_group_member_invited)
        GroupMemberInvitationStatus.ACCEPTED ->
            stringResource(Res.string.feature_chats_group_member_accepted)
        GroupMemberInvitationStatus.KEY_SENT ->
            stringResource(Res.string.feature_chats_group_member_key_sent)
        GroupMemberInvitationStatus.ACTIVE ->
            stringResource(Res.string.feature_chats_group_member_active)
        GroupMemberInvitationStatus.DECLINED ->
            stringResource(Res.string.feature_chats_group_member_declined)
        GroupMemberInvitationStatus.EXPIRED ->
            stringResource(Res.string.feature_chats_group_member_expired)
        GroupMemberInvitationStatus.FAILED ->
            stringResource(Res.string.feature_chats_group_member_failed)
    }

@Composable
private fun ChatBottomBar(
    uiState: ChatUiState,
    containerColor: Color,
    onMessageTextChanged: (String) -> Unit,
    onSendClick: () -> Unit
) {
    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .background(containerColor)
    ) {
        Text(
            text =
                if (uiState.isContactTyping) {
                    stringResource(
                        Res.string.feature_chats_chat_typing,
                        uiState.typingDisplayName.ifBlank { uiState.contactName }
                    )
                } else {
                    ""
                },
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(
                        horizontal = MaterialTheme.spacing.large,
                        vertical = MaterialTheme.spacing.base / 2
                    ),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onBackground,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )

        MessageInput(
            value = uiState.messageText,
            onValueChange = onMessageTextChanged,
            onSendClick = onSendClick,
            enabled = !uiState.isLoadingContact && uiState.isMessageInputEnabled
        )
    }
}

@Composable
private fun ChatContent(
    uiState: ChatUiState,
    listState: LazyListState,
    innerPadding: PaddingValues,
    onRetryMessage: (String) -> Unit
) {
    when {
        uiState.isLoadingContact -> {
            LoadingContent(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
            )
        }

        uiState.messages.isEmpty() -> {
            EmptyContent(
                contactName = uiState.contactName,
                securityState = uiState.contactSecurityState,
                modifier =
                    Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
            )
        }

        else -> {
            MessageList(
                messages = uiState.messages,
                listState = listState,
                onRetryMessage = onRetryMessage,
                topPadding = innerPadding.calculateTopPadding(),
                bottomPadding = innerPadding.calculateBottomPadding(),
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}

@Composable
private fun GroupAvatar() {
    Surface(
        modifier = Modifier.size(36.dp),
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.18f)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                imageVector = Icons.Default.Group,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
private fun SecurityBanner(
    securityState: ContactSecurityState,
    identityHandshakeState: IdentityHandshakeState?,
    directIdentitySetupMode: DirectIdentitySetupMode,
    isChatAuthorized: Boolean,
    onVerifyIdentity: () -> Unit,
    onManualIdentitySetup: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (securityState == ContactSecurityState.MUTUAL_KEYS_VERIFIED && isChatAuthorized) {
        VerifiedSecurityIndicator(modifier = modifier)
        return
    }

    data class CombinedState(
        val icon: ImageVector,
        val title: String,
        val description: String,
        val containerColor: Color,
        val contentColor: Color
    )

    val invitationState =
        if (directIdentitySetupMode == DirectIdentitySetupMode.AUTOMATIC_INVITATION) {
            when (identityHandshakeState) {
                IdentityHandshakeState.INVITE_SENT ->
                    CombinedState(
                        icon = Icons.Default.Schedule,
                        title = stringResource(Res.string.feature_chats_contact_invitation_sent_title),
                        description = stringResource(Res.string.feature_chats_contact_invitation_sent_description),
                        containerColor = MaterialTheme.colorScheme.secondaryContainer,
                        contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                    )

                IdentityHandshakeState.AWAITING_ACCEPTANCE ->
                    CombinedState(
                        icon = Icons.Default.Warning,
                        title = stringResource(Res.string.feature_chats_contact_invitation_received_title),
                        description = stringResource(Res.string.feature_chats_contact_invitation_received_description),
                        containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                        contentColor = MaterialTheme.colorScheme.onTertiaryContainer
                    )

                IdentityHandshakeState.ACCEPTANCE_SENT,
                IdentityHandshakeState.WAITING_FOR_READY ->
                    CombinedState(
                        icon = Icons.Default.Schedule,
                        title = stringResource(Res.string.feature_chats_contact_invitation_finishing_title),
                        description = stringResource(Res.string.feature_chats_contact_invitation_finishing_description),
                        containerColor = MaterialTheme.colorScheme.secondaryContainer,
                        contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                    )

                IdentityHandshakeState.DECLINED ->
                    CombinedState(
                        icon = Icons.Default.Warning,
                        title = stringResource(Res.string.feature_chats_contact_invitation_declined_title),
                        description = stringResource(Res.string.feature_chats_contact_invitation_declined_description),
                        containerColor = MaterialTheme.colorScheme.errorContainer,
                        contentColor = MaterialTheme.colorScheme.onErrorContainer
                    )

                IdentityHandshakeState.CONVERSATION_DELETED ->
                    CombinedState(
                        icon = Icons.Default.Warning,
                        title = stringResource(Res.string.feature_chats_direct_chat_reinvite_required_title),
                        description = stringResource(Res.string.feature_chats_direct_chat_reinvite_required_description),
                        containerColor = MaterialTheme.colorScheme.errorContainer,
                        contentColor = MaterialTheme.colorScheme.onErrorContainer
                    )

                IdentityHandshakeState.EXPIRED,
                IdentityHandshakeState.FAILED,
                null ->
                    CombinedState(
                        icon = Icons.Default.Warning,
                        title = stringResource(Res.string.feature_chats_direct_chat_reinvite_required_title),
                        description = stringResource(Res.string.feature_chats_direct_chat_reinvite_required_description),
                        containerColor = MaterialTheme.colorScheme.errorContainer,
                        contentColor = MaterialTheme.colorScheme.onErrorContainer
                    )

                IdentityHandshakeState.MUTUAL_UNVERIFIED -> null
            }
        } else {
            null
        }

    val combinedState =
        invitationState ?: when (securityState) {
            ContactSecurityState.NO_REMOTE_PUBLIC_KEYS ->
                CombinedState(
                    icon = Icons.Default.LockOpen,
                    title =
                        if (directIdentitySetupMode == DirectIdentitySetupMode.MANUAL_IDENTITY_SHARING) {
                            stringResource(Res.string.feature_chats_manual_identity_required_title)
                        } else {
                            stringResource(Res.string.feature_chats_chat_unencrypted_title)
                        },
                    description =
                        if (directIdentitySetupMode == DirectIdentitySetupMode.MANUAL_IDENTITY_SHARING) {
                            stringResource(Res.string.feature_chats_manual_identity_required_description)
                        } else {
                            stringResource(Res.string.feature_chats_chat_unencrypted_description)
                        },
                    containerColor = MaterialTheme.colorScheme.errorContainer,
                    contentColor = MaterialTheme.colorScheme.onErrorContainer
                )

            ContactSecurityState.ONE_WAY_KEYS ->
                CombinedState(
                    icon = Icons.Default.LockOpen,
                    title =
                        if (directIdentitySetupMode == DirectIdentitySetupMode.MANUAL_IDENTITY_SHARING) {
                            stringResource(Res.string.feature_chats_manual_identity_incomplete_title)
                        } else {
                            stringResource(Res.string.feature_chats_chat_key_exchange_incomplete_title)
                        },
                    description =
                        if (directIdentitySetupMode == DirectIdentitySetupMode.MANUAL_IDENTITY_SHARING) {
                            stringResource(Res.string.feature_chats_manual_identity_incomplete_description)
                        } else {
                            stringResource(Res.string.feature_chats_chat_key_exchange_incomplete_description)
                        },
                    containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.8f),
                    contentColor = MaterialTheme.colorScheme.onErrorContainer
                )

            ContactSecurityState.MUTUAL_KEYS_UNVERIFIED ->
                CombinedState(
                    icon = Icons.Default.Warning,
                    title = stringResource(Res.string.feature_chats_chat_unverified_title),
                    description = stringResource(Res.string.feature_chats_chat_unverified_description),
                    containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.6f),
                    contentColor = MaterialTheme.colorScheme.onErrorContainer
                )

            ContactSecurityState.MUTUAL_KEYS_VERIFIED_BY_ME ->
                CombinedState(
                    icon = Icons.Default.Schedule,
                    title = stringResource(Res.string.feature_chats_chat_verified_by_me_title),
                    description = stringResource(Res.string.feature_chats_chat_verified_by_me_description),
                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                )

            ContactSecurityState.MUTUAL_KEYS_VERIFIED_BY_CONTACT ->
                CombinedState(
                    icon = Icons.Default.Security,
                    title = stringResource(Res.string.feature_chats_chat_verified_by_contact_title),
                    description = stringResource(Res.string.feature_chats_chat_verified_by_contact_description),
                    containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                    contentColor = MaterialTheme.colorScheme.onTertiaryContainer
                )

            ContactSecurityState.MUTUAL_KEYS_VERIFIED ->
                CombinedState(
                    icon = Icons.Default.Warning,
                    title = stringResource(Res.string.feature_chats_direct_chat_reinvite_required_title),
                    description = stringResource(Res.string.feature_chats_direct_chat_reinvite_required_description),
                    containerColor = MaterialTheme.colorScheme.errorContainer,
                    contentColor = MaterialTheme.colorScheme.onErrorContainer
                )
        }

    Surface(
        modifier = modifier.fillMaxWidth(),
        color = combinedState.containerColor,
        contentColor = combinedState.contentColor
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = combinedState.icon,
                contentDescription = null,
                modifier = Modifier.size(16.dp)
            )

            Column(
                modifier =
                    Modifier
                        .padding(start = MaterialTheme.spacing.small)
                        .weight(1f)
            ) {
                Text(
                    text = combinedState.title,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold
                )

                Text(
                    text = combinedState.description,
                    modifier = Modifier.padding(top = 2.dp),
                    style = MaterialTheme.typography.labelSmall
                )
            }

            when {
                directIdentitySetupMode == DirectIdentitySetupMode.MANUAL_IDENTITY_SHARING &&
                    (
                        securityState == ContactSecurityState.NO_REMOTE_PUBLIC_KEYS ||
                            securityState == ContactSecurityState.ONE_WAY_KEYS
                    ) -> {
                    TextButton(onClick = onManualIdentitySetup) {
                        Text(
                            text = stringResource(Res.string.feature_chats_manual_identity_setup_action),
                            style = MaterialTheme.typography.bodySmall,
                            color = combinedState.contentColor,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                securityState == ContactSecurityState.MUTUAL_KEYS_UNVERIFIED ||
                    securityState == ContactSecurityState.MUTUAL_KEYS_VERIFIED_BY_CONTACT -> {
                    TextButton(onClick = onVerifyIdentity) {
                        Text(
                            text = stringResource(Res.string.base_verify),
                            style = MaterialTheme.typography.bodySmall,
                            color = combinedState.contentColor,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun VerifiedSecurityIndicator(modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.12f)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Default.Lock,
                contentDescription = null,
                modifier = Modifier.size(14.dp),
                tint = MaterialTheme.colorScheme.secondary
            )

            Spacer(modifier = Modifier.width(6.dp))

            Text(
                text = stringResource(Res.string.feature_chats_chat_verified_e2ee),
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.secondary
            )
        }
    }
}

@Composable
private fun MessageList(
    messages: List<ChatMessage>,
    listState: LazyListState,
    onRetryMessage: (String) -> Unit,
    topPadding: Dp,
    bottomPadding: Dp,
    modifier: Modifier = Modifier
) {
    val newestMessage = messages.firstOrNull()

    LaunchedEffect(newestMessage?.id) {
        if (newestMessage?.isMine == true) {
            listState.animateScrollToItem(index = 0)
        }
    }

    LazyColumn(
        modifier = modifier,
        state = listState,
        reverseLayout = true,
        contentPadding =
            PaddingValues(
                start = 12.dp,
                top = topPadding + MaterialTheme.spacing.small,
                end = 12.dp,
                bottom = bottomPadding + MaterialTheme.spacing.small
            ),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(
            items = messages,
            key = { message -> message.id }
        ) { message ->
            if (message.type == ChatMessageType.USER) {
                MessageBubble(
                    message = message,
                    onRetryClick = { onRetryMessage(message.id) }
                )
            } else {
                GroupMembershipSystemMessage(
                    type = message.type,
                    memberName = message.senderName
                )
            }
        }
    }
}

@Composable
private fun MessageBubble(
    message: ChatMessage,
    onRetryClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    data class MessageBubbleState(
        val text: String,
        val isContentFailed: Boolean,
        val bubbleColor: Color,
        val contentColor: Color
    )

    val bubbleState =
        when (message.contentStatus) {
            MessageContentStatus.READABLE ->
                MessageBubbleState(
                    text = message.text,
                    isContentFailed = false,
                    bubbleColor =
                        if (message.isMine) {
                            MaterialTheme.colorScheme.secondary.copy(alpha = 0.18f)
                        } else {
                            IncomingBubbleColor
                        },
                    contentColor = MaterialTheme.colorScheme.onBackground
                )

            MessageContentStatus.INVALID_PACKET ->
                MessageBubbleState(
                    text = stringResource(Res.string.feature_chats_invalid_message_packet),
                    isContentFailed = true,
                    bubbleColor = MaterialTheme.colorScheme.errorContainer,
                    contentColor = MaterialTheme.colorScheme.onErrorContainer
                )

            MessageContentStatus.INVALID_PLAINTEXT_PACKET ->
                MessageBubbleState(
                    text = stringResource(Res.string.feature_chats_unable_read_plaintext),
                    isContentFailed = true,
                    bubbleColor = MaterialTheme.colorScheme.errorContainer,
                    contentColor = MaterialTheme.colorScheme.onErrorContainer
                )

            MessageContentStatus.TRANSPORT_DECRYPTION_FAILED ->
                MessageBubbleState(
                    text = stringResource(Res.string.feature_chats_unable_decrypt_secure_message),
                    isContentFailed = true,
                    bubbleColor = MaterialTheme.colorScheme.errorContainer,
                    contentColor = MaterialTheme.colorScheme.onErrorContainer
                )
        }

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = if (message.isMine) Arrangement.End else Arrangement.Start
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(fraction = 0.78f),
            horizontalAlignment = if (message.isMine) Alignment.End else Alignment.Start
        ) {
            if (!message.isMine && !message.senderName.isNullOrBlank()) {
                val senderLabel =
                    if (message.senderIsInContacts) {
                        message.senderName
                    } else {
                        stringResource(
                            Res.string.feature_chats_sender_not_in_contacts,
                            message.senderName
                        )
                    }

                Text(
                    text = senderLabel,
                    modifier = Modifier.padding(start = 8.dp, bottom = 3.dp),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.secondary
                )
            }

            Surface(
                color = bubbleState.bubbleColor,
                contentColor = bubbleState.contentColor,
                shape =
                    RoundedCornerShape(
                        topStart = 16.dp,
                        topEnd = 16.dp,
                        bottomStart = if (message.isMine) 16.dp else 4.dp,
                        bottomEnd = if (message.isMine) 4.dp else 16.dp
                    )
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = MaterialTheme.spacing.small, vertical = MaterialTheme.spacing.base),
                    verticalAlignment = Alignment.Top
                ) {
                    if (bubbleState.isContentFailed) {
                        Icon(imageVector = Icons.Default.ErrorOutline, contentDescription = null)
                        Spacer(modifier = Modifier.width(MaterialTheme.spacing.base))
                    }

                    Text(
                        text = bubbleState.text,
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }

            MessageMetadata(
                message = message,
                onRetryClick = onRetryClick,
                modifier = Modifier.padding(top = 3.dp, start = 4.dp, end = 4.dp)
            )
        }
    }
}

@Composable
private fun MessageMetadata(
    message: ChatMessage,
    onRetryClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        MessageSecurityIndicator(message = message)

        if (message.isMine && message.deliveryStatus != MessageDeliveryStatus.NOT_APPLICABLE) {
            OutgoingDeliveryIndicator(
                deliveryStatus = message.deliveryStatus,
                onRetryClick = onRetryClick
            )

            val progress = message.deliveryProgress
            if (progress.recipientCount > 1) {
                val progressText =
                    if (progress.readCount > 0) {
                        "Read ${progress.readCount}/${progress.recipientCount}"
                    } else if (progress.deliveredCount > 0) {
                        "Delivered ${progress.deliveredCount}/${progress.recipientCount}"
                    } else {
                        "Sending…"
                    }

                Text(
                    text = progressText,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                )
            }
        }
    }
}

@Composable
private fun MessageSecurityIndicator(message: ChatMessage) {
    val text =
        when (message.contentStatus) {
            MessageContentStatus.INVALID_PACKET -> stringResource(Res.string.feature_chats_invalid_packet)
            MessageContentStatus.INVALID_PLAINTEXT_PACKET -> stringResource(Res.string.feature_chats_invalid_plaintext)
            MessageContentStatus.TRANSPORT_DECRYPTION_FAILED ->
                stringResource(Res.string.feature_chats_decryption_failed)
            MessageContentStatus.READABLE ->
                when (message.security) {
                    MessageSecurity.INSECURE -> stringResource(Res.string.feature_chats_not_encrypted)
                    MessageSecurity.END_TO_END_ENCRYPTED -> stringResource(Res.string.feature_chats_encrypted)
                }
        }

    val icon =
        when {
            message.contentStatus != MessageContentStatus.READABLE -> Icons.Default.ErrorOutline
            message.security == MessageSecurity.END_TO_END_ENCRYPTED -> Icons.Default.Lock
            else -> Icons.Default.LockOpen
        }

    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(14.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.width(MaterialTheme.spacing.base))

        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun OutgoingDeliveryIndicator(
    deliveryStatus: MessageDeliveryStatus,
    onRetryClick: () -> Unit
) {
    when (deliveryStatus) {
        MessageDeliveryStatus.NOT_APPLICABLE -> Unit

        MessageDeliveryStatus.QUEUED -> {
            DeliveryLabel(
                text = stringResource(Res.string.feature_chats_queued),
                icon = {
                    Icon(
                        imageVector = Icons.Default.Schedule,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp)
                    )
                }
            )
        }

        MessageDeliveryStatus.SENDING -> {
            DeliveryLabel(
                text = stringResource(Res.string.feature_chats_sending),
                icon = {
                    CircularProgressIndicator(
                        modifier = Modifier.size(12.dp),
                        strokeWidth = 1.5.dp
                    )
                }
            )
        }

        MessageDeliveryStatus.SENT -> {
            DeliveryLabel(
                text = stringResource(Res.string.feature_chats_sent),
                icon = {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp)
                    )
                }
            )
        }

        MessageDeliveryStatus.DELIVERED -> {
            DeliveryLabel(
                text = stringResource(Res.string.feature_chats_delivered),
                icon = {
                    Row {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp)
                        )
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp).padding(start = 1.dp)
                        )
                    }
                }
            )
        }

        MessageDeliveryStatus.READ -> {
            DeliveryLabel(
                text = stringResource(Res.string.feature_chats_read),
                textColor = MaterialTheme.colorScheme.secondary,
                icon = {
                    Row {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp),
                            tint = MaterialTheme.colorScheme.secondary
                        )
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp).padding(start = 1.dp),
                            tint = MaterialTheme.colorScheme.secondary
                        )
                    }
                }
            )
        }

        MessageDeliveryStatus.FAILED -> {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.ErrorOutline,
                    contentDescription = null,
                    modifier = Modifier.size(14.dp),
                    tint = MaterialTheme.colorScheme.error
                )

                Spacer(modifier = Modifier.width(3.dp))

                Text(
                    text = stringResource(Res.string.feature_chats_failed),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.error
                )

                IconButton(
                    onClick = onRetryClick,
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}

@Composable
private fun MessageInput(
    value: String,
    onValueChange: (String) -> Unit,
    onSendClick: () -> Unit,
    enabled: Boolean,
    modifier: Modifier = Modifier
) {
    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .windowInsetsPadding(WindowInsets.navigationBars)
                .imePadding()
                .padding(
                    start = MaterialTheme.spacing.base,
                    end = MaterialTheme.spacing.base,
                    bottom = MaterialTheme.spacing.base
                ),
        verticalAlignment = Alignment.Bottom
    ) {
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            modifier =
                Modifier
                    .weight(1f)
                    .background(
                        color = Field,
                        shape = MaterialTheme.shapes.medium
                    ).padding(
                        horizontal = MaterialTheme.spacing.small + 4.dp,
                        vertical = MaterialTheme.spacing.base
                    ),
            enabled = enabled,
            minLines = 1,
            maxLines = 5,
            textStyle =
                MaterialTheme.typography.bodyMedium.copy(
                    color = MaterialTheme.colorScheme.onBackground
                ),
            cursorBrush = SolidColor(MaterialTheme.colorScheme.secondary),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Default),
            decorationBox = { innerTextField -> innerTextField() }
        )

        IconButton(
            onClick = onSendClick,
            enabled = enabled && value.isNotBlank(),
            modifier = Modifier.padding(start = 4.dp)
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.Send,
                contentDescription = null,
                tint =
                    if (enabled && value.isNotBlank()) {
                        MaterialTheme.colorScheme.secondary
                    } else {
                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.35f)
                    }
            )
        }
    }
}

@Composable
private fun EmptyContent(
    contactName: String,
    securityState: ContactSecurityState,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.padding(horizontal = MaterialTheme.spacing.large),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = stringResource(Res.string.feature_chats_start_conversation_with, contactName),
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onBackground,
                textAlign = TextAlign.Center
            )

            Text(
                text =
                    when (securityState) {
                        ContactSecurityState.NO_REMOTE_PUBLIC_KEYS ->
                            stringResource(Res.string.feature_chats_chat_no_keys_description)

                        ContactSecurityState.ONE_WAY_KEYS ->
                            stringResource(Res.string.feature_chats_chat_one_way_keys_description)

                        ContactSecurityState.MUTUAL_KEYS_UNVERIFIED ->
                            stringResource(Res.string.feature_chats_chat_unverified_keys_description)

                        ContactSecurityState.MUTUAL_KEYS_VERIFIED_BY_ME ->
                            stringResource(Res.string.feature_chats_chat_verified_by_me_keys_description)

                        ContactSecurityState.MUTUAL_KEYS_VERIFIED_BY_CONTACT ->
                            stringResource(Res.string.feature_chats_chat_verified_by_contact_keys_description)

                        ContactSecurityState.MUTUAL_KEYS_VERIFIED ->
                            stringResource(Res.string.feature_chats_chat_verified_keys_description)
                    },
                modifier = Modifier.padding(top = MaterialTheme.spacing.base),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun LoadingContent(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        CircularProgressIndicator(color = MaterialTheme.colorScheme.secondary)

        Spacer(modifier = Modifier.height(MaterialTheme.spacing.medium))

        Text(
            text = stringResource(Res.string.feature_chats_loading_chat),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun ErrorMessage(
    message: String,
    modifier: Modifier = Modifier
) {
    Text(
        text = message,
        modifier =
            modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.errorContainer)
                .padding(
                    horizontal = MaterialTheme.spacing.small,
                    vertical = MaterialTheme.spacing.base
                ),
        color = MaterialTheme.colorScheme.onErrorContainer,
        style = MaterialTheme.typography.bodySmall,
        textAlign = TextAlign.Center
    )
}

@Preview
@Composable
private fun ChatScreenPreview() {
    SecureChatTheme {
        ChatScreen(
            uiState =
                ChatUiState(
                    isLoadingContact = false,
                    contactName = "Alex",
                    contactSecurityState = ContactSecurityState.MUTUAL_KEYS_VERIFIED,
                    errorMessage = "sfsfsjljljljljljlf"
                ),
            onMessageTextChanged = {},
            onSendClick = {},
            onClickHeader = {},
            onRetryMessage = {},
            onVerifyIdentity = {},
            onManualIdentitySetup = {},
            onBack = {}
        )
    }
}

@Preview
@Composable
private fun ChatScreenMessagesPreview() {
    SecureChatTheme {
        ChatScreen(
            uiState =
                ChatUiState(
                    isLoadingContact = false,
                    contactName = "Alex",
                    contactSecurityState = ContactSecurityState.MUTUAL_KEYS_VERIFIED,
                    errorMessage = null,
                    isContactTyping = true,
                    messages =
                        listOf(
                            ChatMessage(
                                id = "1",
                                isMine = true,
                                text = "This is a test goes very long and hopefully brearks right",
                                security = MessageSecurity.INSECURE,
                                contentStatus = MessageContentStatus.READABLE,
                                deliveryStatus = MessageDeliveryStatus.SENDING,
                                timestamp = System.currentTimeMillis(),
                                contactId = "2"
                            ),
                            ChatMessage(
                                id = "2",
                                isMine = false,
                                text = "This is a test goes very long and hopefully brearks right",
                                security = MessageSecurity.END_TO_END_ENCRYPTED,
                                contentStatus = MessageContentStatus.READABLE,
                                deliveryStatus = MessageDeliveryStatus.QUEUED,
                                timestamp = System.currentTimeMillis(),
                                contactId = "1"
                            )
                        )
                ),
            onMessageTextChanged = {},
            onSendClick = {},
            onClickHeader = {},
            onRetryMessage = {},
            onVerifyIdentity = {},
            onManualIdentitySetup = {},
            onBack = {}
        )
    }
}
