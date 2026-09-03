package com.cbgm.sparrow.feature.chats.presentation.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.absolutePadding
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.zIndex
import com.cbgm.sparrow.core.ui.animation.rememberHighlightColor
import com.cbgm.sparrow.core.ui.component.SparrowOverlayAnchor
import com.cbgm.sparrow.core.ui.component.captureSparrowOverlayAnchor
import com.cbgm.sparrow.core.ui.theme.Alpha
import com.cbgm.sparrow.core.ui.theme.Dimens
import com.cbgm.sparrow.core.ui.theme.SparrowTheme
import com.cbgm.sparrow.core.ui.theme.messageBubble
import com.cbgm.sparrow.core.ui.theme.spacing
import com.cbgm.sparrow.feature.attachments.domain.model.SharedContact
import com.cbgm.sparrow.feature.chats.domain.model.MessageContentStatus
import com.cbgm.sparrow.feature.chats.domain.model.MessageDeliveryStatus
import com.cbgm.sparrow.feature.chats.domain.model.MessageSecurity
import com.cbgm.sparrow.feature.chats.presentation.component.model.ImageVideoTypeUi
import com.cbgm.sparrow.feature.chats.presentation.component.model.MessageBubbleUi
import com.cbgm.sparrow.feature.chats.presentation.component.model.MessagePartUi
import com.cbgm.sparrow.feature.chats.presentation.component.model.MessageReactionUi
import com.cbgm.sparrow.feature.chats.presentation.component.model.MessageReplyUi
import com.cbgm.sparrow.feature.safety.presentation.details.model.MessageSafetyWarningUi
import com.cbgm.sparrow.resources.Res
import com.cbgm.sparrow.resources.feature_chats_delivered
import com.cbgm.sparrow.resources.feature_chats_encrypted
import com.cbgm.sparrow.resources.feature_chats_failed
import com.cbgm.sparrow.resources.feature_chats_invalid_message_packet
import com.cbgm.sparrow.resources.feature_chats_invalid_packet
import com.cbgm.sparrow.resources.feature_chats_invalid_plaintext
import com.cbgm.sparrow.resources.feature_chats_not_encrypted
import com.cbgm.sparrow.resources.feature_chats_queued
import com.cbgm.sparrow.resources.feature_chats_read
import com.cbgm.sparrow.resources.feature_chats_sender_not_in_contacts
import com.cbgm.sparrow.resources.feature_chats_sending
import com.cbgm.sparrow.resources.feature_chats_sent
import com.cbgm.sparrow.resources.feature_chats_unable_decrypt_secure_message
import com.cbgm.sparrow.resources.feature_chats_unable_read_plaintext
import com.cbgm.sparrow.resources.feature_chats_waiting_for_invitation_acceptance
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun MessageBubble(
    message: MessageBubbleUi,
    onRetryClick: () -> Unit,
    modifier: Modifier = Modifier,
    onSafetyDetailsClick: (MessageSafetyWarningUi) -> Unit = {},
    onAttachmentVisible: (String) -> Unit = {},
    onAttachmentClick: (String) -> Unit = {},
    onContactClick: (SharedContact) -> Unit = {},
    onReplyPreviewClick: (String) -> Unit = {},
    onActionMenuVisibilityChange: (Boolean) -> Unit = {},
    onReactionsClick: (SparrowOverlayAnchor) -> Unit = {},
    isSearchHighlighted: Boolean = false,
    showMetadata: Boolean = true
) {
    val bubbleState = bubbleState(message)
    val safetyWarning = message.safetyWarning

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = if (message.isMine) Arrangement.End else Arrangement.Start
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(fraction = 0.78f),
            horizontalAlignment = if (message.isMine) Alignment.End else Alignment.Start
        ) {
            SenderLabel(message = message)

            Box(
                modifier =
                    if (message.reactions.isNotEmpty()) {
                        Modifier.padding(bottom = MaterialTheme.spacing.base)
                    } else {
                        Modifier
                    }
            ) {
                BubbleBody(
                    message = message,
                    state = bubbleState,
                    isSearchHighlighted = isSearchHighlighted,
                    safetyWarning = safetyWarning,
                    onAttachmentVisible = onAttachmentVisible,
                    onAttachmentClick = onAttachmentClick,
                    onContactClick = onContactClick,
                    onReplyPreviewClick = onReplyPreviewClick,
                    onLongPress = { onActionMenuVisibilityChange(true) },
                    onSafetyDetailsClick = {
                        safetyWarning?.let(onSafetyDetailsClick)
                    }
                )

                MessageReactions(
                    reactions = message.reactions,
                    onClick = onReactionsClick,
                    modifier =
                        Modifier
                            .align(
                                if (message.isMine) {
                                    Alignment.BottomStart
                                } else {
                                    Alignment.BottomEnd
                                }
                            )
                            .offset(y = MaterialTheme.spacing.small)
                )
            }
            if (showMetadata) {
                Metadata(
                    message = message,
                    onRetryClick = onRetryClick,
                    modifier =
                        Modifier.padding(
                            top = MaterialTheme.spacing.messageBubble.metadataTopPadding,
                            start = MaterialTheme.spacing.micro,
                            end = MaterialTheme.spacing.micro
                        )
                )
            }
        }
    }
}

