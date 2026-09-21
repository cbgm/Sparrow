package com.cbgm.sparrow.feature.chats.presentation.common.header.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import com.cbgm.sparrow.core.time.formatMessageTimestamp
import com.cbgm.sparrow.core.ui.component.SparrowScrollScaffold
import com.cbgm.sparrow.core.ui.theme.Alpha
import com.cbgm.sparrow.core.ui.theme.Dimens
import com.cbgm.sparrow.core.ui.theme.Shapes
import com.cbgm.sparrow.core.ui.theme.SparrowTheme
import com.cbgm.sparrow.core.ui.theme.spacing
import com.cbgm.sparrow.feature.attachments.domain.model.SharedContact
import com.cbgm.sparrow.feature.chats.domain.model.MessageContentStatus
import com.cbgm.sparrow.feature.chats.domain.model.MessageDeliveryStatus
import com.cbgm.sparrow.feature.chats.domain.model.MessageSecurity
import com.cbgm.sparrow.feature.chats.presentation.common.history.component.ContactMessageBubbleBody
import com.cbgm.sparrow.feature.chats.presentation.common.history.component.FileMessageBubbleBody
import com.cbgm.sparrow.feature.chats.presentation.common.history.component.LocationMessageBubbleBody
import com.cbgm.sparrow.feature.chats.presentation.common.history.component.PhotoVideoMessageBubbleBody
import com.cbgm.sparrow.feature.chats.presentation.common.history.component.TextMessageBubbleBody
import com.cbgm.sparrow.feature.chats.presentation.common.history.model.MessageBubbleUi
import com.cbgm.sparrow.feature.chats.presentation.common.history.model.MessagePartUi
import com.cbgm.sparrow.feature.voice.domain.model.VoiceMessageTarget
import com.cbgm.sparrow.feature.voice.presentation.message.VoiceMessageContent
import com.cbgm.sparrow.resources.Res
import com.cbgm.sparrow.resources.feature_chats_attachment
import com.cbgm.sparrow.resources.feature_chats_pinned_message
import com.cbgm.sparrow.resources.ic_pin
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun GroupPinnedMessageBar(
    message: MessageBubbleUi,
    pinnedAtEpochMilliseconds: Long,
    canUnpin: Boolean,
    onClick: () -> Unit,
    onUnpinClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val fallback = stringResource(Res.string.feature_chats_attachment)
    val preview = message.pinnedPreviewText(fallback)
    val sender = message.senderName?.takeIf { !message.isMine && it.isNotBlank() }
    val pinnedTime = formatMessageTimestamp(pinnedAtEpochMilliseconds)
    val label = sender?.let { "$it \u00B7 $pinnedTime" } ?: pinnedTime

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(
                horizontal = MaterialTheme.spacing.small,
                vertical = MaterialTheme.spacing.base
            )
            .clickable(onClick = onClick),
        shape = Shapes.extraSmall,
        color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = Alpha.OpaqueBar)
    ) {
        Row(
            modifier = Modifier.padding(
                vertical = MaterialTheme.spacing.micro
            ),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = {}) {
                Icon(
                    painter = painterResource(Res.drawable.ic_pin),
                    contentDescription = null,
                    modifier = Modifier.size(Dimens.MessageBubble.iconSize),
                    tint = MaterialTheme.colorScheme.primary
                )
            }

            Column(modifier = Modifier.weight(1f).padding(end = MaterialTheme.spacing.small)) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = preview,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            if (canUnpin) {
                IconButton(onClick = onUnpinClick) {
                    Icon(
                        imageVector = Icons.Filled.Close,
                        contentDescription = null,
                        modifier = Modifier.size(Dimens.MessageBubble.iconSize),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
internal fun GroupPinnedMessageContent(
    message: MessageBubbleUi,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    onAttachmentClick: (String) -> Unit = {},
    onContactClick: (SharedContact) -> Unit = {}
) {
    SparrowScrollScaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = { containerColor ->
            PinnedMessageTopBar(
                containerColor = containerColor,
                onBack = onBack
            )
        }
    ) { innerPadding, scrollState ->
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(MaterialTheme.spacing.screenPadding)
                    .verticalScroll(scrollState),
            verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.base)
        ) {
            message.senderName
                ?.takeIf { !message.isMine && it.isNotBlank() }
                ?.let { senderName ->
                    Text(
                        text = senderName,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

            message.voicePart?.let { voicePart ->
                VoiceMessageContent(
                    target =
                        VoiceMessageTarget(
                            attachmentId = voicePart.id,
                            durationMilliseconds = voicePart.durationMilliseconds,
                            source = voicePart.attachmentSource
                        ),
                    modifier = Modifier.fillMaxWidth()
                )
            }

            message.contactPart?.let { contactPart ->
                ContactMessageBubbleBody(
                    contactPart = contactPart,
                    onContactClick = onContactClick
                )
            }

            message.locationPart?.let { locationPart ->
                LocationMessageBubbleBody(
                    locationPart = locationPart,
                    onAttachmentClick = onAttachmentClick
                )
            }

            if (message.imageVideoParts.isNotEmpty()) {
                PhotoVideoMessageBubbleBody(
                    imageVideoParts = message.imageVideoParts,
                    onAttachmentClick = onAttachmentClick
                )
            }

            if (message.fileParts.isNotEmpty()) {
                FileMessageBubbleBody(
                    fileParts = message.fileParts
                )
            }

            message.textPart?.let { textPart ->
                TextMessageBubbleBody(
                    textPart = textPart,
                    safetyWarning = message.safetyWarning,
                    onSafetyDetailsClick = {}
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PinnedMessageTopBar(
    containerColor: Color,
    onBack: () -> Unit
) {
    CenterAlignedTopAppBar(
        windowInsets = WindowInsets(MaterialTheme.spacing.zero),
        colors =
            TopAppBarDefaults.topAppBarColors(
                containerColor = containerColor,
                scrolledContainerColor = containerColor,
                titleContentColor = MaterialTheme.colorScheme.onBackground,
                actionIconContentColor = MaterialTheme.colorScheme.onBackground
            ),
        title = {
            Text(
                text = stringResource(Res.string.feature_chats_pinned_message),
                style = MaterialTheme.typography.titleSmall
            )
        },
        actions = {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = null
                )
            }
        }
    )
}

private fun MessageBubbleUi.pinnedPreviewText(fallback: String): String =
    textPart
        ?.text
        ?.trim()
        ?.takeIf(String::isNotBlank)
        ?: fileParts.firstOrNull()?.fileName?.takeIf(String::isNotBlank)
        ?: fallback

@Preview
@Composable
private fun GroupPinnedMessageBarPreview() {
    SparrowTheme {
        GroupPinnedMessageBar(
            message = MessageBubbleUi(
                id = "2",
                isMine = true,
                security = MessageSecurity.END_TO_END_ENCRYPTED,
                contentStatus = MessageContentStatus.READABLE,
                deliveryStatus = MessageDeliveryStatus.DELIVERED,
                textPart =
                    MessagePartUi.Text(
                        text = "I'm free this evening.",
                        isContentFailed = false
                    )
            ),
            pinnedAtEpochMilliseconds = 0L,
            canUnpin = true,
            onUnpinClick = {},
            onClick = {}
        )
    }
}
