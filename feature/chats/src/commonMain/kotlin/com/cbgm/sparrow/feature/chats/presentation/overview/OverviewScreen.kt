package com.cbgm.sparrow.feature.chats.presentation.overview

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Reply
import androidx.compose.material.icons.filled.ChatBubbleOutline
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import com.cbgm.sparrow.core.ui.component.SparrowSwipeRevealItem
import com.cbgm.sparrow.core.ui.component.SwipeRevealAction
import com.cbgm.sparrow.core.ui.theme.Alpha
import com.cbgm.sparrow.core.ui.theme.Dimens
import com.cbgm.sparrow.core.ui.theme.SparrowTheme
import com.cbgm.sparrow.core.ui.theme.circle
import com.cbgm.sparrow.core.ui.theme.spacing
import com.cbgm.sparrow.feature.avatar.domain.model.AvatarTarget
import com.cbgm.sparrow.feature.avatar.presentation.component.SparrowAvatar
import com.cbgm.sparrow.feature.chats.presentation.common.history.component.ScrollToBottomButton
import com.cbgm.sparrow.feature.chats.presentation.overview.model.ConversationListItem
import com.cbgm.sparrow.feature.chats.presentation.overview.model.OverviewUiEvent
import com.cbgm.sparrow.feature.chats.presentation.overview.model.OverviewUiState
import com.cbgm.sparrow.resources.Res
import com.cbgm.sparrow.resources.feature_chats_attachment
import com.cbgm.sparrow.resources.feature_chats_no_conversations_hint
import com.cbgm.sparrow.resources.feature_chats_no_conversations_yet
import com.cbgm.sparrow.resources.feature_chats_no_messages_yet
import kotlinx.coroutines.delay
import org.jetbrains.compose.resources.stringResource
import kotlin.time.Duration.Companion.milliseconds

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
    // Short Room startup loads should not flash a spinner immediately after the
    // native splash. This only delays the indicator, never data or navigation.
    var showLoadingIndicator by remember { mutableStateOf(false) }
    LaunchedEffect(uiState.isLoading) {
        showLoadingIndicator = false
        if (uiState.isLoading) {
            delay(400L.milliseconds)
            showLoadingIndicator = true
        }
    }

    when {
        uiState.isLoading ->
            Box(
                modifier = modifier.fillMaxSize().padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                if (showLoadingIndicator) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.onBackground)
                }
            }

        uiState.conversations.isEmpty() ->
            Box(modifier = modifier.fillMaxSize().padding(innerPadding)) {
                EmptyContent(modifier = Modifier.fillMaxSize())
                uiState.activeAutoReplyName?.let { name ->
                    ActiveAutoReplyChip(
                        name = name,
                        onClick = { onUiEvent(OverviewUiEvent.AutoReplyClicked) },
                        modifier =
                            Modifier
                                .align(Alignment.TopCenter)
                                .padding(
                                    horizontal = MaterialTheme.spacing.screenPadding,
                                    vertical = MaterialTheme.spacing.base
                                )
                    )
                }
            }

        else ->
            Box(modifier = modifier.fillMaxSize()) {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = innerPadding,
                    state = listState
                ) {
                    uiState.activeAutoReplyName?.let { name ->
                        item(key = "active-auto-reply") {
                            Box(
                                modifier =
                                    Modifier
                                        .fillMaxWidth()
                                        .padding(
                                            horizontal = MaterialTheme.spacing.screenPadding,
                                            vertical = MaterialTheme.spacing.base
                                        ),
                                contentAlignment = Alignment.Center
                            ) {
                                ActiveAutoReplyChip(
                                    name = name,
                                    onClick = { onUiEvent(OverviewUiEvent.AutoReplyClicked) }
                                )
                            }
                        }
                    }

                    items(
                        items = uiState.conversations,
                        key = { conversation -> conversation.conversationId }
                    ) { conversation ->
                        SparrowSwipeRevealItem(
                            modifier = Modifier.fillMaxWidth(),
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
                                            contentDescription = null
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

                ScrollToBottomButton(
                    listState = listState,
                    reverseLayout = false,
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(
                            end = MaterialTheme.spacing.base,
                            bottom = innerPadding.calculateBottomPadding() + MaterialTheme.spacing.base
                        )
                )
            }
    }
}

@Composable
private fun ActiveAutoReplyChip(
    name: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        modifier = modifier,
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.background,
        contentColor = MaterialTheme.colorScheme.primary,
        border =
            BorderStroke(
                width = Dimens.Button.borderWidth,
                color = MaterialTheme.colorScheme.primary
            )
    ) {
        Row(
            modifier =
                Modifier.padding(
                    horizontal = MaterialTheme.spacing.actionItem.horizontalPadding,
                    vertical = MaterialTheme.spacing.base
                ),
            horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.base),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.Reply,
                contentDescription = null,
                modifier = Modifier.size(Dimens.SearchField.searchIconSize)
            )

            Text(
                text = name,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Medium
            )
        }
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
                    target = conversation.avatarTarget
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
                        when {
                            conversation.lastMessage.isNotBlank() -> conversation.lastMessage
                            !conversation.isGroup && conversation.hasMessages ->
                                stringResource(Res.string.feature_chats_attachment)
                            else -> stringResource(Res.string.feature_chats_no_messages_yet)
                        },
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
            modifier = Modifier.padding(MaterialTheme.spacing.base.div(2)),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = Alpha.OpaqueText),
            textAlign = TextAlign.Center
        )
    }
}

@Preview
@Composable
private fun OverviewScreenPreview() {
    SparrowTheme {
        OverviewScreen(
            uiState =
                OverviewUiState(
                    conversations =
                        listOf(
                            ConversationListItem(
                                contactId = "1",
                                contactName = "Alice",
                                avatarTarget = AvatarTarget.User("1"),
                                lastMessage = "Hello!",
                                timestamp = "10:00 AM",
                                unreadCount = 3,
                                conversationId = "5"
                            ),
                            ConversationListItem(
                                contactId = "2",
                                contactName = "Bob",
                                avatarTarget = AvatarTarget.User("2"),
                                lastMessage = "Sounds good, see you then.",
                                timestamp = "Yesterday",
                                conversationId = "6"
                            )
                        ),
                    activeAutoReplyName = "Vacation"
                ),
            onUiEvent = {},
            listState = LazyListState(),
            innerPadding = PaddingValues()
        )
    }
}
