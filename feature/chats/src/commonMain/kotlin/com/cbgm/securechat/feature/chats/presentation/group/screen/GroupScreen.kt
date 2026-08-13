package com.cbgm.securechat.feature.chats.presentation.group.screen

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.PersonRemove
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.cbgm.securechat.core.ui.component.PatternBackground
import com.cbgm.securechat.core.ui.component.SecureChatBannerButton
import com.cbgm.securechat.core.ui.component.SecureChatLazyScaffold
import com.cbgm.securechat.core.ui.theme.SecureChatTheme
import com.cbgm.securechat.core.ui.theme.spacing
import com.cbgm.securechat.feature.chats.domain.model.MessageContentStatus
import com.cbgm.securechat.feature.chats.domain.model.MessageDeliveryStatus
import com.cbgm.securechat.feature.chats.domain.model.MessageSecurity
import com.cbgm.securechat.feature.chats.domain.model.group.ChatMessageType
import com.cbgm.securechat.feature.chats.domain.model.group.GroupConversationState
import com.cbgm.securechat.feature.chats.domain.model.group.GroupMemberInvitationStatus
import com.cbgm.securechat.feature.chats.presentation.component.MessageBubble
import com.cbgm.securechat.feature.chats.presentation.component.MessageInput
import com.cbgm.securechat.feature.chats.presentation.component.model.MessageBubbleModel
import com.cbgm.securechat.feature.chats.presentation.group.model.GroupMessageUiModel
import com.cbgm.securechat.feature.chats.presentation.group.model.GroupUiEvent
import com.cbgm.securechat.feature.chats.presentation.group.model.GroupUiState
import com.cbgm.securechat.resources.Res
import com.cbgm.securechat.resources.feature_chats_chat_typing
import com.cbgm.securechat.resources.feature_chats_group_accept
import com.cbgm.securechat.resources.feature_chats_group_decline
import com.cbgm.securechat.resources.feature_chats_group_deleted_description
import com.cbgm.securechat.resources.feature_chats_group_deleted_status
import com.cbgm.securechat.resources.feature_chats_group_deleted_title
import com.cbgm.securechat.resources.feature_chats_group_invitation_description
import com.cbgm.securechat.resources.feature_chats_group_invitation_title
import com.cbgm.securechat.resources.feature_chats_group_leaving_hint_description
import com.cbgm.securechat.resources.feature_chats_group_leaving_hint_title
import com.cbgm.securechat.resources.feature_chats_group_member_accepted
import com.cbgm.securechat.resources.feature_chats_group_member_active
import com.cbgm.securechat.resources.feature_chats_group_member_added_message
import com.cbgm.securechat.resources.feature_chats_group_member_count
import com.cbgm.securechat.resources.feature_chats_group_member_declined
import com.cbgm.securechat.resources.feature_chats_group_member_expired
import com.cbgm.securechat.resources.feature_chats_group_member_failed
import com.cbgm.securechat.resources.feature_chats_group_member_invited
import com.cbgm.securechat.resources.feature_chats_group_member_key_sent
import com.cbgm.securechat.resources.feature_chats_group_member_left_message
import com.cbgm.securechat.resources.feature_chats_group_member_removed_message
import com.cbgm.securechat.resources.feature_chats_group_message_queued
import com.cbgm.securechat.resources.feature_chats_group_removed_hint_description
import com.cbgm.securechat.resources.feature_chats_group_removed_hint_title
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
import com.cbgm.securechat.resources.feature_chats_group_unknown_member
import com.cbgm.securechat.resources.feature_chats_group_you_left_message
import com.cbgm.securechat.resources.feature_chats_group_you_were_removed_message
import com.cbgm.securechat.resources.feature_chats_loading_chat
import com.cbgm.securechat.resources.feature_chats_no_messages_yet
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GroupScreen(
    uiState: GroupUiState,
    onUiEvent: (GroupUiEvent) -> Unit,
    modifier: Modifier = Modifier
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
            TopBar(
                uiState = uiState,
                containerColor = containerColor,
                onUiEvent = onUiEvent
            )
        },
        bottomBar = { containerColor ->
            BottomBar(
                uiState = uiState,
                containerColor = containerColor,
                onUiEvent = onUiEvent
            )
        }
    ) { innerPadding, listState ->
        Content(
            uiState = uiState,
            listState = listState,
            innerPadding = innerPadding,
            onRetryMessage = { messageId ->
                onUiEvent(GroupUiEvent.RetryMessage(messageId))
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TopBar(
    uiState: GroupUiState,
    containerColor: Color,
    onUiEvent: (GroupUiEvent) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        TopAppBar(
            colors =
                TopAppBarDefaults.topAppBarColors(
                    containerColor = containerColor,
                    scrolledContainerColor = containerColor,
                    titleContentColor = MaterialTheme.colorScheme.onBackground,
                    navigationIconContentColor = MaterialTheme.colorScheme.onBackground
                ),
            title = {
                Row(
                    modifier = Modifier.clickable { onUiEvent(GroupUiEvent.HeaderClicked) },
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Avatar()
                    Spacer(modifier = Modifier.width(MaterialTheme.spacing.small))
                    Column {
                        Text(
                            text = uiState.title,
                            style = MaterialTheme.typography.titleSmall,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = subtitle(uiState),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            },
            navigationIcon = {
                IconButton(onClick = { onUiEvent(GroupUiEvent.BackClicked) }) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = null
                    )
                }
            }
        )

        uiState.errorMessage?.let { ErrorMessage(message = it) }
        StatusHint(uiState = uiState, onUiEvent = onUiEvent)
    }
}

@Composable
private fun BottomBar(
    uiState: GroupUiState,
    containerColor: Color,
    onUiEvent: (GroupUiEvent) -> Unit
) {
    Surface(color = containerColor) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Text(
                text =
                    if (uiState.isSomeoneTyping) {
                        stringResource(
                            Res.string.feature_chats_chat_typing,
                            uiState.typingDisplayName.ifBlank { uiState.title }
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
                onValueChange = { onUiEvent(GroupUiEvent.MessageTextChanged(it)) },
                onSendClick = { onUiEvent(GroupUiEvent.SendClicked) },
                enabled = !uiState.isLoading && uiState.isMessageInputEnabled
            )
        }
    }
}

@Composable
private fun Content(
    uiState: GroupUiState,
    listState: LazyListState,
    innerPadding: PaddingValues,
    onRetryMessage: (String) -> Unit
) {
    when {
        uiState.isLoading -> LoadingContent(
            modifier = Modifier.fillMaxSize().padding(innerPadding)
        )
        uiState.messages.isEmpty() -> EmptyContent(
            title = uiState.title,
            modifier = Modifier.fillMaxSize().padding(innerPadding)
        )
        else -> MessageList(
            messages = uiState.messages,
            listState = listState,
            onRetryMessage = onRetryMessage,
            contentPadding = innerPadding
        )
    }
}

@Composable
private fun MessageList(
    messages: List<GroupMessageUiModel>,
    listState: LazyListState,
    onRetryMessage: (String) -> Unit,
    contentPadding: PaddingValues
) {
    val newestMessage = messages.firstOrNull()
    LaunchedEffect(newestMessage?.id) {
        if (newestMessage?.bubble?.isMine == true) {
            listState.animateScrollToItem(index = 0)
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        state = listState,
        reverseLayout = true,
        contentPadding =
            PaddingValues(
                start = 12.dp,
                top = contentPadding.calculateTopPadding() + MaterialTheme.spacing.small,
                end = 12.dp,
                bottom = contentPadding.calculateBottomPadding() + MaterialTheme.spacing.small
            ),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(items = messages, key = GroupMessageUiModel::id) { message ->
            if (message.type == ChatMessageType.USER) {
                MessageBubble(
                    message = message.bubble,
                    onRetryClick = { onRetryMessage(message.id) }
                )
            } else {
                MembershipSystemMessage(
                    type = message.type,
                    memberName = message.bubble.senderName
                )
            }
        }
    }
}

@Composable
private fun StatusHint(
    uiState: GroupUiState,
    onUiEvent: (GroupUiEvent) -> Unit
) {
    when {
        uiState.showInvitationActions ->
            InvitationHint(
                onAccept = { onUiEvent(GroupUiEvent.AcceptInvitation) },
                onDecline = { onUiEvent(GroupUiEvent.DeclineInvitation) }
            )
        uiState.state == GroupConversationState.DELETED -> ConversationDeletedHint()
        uiState.state == GroupConversationState.REMOVED ||
            (uiState.state == GroupConversationState.DECLINED && uiState.messages.isNotEmpty()) ->
            MembershipRemovedHint()
        uiState.state == GroupConversationState.LEAVING -> MembershipLeavingHint()
        uiState.state != GroupConversationState.READY && uiState.isMessageInputEnabled ->
            PendingMessageHint(uiState = uiState)
    }
}

@Composable
private fun PendingMessageHint(uiState: GroupUiState) {
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
            uiState.memberProgress.forEach { member ->
                Text(
                    text = "${member.displayName} · ${memberStatus(member.status)}",
                    style = MaterialTheme.typography.labelSmall
                )
            }
        }
    }
}

@Composable
private fun subtitle(uiState: GroupUiState): String =
    when (uiState.state) {
        GroupConversationState.READY ->
            stringResource(Res.string.feature_chats_group_member_count, uiState.memberCount)
        GroupConversationState.INVITED -> stringResource(Res.string.feature_chats_group_status_invited)
        GroupConversationState.JOINING -> stringResource(Res.string.feature_chats_group_status_joining)
        GroupConversationState.WAITING_FOR_MEMBERS ->
            pendingSubtitle(
                readyCount = uiState.readyMemberCount,
                pendingCount = uiState.pendingMemberCount,
                waitingResource = Res.string.feature_chats_group_status_waiting
            )
        GroupConversationState.DISTRIBUTING_KEYS ->
            pendingSubtitle(
                readyCount = uiState.readyMemberCount,
                pendingCount = uiState.pendingMemberCount,
                waitingResource = Res.string.feature_chats_group_status_distributing
            )
        GroupConversationState.LEAVING -> stringResource(Res.string.feature_chats_group_status_leaving)
        GroupConversationState.REMOVED -> stringResource(Res.string.feature_chats_group_status_removed)
        GroupConversationState.DELETED -> stringResource(Res.string.feature_chats_group_deleted_status)
        GroupConversationState.DECLINED -> stringResource(Res.string.feature_chats_group_status_declined)
        GroupConversationState.EXPIRED -> stringResource(Res.string.feature_chats_group_status_expired)
        GroupConversationState.FAILED -> stringResource(Res.string.feature_chats_group_status_failed)
    }

@Composable
private fun pendingSubtitle(
    readyCount: Int,
    pendingCount: Int,
    waitingResource: StringResource
): String =
    if (readyCount > 0) {
        stringResource(
            Res.string.feature_chats_group_status_partial,
            readyCount,
            pendingCount
        )
    } else {
        stringResource(waitingResource, pendingCount)
    }

@Composable
private fun memberStatus(status: GroupMemberInvitationStatus): String =
    when (status) {
        GroupMemberInvitationStatus.INVITED -> stringResource(Res.string.feature_chats_group_member_invited)
        GroupMemberInvitationStatus.ACCEPTED -> stringResource(Res.string.feature_chats_group_member_accepted)
        GroupMemberInvitationStatus.KEY_SENT -> stringResource(Res.string.feature_chats_group_member_key_sent)
        GroupMemberInvitationStatus.ACTIVE -> stringResource(Res.string.feature_chats_group_member_active)
        GroupMemberInvitationStatus.DECLINED -> stringResource(Res.string.feature_chats_group_member_declined)
        GroupMemberInvitationStatus.EXPIRED -> stringResource(Res.string.feature_chats_group_member_expired)
        GroupMemberInvitationStatus.FAILED -> stringResource(Res.string.feature_chats_group_member_failed)
    }

@Composable
private fun Avatar() {
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
private fun EmptyContent(
    title: String,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.padding(horizontal = MaterialTheme.spacing.large),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onBackground,
                textAlign = TextAlign.Center
            )
            Text(
                text = stringResource(Res.string.feature_chats_no_messages_yet),
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
                .padding(
                    horizontal = MaterialTheme.spacing.small,
                    vertical = MaterialTheme.spacing.base
                ),
        color = MaterialTheme.colorScheme.onErrorContainer,
        style = MaterialTheme.typography.bodySmall,
        textAlign = TextAlign.Center
    )
}

@Composable
private fun ConversationDeletedHint(modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.errorContainer,
        contentColor = MaterialTheme.colorScheme.onErrorContainer
    ) {
        Column(
            modifier = Modifier.padding(horizontal = MaterialTheme.spacing.screenPadding, vertical = MaterialTheme.spacing.small)
        ) {
            Text(
                text = stringResource(Res.string.feature_chats_group_deleted_title),
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = stringResource(Res.string.feature_chats_group_deleted_description),
                style = MaterialTheme.typography.labelMedium
            )
        }
    }
}

@Preview
@Composable
private fun ConversationDeletedHintPreview() {
    SecureChatTheme {
        ConversationDeletedHint()
    }
}

@Composable
private fun InvitationHint(
    onAccept: () -> Unit,
    onDecline: () -> Unit
) {
    Surface(
        color = MaterialTheme.colorScheme.secondaryContainer,
        contentColor = MaterialTheme.colorScheme.onSecondaryContainer
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(
                        horizontal = MaterialTheme.spacing.screenPadding,
                        vertical = MaterialTheme.spacing.small
                    )
        ) {
            Text(
                text = stringResource(Res.string.feature_chats_group_invitation_title),
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = stringResource(Res.string.feature_chats_group_invitation_description),
                style = MaterialTheme.typography.labelMedium
            )
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = MaterialTheme.spacing.base),
                horizontalArrangement = Arrangement.End
            ) {
                SecureChatBannerButton(
                    onClick = onDecline,
                    fillMaxWidth = false,
                    text = stringResource(Res.string.feature_chats_group_decline)
                )

                SecureChatBannerButton(
                    onClick = onAccept,
                    fillMaxWidth = false,
                    text = stringResource(Res.string.feature_chats_group_accept)
                )
            }
        }
    }
}

