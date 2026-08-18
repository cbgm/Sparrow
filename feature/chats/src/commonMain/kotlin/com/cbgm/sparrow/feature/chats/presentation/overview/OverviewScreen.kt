package com.cbgm.sparrow.feature.chats.presentation.overview

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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChatBubbleOutline
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.cbgm.sparrow.core.ui.component.Avatar
import com.cbgm.sparrow.core.ui.theme.SparrowTheme
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
import kotlin.math.roundToInt

@Composable
fun OverviewScreen(
    uiState: OverviewUiState,
    modifier: Modifier = Modifier,
    groupAvatars: Map<String, ByteArray?> = emptyMap(),
    onUiEvent: (OverviewUiEvent) -> Unit,
    listState: LazyListState,
    innerPadding: PaddingValues
) {
    Content(
        uiState = uiState,
        groupAvatars = groupAvatars,
        onUiEvent = onUiEvent,
        listState = listState,
        innerPadding = innerPadding,
        modifier = modifier
    )
}

@Composable
private fun Content(
    uiState: OverviewUiState,
    groupAvatars: Map<String, ByteArray?>,
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
                    SwipeRevealDeleteContainer(
                        onDelete = {
                            onUiEvent(
                                OverviewUiEvent.DeleteConversation(conversation.conversationId)
                            )
                        }
                    ) {
                        ConversationItem(
                            conversation = conversation,
                            groupAvatarBytes = groupAvatars[conversation.conversationId],
                            onClick = {
                                onUiEvent(OverviewUiEvent.ChatClicked(conversation))
                            }
                        )
                        HorizontalDivider(
                            modifier = Modifier.fillMaxWidth().padding(start = 80.dp),
                            color = MaterialTheme.colorScheme.onPrimary.copy(alpha = .05f)
                        )
                    }
                }
            }

        is OverviewUiState.Error -> Unit
    }
}

@Composable
private fun ConversationItem(
    conversation: ConversationListItem,
    groupAvatarBytes: ByteArray?,
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
                Avatar(
                    name = conversation.contactName,
                    pictureBytes =
                        if (conversation.isGroup) {
                            groupAvatarBytes
                        } else {
                            conversation.profilePictureBytes
                        }
                )
            },
            headlineContent = {
                Text(
                    text = conversation.contactName,
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
                        conversation.lastMessage.takeIf { it.isNotBlank() }
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
                        text = conversation.timestamp,
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
                                text = if (conversation.unreadCount > 99) "99+" else conversation.unreadCount.toString(),
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

@Composable
private fun SwipeRevealDeleteContainer(
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val density = LocalDensity.current
    val actionWidth = 80.dp
    val actionWidthPx = with(density) { actionWidth.toPx() }
    var offset by remember { mutableFloatStateOf(0f) }

    Box(
        modifier =
            modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.error)
    ) {
        IconButton(
            onClick = onDelete,
            modifier =
                Modifier
                    .align(Alignment.CenterEnd)
                    .width(actionWidth)
        ) {
            Icon(
                imageVector = Icons.Default.DeleteOutline,
                contentDescription = stringResource(Res.string.feature_chats_delete_conversation),
                tint = MaterialTheme.colorScheme.onError,
                modifier = Modifier.size(28.dp)
            )
        }

        Box(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .offset { IntOffset(offset.roundToInt(), 0) }
                    .draggable(
                        orientation = Orientation.Horizontal,
                        state =
                            rememberDraggableState { delta ->
                                offset = (offset + delta).coerceIn(-actionWidthPx, 0f)
                            },
                        onDragStopped = {
                            offset =
                                if (offset <= -actionWidthPx / 2f) {
                                    -actionWidthPx
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

@Preview
@Composable
private fun SwipeRevealDeleteContainerPreview() {
    SparrowTheme {
        SwipeRevealDeleteContainer(onDelete = {}) {
            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.background),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Alice")
            }
        }
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