@Composable
private fun SenderLabel(message: MessageBubbleUi) {
    if (message.isMine || message.senderName.isNullOrBlank()) return

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
        modifier = Modifier.padding(
            start = MaterialTheme.spacing.small,
            bottom = MaterialTheme.spacing.messageBubble.senderBottomPadding
        ),
        style = MaterialTheme.typography.labelSmall,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.primary
    )
}

private enum class PrimaryContent { CONTACT, LOCATION, IMAGE_VIDEO, FILE, TEXT, NONE }

private fun MessageBubbleUi.primaryContent(showTextBubble: Boolean): PrimaryContent = when {
    contactPart != null -> PrimaryContent.CONTACT
    locationPart != null -> PrimaryContent.LOCATION
    imageVideoParts.isNotEmpty() -> PrimaryContent.IMAGE_VIDEO
    fileParts.isNotEmpty() -> PrimaryContent.FILE
    showTextBubble -> PrimaryContent.TEXT
    else -> PrimaryContent.NONE
}

@Composable
private fun BubbleBody(
    message: MessageBubbleUi,
    state: BubbleState,
    isSearchHighlighted: Boolean = false,
    safetyWarning: MessageSafetyWarningUi? = null,
    onAttachmentVisible: (String) -> Unit = {},
    onAttachmentClick: (String) -> Unit = {},
    onContactClick: (SharedContact) -> Unit = {},
    onReplyPreviewClick: (String) -> Unit = {},
    onLongPress: () -> Unit = {},
    onSafetyDetailsClick: () -> Unit = {}
) {
    val showTextBubble =
        message.locationPart == null &&
            message.contactPart == null &&
            (state.text.isNotBlank() || state.isContentFailed || safetyWarning != null)

    val reply = message.reply
    val primaryContent = message.primaryContent(showTextBubble)

    fun replyFor(target: PrimaryContent) = reply?.takeIf { primaryContent == target }

    Column(
        verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.micro),
        horizontalAlignment = if (message.isMine) Alignment.End else Alignment.Start
    ) {
        message.contactPart?.let { contactPart ->
            MessageBubbleSurface(
                message = message,
                state = state,
                hasInnerPadding = false,
                isSearchHighlighted = isSearchHighlighted,
                reply = replyFor(PrimaryContent.CONTACT),
                onReplyPreviewClick = onReplyPreviewClick,
                onLongPress = onLongPress
            ) {
                ContactMessageBubbleBody(
                    contactPart = contactPart,
                    onAttachmentVisible = onAttachmentVisible,
                    onContactClick = onContactClick
                )
            }
        }

        message.locationPart?.let { locationPart ->
            MessageBubbleSurface(
                message = message,
                state = state,
                hasInnerPadding = false,
                isSearchHighlighted = isSearchHighlighted,
                reply = replyFor(PrimaryContent.LOCATION),
                onReplyPreviewClick = onReplyPreviewClick,
                onLongPress = onLongPress
            ) {
                LocationMessageBubbleBody(
                    locationPart = locationPart,
                    onAttachmentVisible = onAttachmentVisible,
                    onAttachmentClick = onAttachmentClick
                )
            }
        }

        if (message.imageVideoParts.isNotEmpty()) {
            MessageBubbleSurface(
                message = message,
                state = state,
                isSearchHighlighted = isSearchHighlighted,
                reply = replyFor(PrimaryContent.IMAGE_VIDEO),
                onReplyPreviewClick = onReplyPreviewClick,
                onLongPress = onLongPress
            ) {
                PhotoVideoMessageBubbleBody(
                    imageVideoParts = message.imageVideoParts,
                    onAttachmentVisible = onAttachmentVisible,
                    onAttachmentClick = onAttachmentClick
                )
            }
        }

        if (message.fileParts.isNotEmpty()) {
            MessageBubbleSurface(
                message = message,
                state = state,
                isSearchHighlighted = isSearchHighlighted,
                reply = replyFor(PrimaryContent.FILE),
                onReplyPreviewClick = onReplyPreviewClick,
                onLongPress = onLongPress
            ) {
                FileMessageBubbleBody(
                    fileParts = message.fileParts,
                    onAttachmentVisible = onAttachmentVisible
                )
            }
        }

        if (showTextBubble) {
            MessageBubbleSurface(
                message = message,
                state = state,
                hasInnerPadding = safetyWarning == null,
                isSearchHighlighted = isSearchHighlighted,
                reply = replyFor(PrimaryContent.TEXT),
                onReplyPreviewClick = onReplyPreviewClick,
                onLongPress = onLongPress
            ) {
                TextMessageBubbleBody(
                    textPart =
                        message.textPart
                            ?: MessagePartUi.Text(
                                text = state.text,
                                isContentFailed = state.isContentFailed
                            ),
                    safetyWarning = safetyWarning,
                    onSafetyDetailsClick = onSafetyDetailsClick
                )
            }
        }
    }
}