@Preview
@Composable
private fun InvitationHintPreview() {
    SecureChatTheme {
        InvitationHint(
            onAccept = {},
            onDecline = {}
        )
    }
}

@Composable
private fun MembershipLeavingHint(modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.secondaryContainer,
        contentColor = MaterialTheme.colorScheme.onSecondaryContainer
    ) {
        Column(
            modifier = Modifier.padding(horizontal = MaterialTheme.spacing.screenPadding, vertical = MaterialTheme.spacing.small)
        ) {
            Text(
                text = stringResource(Res.string.feature_chats_group_leaving_hint_title),
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = stringResource(Res.string.feature_chats_group_leaving_hint_description),
                style = MaterialTheme.typography.labelMedium
            )
        }
    }
}

@Preview
@Composable
private fun MembershipLeavingHintPreview() {
    SecureChatTheme {
        MembershipLeavingHint()
    }
}

@Composable
private fun MembershipRemovedHint(modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.errorContainer,
        contentColor = MaterialTheme.colorScheme.onErrorContainer
    ) {
        Column(
            modifier = Modifier.padding(horizontal = MaterialTheme.spacing.screenPadding, vertical = MaterialTheme.spacing.small)
        ) {
            Text(
                text = stringResource(Res.string.feature_chats_group_removed_hint_title),
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = stringResource(Res.string.feature_chats_group_removed_hint_description),
                style = MaterialTheme.typography.labelMedium
            )
        }
    }
}

