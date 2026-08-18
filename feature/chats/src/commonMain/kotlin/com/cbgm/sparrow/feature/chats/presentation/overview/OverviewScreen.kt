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
import com.cbgm.sparrow.core.ui.component.SparrowAvatar
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
import kotlin.math.roundToInt

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
                    SwipeRevealDeleteContainer(
                        onDelete = {
                            onUiEvent(
                                OverviewUiEvent.DeleteConversation(conversation.conversationId)
                            )
                        }
                    ) {
                        ConversationItem(
                            conversation = conversation,
                            onClick = {
                                onUiEvent(OverviewUiEvent.ChatClicked(conversation))
                            }
                        )
                        HorizontalDivider(
                            modifier = Modifier.fillMaxWidth().padding(start = MaterialTheme.spacing.listDividerStart),
                            color = MaterialTheme.colorScheme.onPrimary.copy(alpha = Alpha.OverviewScreen.actionBackground)
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
                            MaterialTheme.colorScheme.onPrimary.copy(alpha = Alpha.OverviewScreen.unreadContent)
                        } else {
                            MaterialTheme.colorScheme.onPrimary.copy(alpha = Alpha.OpaqueText)
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
                                MaterialTheme.colorScheme.secondary
                            } else {
                                MaterialTheme.colorScheme.onPrimary.copy(alpha = Alpha.OpaqueText)
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
                                    .background(MaterialTheme.colorScheme.secondary, MaterialTheme.shapes.circle),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = if (conversation.unreadCount > 99) "99+" else conversation.unreadCount.toString(),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF071A2E),
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
                        MaterialTheme.colorScheme.secondary.copy(alpha = Alpha.OverviewScreen.avatarBadge),
                        MaterialTheme.shapes.circle
                    ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.ChatBubbleOutline,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.secondary,
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

@Composable
private fun SwipeRevealDeleteContainer(
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val density = LocalDensity.current
    val actionWidth = Dimens.SwipeRevealActions.actionWidth
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
                modifier = Modifier.size(Dimens.OverviewScreen.deleteIconSize)
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