@Composable
private fun MessageBubbleSurface(
    message: MessageBubbleUi,
    state: BubbleState,
    isSearchHighlighted: Boolean,
    modifier: Modifier = Modifier,
    hasInnerPadding: Boolean = true,
    reply: MessageReplyUi? = null,
    onReplyPreviewClick: (String) -> Unit = {},
    onLongPress: () -> Unit = {},
    content: @Composable () -> Unit
) {
    val bubbleShapes = MaterialTheme.shapes.messageBubble
    val bubbleColor =
        rememberHighlightColor(
            isHighlighted = isSearchHighlighted,
            baseColor = state.bubbleColor,
            highlightColor = MaterialTheme.colorScheme.primary.copy(alpha = Alpha.Subtle)
        )
    val bubbleShape =
        MessageBubbleShape(
            isMine = message.isMine,
            cornerRadius = bubbleShapes.cornerRadius,
            tailWidth = bubbleShapes.tailWidth,
            tailHeight = bubbleShapes.tailHeight,
            tailReturnOffset = bubbleShapes.tailReturnOffset
        )

    val contentPadding =
        if (hasInnerPadding) MaterialTheme.spacing.micro else MaterialTheme.spacing.zero
    val tailPadding = bubbleShapes.tailWidth + contentPadding

    Surface(
        modifier = modifier
            .combinedClickable(
                onClick = {},
                onLongClick = onLongPress
            ),
        color = bubbleColor,
        contentColor = state.contentColor,
        shape = bubbleShape
    ) {
        Column {
            reply?.let { replyPreview ->
                MessageReplyInlay(
                    reply = replyPreview,
                    onClick = { onReplyPreviewClick(replyPreview.messageId) },
                    color = state.bubbleColor,
                    isMine = message.isMine
                )
            }

            Box(
                modifier =
                    Modifier.absolutePadding(
                        left = if (message.isMine) contentPadding else tailPadding,
                        top = contentPadding,
                        right = if (message.isMine) tailPadding else contentPadding,
                        bottom = contentPadding
                    )
            ) {
                content()
            }
        }
    }
}