@Preview
@Composable
private fun MembershipRemovedHintPreview() {
    SecureChatTheme {
        MembershipRemovedHint()
    }
}

@Composable
private fun MembershipSystemMessage(
    type: ChatMessageType,
    memberName: String?,
    modifier: Modifier = Modifier
) {
    val text = getSystemMessage(type, memberName)

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center
    ) {
        Surface(
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.88f),
            contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
            shape = MaterialTheme.shapes.small
        ) {
            Row(
                modifier =
                    Modifier.padding(
                        horizontal = MaterialTheme.spacing.small,
                        vertical = MaterialTheme.spacing.base
                    ),
                horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.small),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector =
                        if (type == ChatMessageType.GROUP_MEMBER_ADDED) {
                            Icons.Default.PersonAdd
                        } else {
                            Icons.Default.PersonRemove
                        },
                    contentDescription = null
                )
                Text(
                    text = text,
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}

@Composable
private fun getSystemMessage(
    type: ChatMessageType,
    memberName: String?
) = when (type) {
    ChatMessageType.GROUP_MEMBER_ADDED ->
        stringResource(
            Res.string.feature_chats_group_member_added_message,
            memberName?.takeIf(String::isNotBlank)
                ?: stringResource(Res.string.feature_chats_group_unknown_member)
        )

    ChatMessageType.GROUP_MEMBER_REMOVED ->
        stringResource(
            Res.string.feature_chats_group_member_removed_message,
            memberName?.takeIf(String::isNotBlank)
                ?: stringResource(Res.string.feature_chats_group_unknown_member)
        )

    ChatMessageType.LOCAL_GROUP_MEMBERSHIP_REMOVED ->
        stringResource(Res.string.feature_chats_group_you_were_removed_message)

    ChatMessageType.GROUP_MEMBER_LEFT ->
        stringResource(
            Res.string.feature_chats_group_member_left_message,
            memberName?.takeIf(String::isNotBlank)
                ?: stringResource(Res.string.feature_chats_group_unknown_member)
        )

    ChatMessageType.LOCAL_GROUP_MEMBERSHIP_LEFT ->
        stringResource(Res.string.feature_chats_group_you_left_message)

    ChatMessageType.USER -> ""
}

