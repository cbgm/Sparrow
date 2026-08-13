package com.cbgm.securechat.feature.chats.presentation.screen.overview

/** Public conversations screen contract. */
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChatBubbleOutline
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.cbgm.securechat.core.ui.component.ContactAvatar
import com.cbgm.securechat.core.ui.theme.SecureChatTheme
import com.cbgm.securechat.core.ui.theme.spacing
import com.cbgm.securechat.feature.chats.presentation.model.ChatListItem
import com.cbgm.securechat.feature.chats.presentation.model.ChatsUiEvent
import com.cbgm.securechat.feature.chats.presentation.model.ChatsUiState
import com.cbgm.securechat.feature.chats.presentation.screen.overview.component.SwipeRevealDeleteContainer
import com.cbgm.securechat.resources.Res
import com.cbgm.securechat.resources.feature_chats_no_conversations_hint
import com.cbgm.securechat.resources.feature_chats_no_conversations_yet
import com.cbgm.securechat.resources.feature_chats_no_messages_yet
import org.jetbrains.compose.resources.stringResource

@Composable
fun ChatsScreen(
    uiState: ChatsUiState,
    onUiEvent: (ChatsUiEvent) -> Unit,
    listState: LazyListState,
    innerPadding: PaddingValues,
    modifier: Modifier = Modifier
) {
    when (uiState) {
        ChatsUiState.Loading -> {
            Box(
                modifier =
                    modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(
                    color = MaterialTheme.colorScheme.onBackground
                )
            }
        }

        ChatsUiState.Empty -> {
            EmptyContent(
                modifier =
                    modifier
                        .fillMaxSize()
                        .padding(innerPadding)
            )
        }

        is ChatsUiState.Content -> {
            LazyColumn(
                modifier = modifier.fillMaxSize(),
                contentPadding = innerPadding,
                state = listState
            ) {
                items(
                    items = uiState.conversations,
                    key = { chat -> chat.conversationId }
                ) { chat ->
                    SwipeRevealDeleteContainer(
                        onDelete = { onUiEvent(ChatsUiEvent.DeleteConversation(chat.conversationId)) }
                    ) {
                        ChatItem(
                            chat = chat,
                            onClick = {
                                onUiEvent(ChatsUiEvent.ChatClicked(chat))
                            }
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

        is ChatsUiState.Error -> {}
    }
}

@Composable
private fun ChatItem(
    chat: ChatListItem,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val hasUnread = chat.unreadCount > 0

    Column(modifier = modifier.fillMaxWidth()) {
        ListItem(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onClick),
            leadingContent = {
                ContactAvatar(chat.contactName)
            },
            headlineContent = {
                Text(
                    text = chat.contactName,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            supportingContent = {
                Text(
                    text =
                        chat.lastMessage.takeIf { it.isNotBlank() }
                            ?: stringResource(Res.string.feature_chats_no_messages_yet),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.bodySmall,
                    color =
                        if (hasUnread) {
                            MaterialTheme.colorScheme.onPrimary.copy(alpha = .9f)
                        } else {
                            MaterialTheme.colorScheme.onPrimary.copy(alpha = .74f)
                        },
                    fontWeight = if (hasUnread) FontWeight.Medium else FontWeight.Normal
                )
            },
            trailingContent = {
                Column(
                    horizontalAlignment = Alignment.End,
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = chat.timestamp,
                        style = MaterialTheme.typography.labelSmall,
                        color =
                            if (hasUnread) {
                                MaterialTheme.colorScheme.secondary
                            } else {
                                MaterialTheme.colorScheme.onPrimary.copy(alpha = .74f)
                            }
                    )

                    if (hasUnread) {
                        Box(
                            modifier =
                                Modifier
                                    .sizeIn(minWidth = 20.dp, minHeight = 20.dp)
                                    .background(MaterialTheme.colorScheme.secondary, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = if (chat.unreadCount > 99) "99+" else chat.unreadCount.toString(),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF071A2E),
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp)
                            )
                        }
                    }
                }
            },
            colors =
                ListItemDefaults.colors(
                    containerColor = MaterialTheme.colorScheme.background
                )
        )
    }
}

@Composable
private fun EmptyContent(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.padding(horizontal = MaterialTheme.spacing.screenPadding),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier =
                Modifier
                    .size(80.dp)
                    .background(
                        MaterialTheme.colorScheme.secondary.copy(alpha = 0.12f),
                        CircleShape
                    ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.ChatBubbleOutline,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.size(36.dp)
            )
        }

        Text(
            text = stringResource(Res.string.feature_chats_no_conversations_yet),
            modifier = Modifier.padding(top = MaterialTheme.spacing.medium),
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onBackground
        )

        Text(
            text = stringResource(Res.string.feature_chats_no_conversations_hint),
            modifier = Modifier.fillMaxWidth().padding(MaterialTheme.spacing.base.div(2)),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
        )
    }
}

@Preview
@Composable
private fun ChatsScreenPreview() {
    SecureChatTheme {
        ChatsScreen(
            uiState =
                ChatsUiState.Content(
                    conversations =
                        listOf(
                            ChatListItem(
                                contactId = "1",
                                contactName = "Alice",
                                lastMessage = "Hello!",
                                timestamp = "10:00 AM",
                                unreadCount = 3,
                                conversationId = "5"
                            ),
                            ChatListItem(
                                contactId = "2",
                                contactName = "Bob",
                                lastMessage = "Sounds good, see you then.",
                                timestamp = "Yesterday",
                                conversationId = "6"
                            )
                        )
                ),
            onUiEvent = {},
            listState = LazyListState(),
            innerPadding = PaddingValues()
        )
    }
}