@Composable
private fun MessageReactions(
    reactions: List<MessageReactionUi>,
    onClick: (SparrowOverlayAnchor) -> Unit,
    modifier: Modifier = Modifier
) {
    if (reactions.isEmpty()) return

    val visibleReactions = reactions.take(3)
    val iconSlotSize = Dimens.MessageReaction.cloudIconSlotSize
    val iconStep = Dimens.MessageReaction.cloudIconStep
    val cloudHeight = iconSlotSize + Dimens.MessageReaction.cloudSideOffsetY
    val cloudWidth = iconSlotSize + iconStep * (visibleReactions.size - 1).toFloat()
    var anchor by remember { mutableStateOf<SparrowOverlayAnchor?>(null) }

    val interactionSource = remember { MutableInteractionSource() }

    Surface(
        modifier =
            modifier
                .captureSparrowOverlayAnchor { anchor = it }
                .clickable(
                    interactionSource = interactionSource,
                    indication = null,
                    onClick = { anchor?.let(onClick) }
                ),
        color = Color.Transparent
    ) {
        Box(
            modifier = Modifier
                .padding(Dimens.MessageReaction.cloudContentPadding)
                .width(cloudWidth)
                .height(cloudHeight)
        ) {
            visibleReactions.forEachIndexed { index, reaction ->
                val yOffset =
                    if (visibleReactions.size == 3 && index != 1) {
                        Dimens.MessageReaction.cloudSideOffsetY
                    } else {
                        Dimens.Base.zero
                    }

                Box(
                    modifier =
                        Modifier
                            .size(iconSlotSize)
                            .offset(
                                x = iconStep * index.toFloat(),
                                y = yOffset
                            )
                            .zIndex(index.toFloat()),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = reaction.emoji,
                        style = MaterialTheme.typography.labelMedium
                    )
                }
            }
        }
    }
}

@Composable
private fun Metadata(
    message: MessageBubbleUi,
    onRetryClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.micro)
    ) {
        SecurityIndicator(message = message)

        if (message.isMine && message.deliveryStatus != MessageDeliveryStatus.NOT_APPLICABLE) {
            DeliveryIndicator(
                deliveryStatus = message.deliveryStatus,
                onRetryClick = onRetryClick
            )
            DeliveryProgress(message = message)
        }
    }
}

@Composable
private fun DeliveryProgress(message: MessageBubbleUi) {
    val progress = message.deliveryProgress
    if (progress.recipientCount <= 1) return

    val text =
        when {
            progress.readCount > 0 -> "${progress.readCount}/${progress.recipientCount}"
            progress.deliveredCount > 0 -> "${progress.deliveredCount}/${progress.recipientCount}"
            else -> "Sending…"
        }

    Text(
        text = text,
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onBackground.copy(alpha = Alpha.OpaqueText)
    )
}

@Composable
private fun MetadataIcon(
    imageVector: ImageVector,
    modifier: Modifier = Modifier,
    tint: Color = MaterialTheme.colorScheme.onSurfaceVariant
) {
    Icon(
        imageVector = imageVector,
        contentDescription = null,
        modifier = modifier.size(Dimens.MessageBubble.iconSize),
        tint = tint
    )
}

