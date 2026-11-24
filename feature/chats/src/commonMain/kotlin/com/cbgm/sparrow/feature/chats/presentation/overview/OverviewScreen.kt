package com.cbgm.sparrow.feature.chats.presentation.overview

import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Reply
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import com.cbgm.sparrow.core.ui.component.SparrowBanner
import com.cbgm.sparrow.core.ui.component.SparrowSwipeRevealItem
import com.cbgm.sparrow.core.ui.component.SwipeActionTone
import com.cbgm.sparrow.core.ui.component.SwipeRevealAction
import com.cbgm.sparrow.core.ui.icon.SparrowIcons
import com.cbgm.sparrow.core.ui.theme.Dimens
import com.cbgm.sparrow.core.ui.theme.SparrowTheme
import com.cbgm.sparrow.core.ui.theme.circle
import com.cbgm.sparrow.core.ui.theme.divider
import com.cbgm.sparrow.core.ui.theme.spacing
import com.cbgm.sparrow.feature.avatar.domain.model.AvatarTarget
import com.cbgm.sparrow.feature.avatar.presentation.component.SparrowAvatar
import com.cbgm.sparrow.feature.chats.presentation.common.history.component.ScrollToBottomButton
import com.cbgm.sparrow.feature.chats.presentation.component.RouteWaitingBanner
import com.cbgm.sparrow.feature.chats.presentation.overview.model.ConversationListItemUi
import com.cbgm.sparrow.feature.chats.presentation.overview.model.LastMessagePreviewUi
import com.cbgm.sparrow.feature.chats.presentation.overview.model.OverviewUiEvent
import com.cbgm.sparrow.feature.chats.presentation.overview.model.OverviewUiState
import com.cbgm.sparrow.resources.Res
import com.cbgm.sparrow.resources.feature_attachments_media
import com.cbgm.sparrow.resources.feature_auto_reply
import com.cbgm.sparrow.resources.feature_chats_attachment
import com.cbgm.sparrow.resources.feature_chats_contact_card
import com.cbgm.sparrow.resources.feature_chats_location
import com.cbgm.sparrow.resources.feature_chats_no_conversations_hint
import com.cbgm.sparrow.resources.feature_chats_no_conversations_yet
import com.cbgm.sparrow.resources.feature_chats_no_messages_yet
import com.cbgm.sparrow.resources.feature_chats_voice_message
import com.cbgm.sparrow.resources.feature_polls_label
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
                Column(
                    modifier = Modifier.align(Alignment.TopCenter),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    if (uiState.waitingForRoute) {
                        RouteWaitingBanner(modifier = Modifier.padding(horizontal = MaterialTheme.spacing.micro))
                    }
                    uiState.activeAutoReplyName?.let { name ->
                        ActiveAutoReplyBanner(
                            name = name,
                            onClick = { onUiEvent(OverviewUiEvent.AutoReplyClicked) },
                            modifier =
                                Modifier.padding(
                                    horizontal = MaterialTheme.spacing.screenPadding,
                                    vertical = MaterialTheme.spacing.micro
                                )
                        )
                    }
                }
            }

        else ->
            Box(modifier = modifier.fillMaxSize()) {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = innerPadding,
                    state = listState
                ) {
                    if (uiState.waitingForRoute) {
                        item(key = "route-waiting") {
                            RouteWaitingBanner(modifier = Modifier.padding(horizontal = MaterialTheme.spacing.micro))
                        }
                    }
                    uiState.activeAutoReplyName?.let { name ->
                        item(key = "active-auto-reply") {
                            ActiveAutoReplyBanner(
                                name = name,
                                onClick = { onUiEvent(OverviewUiEvent.AutoReplyClicked) },
                                modifier =
                                    Modifier.padding(
                                        horizontal = MaterialTheme.spacing.screenPadding,
                                        vertical = MaterialTheme.spacing.micro
                                    )
                            )
                        }
                    }

                    itemsIndexed(
                        items = uiState.conversations,
                        key = { _, conversation -> conversation.conversationId }
                    ) { index, conversation ->
                        SparrowSwipeRevealItem(
                            modifier = Modifier.fillMaxWidth(),
                            actions =
                                listOf(
                                    SwipeRevealAction(
                                        tone = SwipeActionTone.DESTRUCTIVE,
                                        onClick = {
                                            onUiEvent(
                                                OverviewUiEvent.DeleteConversation(
                                                    conversation.conversationId
                                                )
                                            )
                                        }
                                    ) {
                                        Icon(
                                            imageVector = SparrowIcons.Delete,
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

                        if (index < uiState.conversations.lastIndex) {
                            HorizontalDivider(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(start = MaterialTheme.spacing.listDividerStart),
                                thickness = Dimens.Base.dividerThickness,
                                color = MaterialTheme.colorScheme.divider
                            )
                        }
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
private fun ActiveAutoReplyBanner(
    name: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    SparrowBanner(
        icon = Icons.AutoMirrored.Filled.Reply,
        accent = MaterialTheme.colorScheme.primary,
        title = stringResource(Res.string.feature_auto_reply),
        description = name,
        onClick = onClick,
        modifier = modifier
    )
}

@Composable
private fun ConversationItem(
    conversation: ConversationListItemUi,
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
                    target = conversation.avatarTarget,
                    ring = conversation.avatarRing
                )
            },
            headlineContent = {
                Text(
                    text = conversation.contactName,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onBackground
                )
            },
            supportingContent = {
                Text(
                    text =
                        when {
                            conversation.lastMessagePreview == LastMessagePreviewUi.POLL ->
                                stringResource(Res.string.feature_polls_label)
                            conversation.lastMessage.isNotBlank() -> conversation.lastMessage
                            conversation.lastMessagePreview == LastMessagePreviewUi.MEDIA ->
                                stringResource(Res.string.feature_attachments_media)
                            conversation.lastMessagePreview == LastMessagePreviewUi.LOCATION ->
                                stringResource(Res.string.feature_chats_location)
                            conversation.lastMessagePreview == LastMessagePreviewUi.CONTACT_CARD ->
                                stringResource(Res.string.feature_chats_contact_card)
                            conversation.lastMessagePreview == LastMessagePreviewUi.VOICE ->
                                stringResource(Res.string.feature_chats_voice_message)
                            conversation.hasMessages -> stringResource(Res.string.feature_chats_attachment)
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
                        }
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
        EmptyCircuitGraphic()

        Text(
            text = stringResource(Res.string.feature_chats_no_conversations_yet),
            modifier = Modifier.padding(top = MaterialTheme.spacing.large),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onBackground
        )

        Text(
            text = stringResource(Res.string.feature_chats_no_conversations_hint),
            modifier = Modifier.padding(MaterialTheme.spacing.micro),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}

/** You, a dotted path and a contact that is not there yet. */
@Composable
private fun EmptyCircuitGraphic() {
    val you = MaterialTheme.colorScheme.onBackground
    val path = MaterialTheme.colorScheme.outline
    Canvas(
        modifier = Modifier.size(
            width = Dimens.OverviewScreen.emptyGraphicWidth,
            height = Dimens.OverviewScreen.emptyGraphicHeight
        )
    ) {
        val centerY = size.height / 2f
        val dotRadius = Dimens.OverviewScreen.emptyGraphicDotRadius.toPx()
        val contactRadius = Dimens.OverviewScreen.emptyGraphicContactRadius.toPx()
        val stroke = Dimens.OverviewScreen.emptyGraphicStroke.toPx()
        val dash = PathEffect.dashPathEffect(floatArrayOf(stroke, stroke * 4f))
        drawCircle(color = you, radius = dotRadius, center = Offset(dotRadius, centerY))
        drawLine(
            color = path,
            start = Offset(dotRadius * 3f, centerY),
            end = Offset(size.width - contactRadius * 2f, centerY),
            strokeWidth = stroke,
            pathEffect = dash
        )
        drawCircle(
            color = path,
            radius = contactRadius,
            center = Offset(size.width - contactRadius - stroke, centerY),
            style = Stroke(width = stroke, pathEffect = PathEffect.dashPathEffect(floatArrayOf(stroke * 2f, stroke * 2f)))
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
                            ConversationListItemUi(
                                contactId = "1",
                                contactName = "Alice",
                                avatarTarget = AvatarTarget.User("1"),
                                lastMessage = "Hello!",
                                timestamp = "10:00 AM",
                                unreadCount = 3,
                                conversationId = "5"
                            ),
                            ConversationListItemUi(
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
