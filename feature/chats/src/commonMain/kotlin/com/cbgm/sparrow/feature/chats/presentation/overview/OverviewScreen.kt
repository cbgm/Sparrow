package com.cbgm.sparrow.feature.chats.presentation.overview

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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChatBubbleOutline
import androidx.compose.material.icons.filled.DeleteOutline
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import com.cbgm.sparrow.core.ui.component.SparrowAvatar
import com.cbgm.sparrow.core.ui.component.SparrowSwipeRevealItem
import com.cbgm.sparrow.core.ui.component.SwipeRevealAction
import com.cbgm.sparrow.core.ui.theme.Alpha
import com.cbgm.sparrow.core.ui.theme.Dimens
import com.cbgm.sparrow.core.ui.theme.SparrowTheme
import com.cbgm.sparrow.core.ui.theme.circle
import com.cbgm.sparrow.core.ui.theme.spacing
import com.cbgm.sparrow.feature.chats.presentation.overview.model.ConversationListItem
import com.cbgm.sparrow.feature.chats.presentation.overview.model.OverviewUiEvent
import com.cbgm.sparrow.feature.chats.presentation.overview.model.OverviewUiState
import com.cbgm.sparrow.resources.Res
import com.cbgm.sparrow.resources.feature_chats_delete_conversation
import com.cbgm.sparrow.resources.feature_chats_no_conversations_hint
import com.cbgm.sparrow.resources.feature_chats_no_conversations_yet
import com.cbgm.sparrow.resources.feature_chats_no_messages_yet
import org.jetbrains.compose.resources.stringResource

@Composable
fun OverviewScreen(
    uiState: OverviewUiState,
    onUiEvent: (OverviewUiEvent) -> Unit,
    listState: LazyListState,
    innerPadding: PaddingValues,
    modifier: Modifier = Modifier
) {
    Content(
        uiState = uiState,
        onUiEvent = onUiEvent,
        listState = listState,
        innerPadding = innerPadding,
        modifier = modifier
    )
}

@Composable
private fun Content(
    uiState: OverviewUiState,
    onUiEvent: (OverviewUiEvent) -> Unit,
    listState: LazyListState,
    innerPadding: PaddingValues,
    modifier: Modifier = Modifier
) {
    when (uiState) {
        OverviewUiState.Loading ->
            Box(
                modifier = modifier.fillMaxSize().padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.onBackground)
            }

        OverviewUiState.Empty ->
            EmptyContent(
                modifier = modifier.fillMaxSize().padding(innerPadding)
            )

        is OverviewUiState.Content ->
            LazyColumn(
                modifier = modifier.fillMaxSize(),
                contentPadding = innerPadding,
                state = listState
            ) {
                items(
                    items = uiState.conversations,
                    key = { conversation -> conversation.conversationId }
                ) { conversation ->
                    SparrowSwipeRevealItem(
                        actions =
                            listOf(
                                SwipeRevealAction(
                                    backgroundColor = MaterialTheme.colorScheme.error,
                                    contentColor = MaterialTheme.colorScheme.onError,
                                    onClick = {
                                        onUiEvent(
                                            OverviewUiEvent.DeleteConversation(
                                                conversation.conversationId
                                            )
                                        )
                                    }
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.DeleteOutline,
                                        contentDescription = stringResource(Res.string.feature_chats_delete_conversation)
                                    )
                                }
                            )
                    ) {
                        ConversationItem(
                            conversation = conversation,
                            onClick = {
                                onUiEvent(OverviewUiEvent.ChatClicked(conversation))
                            }
                        )
                    }

                    HorizontalDivider(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = MaterialTheme.spacing.listDividerStart),
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = Alpha.itemDivider)
                    )
                }
            }

        is OverviewUiState.Error -> Unit
    }
}

@Composable
private fun ConversationItem(
    conversation: ConversationListItem,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val hasUnread = conversation.unreadCount > 0

    Column(modifier = modifier.fillMaxWidth()) {
        ListItem(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onClick),
            leadingContent = {
                SparrowAvatar(
                    name = conversation.contactName,
                    pictureBytes = conversation.avatarBytes
                )
            },
            headlineContent = {
                Text(
                    text = conversation.contactName,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
            },
            supportingContent = {
                Text(
                    text =
                        conversation.lastMessage.takeIf { it.isNotBlank() }
                            ?: stringResource(Res.string.feature_chats_no_messages_yet),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.bodySmall,
                    color =
                        if (hasUnread) {
                            MaterialTheme.colorScheme.onBackground
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                    fontWeight = if (hasUnread) FontWeight.Medium else FontWeight.Normal
                )
            },
            trailingContent = {
                Column(
                    horizontalAlignment = Alignment.End,
                    verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.micro)
                ) {
                    Text(
                        text = conversation.timestamp,
                        style = MaterialTheme.typography.labelSmall,
                        color =
                            if (hasUnread) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            }
                    )

                    if (hasUnread) {
                        Box(
                            modifier =
                                Modifier
                                    .sizeIn(
                                        minWidth = Dimens.OverviewScreen.unreadBadgeMinSize,
                                        minHeight = Dimens.OverviewScreen.unreadBadgeMinSize
                                    )
                                    .background(
                                        MaterialTheme.colorScheme.primary,
                                        MaterialTheme.shapes.circle
                                    ),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = if (conversation.unreadCount > 99) "99+" else conversation.unreadCount.toString(),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.padding(
                                    horizontal = MaterialTheme.spacing.overviewScreen.unreadBadgeHorizontalPadding,
                                    vertical = MaterialTheme.spacing.overviewScreen.unreadBadgeVerticalPadding
                                )
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
                    .size(Dimens.OverviewScreen.emptyStateIconContainerSize)
                    .background(
                        MaterialTheme.colorScheme.primary.copy(alpha = Alpha.OverviewScreen.avatarBadge),
                        MaterialTheme.shapes.circle
                    ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.ChatBubbleOutline,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(Dimens.OverviewScreen.emptyStateIconSize)
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
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = Alpha.OpaqueText)
        )
    }
}

@Preview
@Composable
private fun OverviewScreenPreview() {
    SparrowTheme {
        OverviewScreen(
            uiState =
                OverviewUiState.Content(
                    conversations =
                        listOf(
                            ConversationListItem(
                                contactId = "1",
                                contactName = "Alice",
                                lastMessage = "Hello!",
                                timestamp = "10:00 AM",
                                unreadCount = 3,
                                conversationId = "5"
                            ),
                            ConversationListItem(
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