@Composable
private fun SecurityIndicator(message: MessageBubbleUi) {
    val text =
        when (message.contentStatus) {
            MessageContentStatus.INVALID_PACKET -> stringResource(Res.string.feature_chats_invalid_packet)
            MessageContentStatus.INVALID_PLAINTEXT_PACKET -> stringResource(Res.string.feature_chats_invalid_plaintext)
            MessageContentStatus.TRANSPORT_DECRYPTION_FAILED ->
                stringResource(Res.string.feature_chats_unable_decrypt_secure_message)

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
        MetadataIcon(icon)
        Spacer(modifier = Modifier.width(MaterialTheme.spacing.base))
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun DeliveryIndicator(
    deliveryStatus: MessageDeliveryStatus,
    onRetryClick: () -> Unit
) {
    when (deliveryStatus) {
        MessageDeliveryStatus.NOT_APPLICABLE -> Unit

        MessageDeliveryStatus.WAITING_FOR_AUTHORIZATION,
        MessageDeliveryStatus.QUEUED ->
            DeliveryLabel(
                text = stringResource(
                    if (deliveryStatus == MessageDeliveryStatus.WAITING_FOR_AUTHORIZATION) {
                        Res.string.feature_chats_waiting_for_invitation_acceptance
                    } else {
                        Res.string.feature_chats_queued
                    }
                ),
                icon = { MetadataIcon(Icons.Default.Schedule) }
            )

        MessageDeliveryStatus.SENDING ->
            DeliveryLabel(
                text = stringResource(Res.string.feature_chats_sending),
                icon = {
                    CircularProgressIndicator(
                        modifier = Modifier.size(Dimens.MessageBubble.progressSize),
                        strokeWidth = Dimens.MessageBubble.progressStrokeWidth
                    )
                }
            )

        MessageDeliveryStatus.SENT -> CheckDeliveryLabel(stringResource(Res.string.feature_chats_sent))
        MessageDeliveryStatus.DELIVERED ->
            DoubleCheckDeliveryLabel(
                text = stringResource(Res.string.feature_chats_delivered),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

        MessageDeliveryStatus.READ ->
            DoubleCheckDeliveryLabel(
                text = stringResource(Res.string.feature_chats_read),
                color = MaterialTheme.colorScheme.primary
            )

        MessageDeliveryStatus.FAILED -> FailedDelivery(onRetryClick = onRetryClick)
    }
}

@Composable
private fun CheckDeliveryLabel(text: String) {
    DeliveryLabel(
        text = text,
        icon = { MetadataIcon(Icons.Default.Check) }
    )
}

@Composable
private fun DoubleCheckDeliveryLabel(
    text: String,
    color: Color
) {
    DeliveryLabel(
        text = text,
        textColor = color,
        icon = {
            Row {
                MetadataIcon(Icons.Default.Check, tint = color)
                MetadataIcon(
                    Icons.Default.Check,
                    modifier = Modifier.padding(start = MaterialTheme.spacing.messageBubble.stackedCheckStartPadding),
                    tint = color
                )
            }
        }
    )
}

@Composable
private fun FailedDelivery(onRetryClick: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        MetadataIcon(Icons.Default.ErrorOutline, tint = MaterialTheme.colorScheme.error)
        Spacer(modifier = Modifier.width(MaterialTheme.spacing.messageBubble.deliveryLabelGap))
        Text(
            text = stringResource(Res.string.feature_chats_failed),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.error
        )
        IconButton(
            onClick = onRetryClick,
            modifier = Modifier.size(Dimens.MessageBubble.retrySize)
        ) {
            Icon(
                imageVector = Icons.Default.Refresh,
                contentDescription = null,
                modifier = Modifier.size(Dimens.MessageBubble.retryIconSize),
                tint = MaterialTheme.colorScheme.error
            )
        }
    }
}

@Composable
private fun DeliveryLabel(
    text: String,
    icon: @Composable () -> Unit,
    textColor: Color = MaterialTheme.colorScheme.onSurfaceVariant
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        icon()
        Spacer(modifier = Modifier.width(MaterialTheme.spacing.messageBubble.deliveryLabelGap))
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color = textColor
        )
    }
}