@Preview
@Composable
private fun MembershipSystemMessageAddedPreview() {
    SecureChatTheme {
        MembershipSystemMessage(
            type = ChatMessageType.GROUP_MEMBER_ADDED,
            memberName = "Alex"
        )
    }
}

@Preview
@Composable
private fun MembershipSystemMessagePreview() {
    SecureChatTheme {
        MembershipSystemMessage(
            type = ChatMessageType.GROUP_MEMBER_REMOVED,
            memberName = "Alex"
        )
    }
}

@Preview
@Composable
private fun MembershipSystemMessageLeftPreview() {
    SecureChatTheme {
        MembershipSystemMessage(
            type = ChatMessageType.GROUP_MEMBER_LEFT,
            memberName = "Alex"
        )
    }
}

@Preview
@Composable
private fun GroupScreenPreview() {
    SecureChatTheme {
        GroupScreen(
            uiState =
                GroupUiState(
                    title = "Family",
                    isLoading = false,
                    isMessageInputEnabled = true,
                    memberCount = 4
                ),
            onUiEvent = {}
        )
    }
}

@Preview
@Composable
private fun GroupMessagesPreview() {
    SecureChatTheme {
        GroupScreen(
            uiState =
                GroupUiState(
                    title = "Family",
                    isLoading = false,
                    isMessageInputEnabled = true,
                    memberCount = 4,
                    messages =
                        listOf(
                            GroupMessageUiModel(
                                bubble =
                                    MessageBubbleModel(
                                        id = "1",
                                        isMine = false,
                                        text = "Hello everyone",
                                        security = MessageSecurity.END_TO_END_ENCRYPTED,
                                        contentStatus = MessageContentStatus.READABLE,
                                        deliveryStatus = MessageDeliveryStatus.NOT_APPLICABLE,
                                        senderName = "Alex",
                                        senderIsInContacts = true
                                    ),
                                type = ChatMessageType.USER
                            )
                        )
                ),
            onUiEvent = {}
        )
    }
}