@Composable
private fun bubbleState(message: MessageBubbleUi): BubbleState =
    when (message.contentStatus) {
        MessageContentStatus.READABLE ->
            BubbleState(
                text = message.textPart?.text ?: "",
                isContentFailed = false,
                bubbleColor =
                    if (message.isMine) {
                        MaterialTheme.colorScheme.primaryContainer
                    } else {
                        MaterialTheme.colorScheme.surfaceContainerHigh
                    },
                contentColor = if (message.isMine) {
                    MaterialTheme.colorScheme.onPrimaryContainer
                } else {
                    MaterialTheme.colorScheme.onSurface
                }
            )

        MessageContentStatus.INVALID_PACKET ->
            failedBubbleState(stringResource(Res.string.feature_chats_invalid_message_packet))

        MessageContentStatus.INVALID_PLAINTEXT_PACKET ->
            failedBubbleState(stringResource(Res.string.feature_chats_unable_read_plaintext))

        MessageContentStatus.TRANSPORT_DECRYPTION_FAILED ->
            failedBubbleState(stringResource(Res.string.feature_chats_unable_decrypt_secure_message))
    }

@Composable
private fun failedBubbleState(text: String) =
    BubbleState(
        text = text,
        isContentFailed = true,
        bubbleColor = MaterialTheme.colorScheme.errorContainer,
        contentColor = MaterialTheme.colorScheme.onErrorContainer
    )

internal data class BubbleState(
    val text: String,
    val isContentFailed: Boolean,
    val bubbleColor: Color,
    val contentColor: Color
)

@Preview(heightDp = 300)
@Composable
private fun MessageBubblePreview() {
    SparrowTheme {
        MessageBubble(
            message =
                MessageBubbleUi(
                    id = "preview",
                    isMine = true,
                    security = MessageSecurity.END_TO_END_ENCRYPTED,
                    contentStatus = MessageContentStatus.READABLE,
                    deliveryStatus = MessageDeliveryStatus.DELIVERED,
                    reactions =
                        listOf(
                            MessageReactionUi(emoji = "❤️", count = 2, reactedByMe = true),
                            MessageReactionUi(emoji = "😂", count = 1, reactedByMe = false),
                            MessageReactionUi(emoji = "👍", count = 3, reactedByMe = false),
                            MessageReactionUi(emoji = "🔥", count = 1, reactedByMe = false)
                        ),
                    textPart =
                        MessagePartUi.Text(
                            text = "Encrypted message",
                            isContentFailed = false
                        )
                ),
            onRetryClick = {}
        )
    }
}

@Preview
@Composable
private fun MessageBubbleWithAttachmentsPreview() {
    SparrowTheme {
        MessageBubble(
            message =
                MessageBubbleUi(
                    id = "preview-attachments",
                    isMine = false,
                    security = MessageSecurity.END_TO_END_ENCRYPTED,
                    contentStatus = MessageContentStatus.READABLE,
                    deliveryStatus = MessageDeliveryStatus.DELIVERED,
                    senderName = "Chris",
                    fileParts = listOf(
                        MessagePartUi.File(
                            id = "preview-file",
                            mimeType = "application/pdf",
                            byteSize = 0,
                            fileName = "test.pdf",
                            localFilePath = ""
                        )
                    ),
                    imageVideoParts = listOf(
                        MessagePartUi.ImageVideo(
                            id = "preview-image-1",
                            type = ImageVideoTypeUi.IMAGE,
                            mimeType = "image/jpeg",
                            byteSize = 0
                        ),
                        MessagePartUi.ImageVideo(
                            id = "preview-video",
                            type = ImageVideoTypeUi.VIDEO,
                            mimeType = "video/mp4",
                            byteSize = 0
                        )
                    ),
                    locationPart = null,
                    textPart = MessagePartUi.Text(
                        text = "Test message",
                        isContentFailed = false
                    )
                ),
            onRetryClick = {},
            onAttachmentVisible = {},
            onAttachmentClick = {}
        )
    }
}
