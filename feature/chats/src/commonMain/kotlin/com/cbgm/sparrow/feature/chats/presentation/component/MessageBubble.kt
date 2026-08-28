package com.cbgm.sparrow.feature.chats.presentation.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.absolutePadding
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import com.cbgm.sparrow.core.ui.animation.rememberHighlightColor
import com.cbgm.sparrow.core.ui.theme.Alpha
import com.cbgm.sparrow.core.ui.theme.Dimens
import com.cbgm.sparrow.core.ui.theme.SparrowTheme
import com.cbgm.sparrow.core.ui.theme.messageBubble
import com.cbgm.sparrow.core.ui.theme.spacing
import com.cbgm.sparrow.feature.chats.domain.model.MessageContentStatus
import com.cbgm.sparrow.feature.chats.domain.model.MessageDeliveryStatus
import com.cbgm.sparrow.feature.chats.domain.model.MessageSecurity
import com.cbgm.sparrow.feature.chats.presentation.component.model.ImageVideoTypeUi
import com.cbgm.sparrow.feature.chats.presentation.component.model.MessageBubbleUi
import com.cbgm.sparrow.feature.chats.presentation.component.model.MessagePartUi
import com.cbgm.sparrow.feature.safety.presentation.details.model.MessageSafetyWarningUi
import com.cbgm.sparrow.resources.Res
import com.cbgm.sparrow.resources.feature_chats_decryption_failed
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
    isSearchHighlighted: Boolean = false
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

            BubbleBody(
                message = message,
                state = bubbleState,
                isSearchHighlighted = isSearchHighlighted,
                safetyWarning = safetyWarning,
                onAttachmentVisible = onAttachmentVisible,
                onAttachmentClick = onAttachmentClick,
                onSafetyDetailsClick = {
                    if (safetyWarning != null) onSafetyDetailsClick(safetyWarning)
                }
            )
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

@Composable
private fun BubbleBody(
    message: MessageBubbleUi,
    state: BubbleState,
    isSearchHighlighted: Boolean = false,
    safetyWarning: MessageSafetyWarningUi? = null,
    onAttachmentVisible: (String) -> Unit = {},
    onAttachmentClick: (String) -> Unit = {},
    onSafetyDetailsClick: () -> Unit = {}
) {
    val showTextBubble =
        message.locationPart == null &&
            (state.text.isNotBlank() || state.isContentFailed || safetyWarning != null)

    Column(
        verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.micro),
        horizontalAlignment = if (message.isMine) Alignment.End else Alignment.Start
    ) {
        message.locationPart?.let { locationPart ->
            MessageBubbleSurface(
                message = message,
                state = state,
                isSearchHighlighted = isSearchHighlighted
            ) {
                LocationMessageBubbleBody(
                    locationPart = locationPart,
                    onAttachmentClick = onAttachmentClick
                )
            }
        }

        if (message.imageVideoParts.isNotEmpty()) {
            MessageBubbleSurface(
                message = message,
                state = state,
                isSearchHighlighted = isSearchHighlighted
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
                isSearchHighlighted = isSearchHighlighted
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
                isSearchHighlighted = isSearchHighlighted
            ) {
                TextMessageBubbleBody(
                    textPart =
                        message.textPart
                            ?: MessagePartUi.TextUi(
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
    content: @Composable () -> Unit
) {
    val bubbleShapes = MaterialTheme.shapes.messageBubble
    val bubbleColor =
        rememberHighlightColor(
            isHighlighted = isSearchHighlighted,
            baseColor = state.bubbleColor,
            highlightColor = MaterialTheme.colorScheme.primary.copy(alpha = Alpha.Subtle)
        )

    Surface(
        modifier = modifier,
        color = bubbleColor,
        contentColor = state.contentColor,
        shape =
            MessageBubbleShape(
                isMine = message.isMine,
                cornerRadius = bubbleShapes.cornerRadius,
                tailWidth = bubbleShapes.tailWidth,
                tailHeight = bubbleShapes.tailHeight,
                tailReturnOffset = bubbleShapes.tailReturnOffset
            )
    ) {
        Box(
            modifier =
                Modifier.absolutePadding(
                    left =
                        if (message.isMine) {
                            MaterialTheme.spacing.micro
                        } else {
                            bubbleShapes.tailWidth + MaterialTheme.spacing.micro
                        },
                    top = MaterialTheme.spacing.micro,
                    right =
                        if (message.isMine) {
                            bubbleShapes.tailWidth + MaterialTheme.spacing.micro
                        } else {
                            MaterialTheme.spacing.micro
                        },
                    bottom = MaterialTheme.spacing.micro
                )
        ) {
            content()
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
            progress.readCount > 0 -> "Read ${progress.readCount}/${progress.recipientCount}"
            progress.deliveredCount > 0 -> "Delivered ${progress.deliveredCount}/${progress.recipientCount}"
            else -> "Sending…"
        }

    Text(
        text = text,
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onBackground.copy(alpha = Alpha.OpaqueText)
    )
}

@Composable
private fun SecurityIndicator(message: MessageBubbleUi) {
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
            modifier = Modifier.size(Dimens.MessageBubble.iconSize),
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
private fun DeliveryIndicator(
    deliveryStatus: MessageDeliveryStatus,
    onRetryClick: () -> Unit
) {
    when (deliveryStatus) {
        MessageDeliveryStatus.NOT_APPLICABLE -> Unit
        MessageDeliveryStatus.WAITING_FOR_AUTHORIZATION ->
            DeliveryLabel(
                text = stringResource(Res.string.feature_chats_waiting_for_invitation_acceptance),
                icon = {
                    Icon(
                        imageVector = Icons.Default.Schedule,
                        contentDescription = null,
                        modifier = Modifier.size(Dimens.MessageBubble.iconSize)
                    )
                }
            )

        MessageDeliveryStatus.QUEUED ->
            DeliveryLabel(
                text = stringResource(Res.string.feature_chats_queued),
                icon = {
                    Icon(
                        imageVector = Icons.Default.Schedule,
                        contentDescription = null,
                        modifier = Modifier.size(Dimens.MessageBubble.iconSize)
                    )
                }
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
        icon = {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = null,
                modifier = Modifier.size(Dimens.MessageBubble.iconSize)
            )
        }
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
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    modifier = Modifier.size(Dimens.MessageBubble.iconSize),
                    tint = color
                )
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    modifier = Modifier.size(Dimens.MessageBubble.iconSize)
                        .padding(start = MaterialTheme.spacing.messageBubble.stackedCheckStartPadding),
                    tint = color
                )
            }
        }
    )
}

@Composable
private fun FailedDelivery(onRetryClick: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            imageVector = Icons.Default.ErrorOutline,
            contentDescription = null,
            modifier = Modifier.size(Dimens.MessageBubble.iconSize),
            tint = MaterialTheme.colorScheme.error
        )
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

@Preview
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
                    textPart =
                        MessagePartUi.TextUi(
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
                        MessagePartUi.FileUi(
                            id = "preview-file",
                            mimeType = "application/pdf",
                            byteSize = 0,
                            fileName = "test.pdf",
                            localFilePath = ""
                        )
                    ),
                    imageVideoParts = listOf(
                        MessagePartUi.ImageVideoUi(
                            id = "preview-image-1",
                            type = ImageVideoTypeUi.IMAGE,
                            mimeType = "image/jpeg",
                            byteSize = 0
                        ),
                        MessagePartUi.ImageVideoUi(
                            id = "preview-video",
                            type = ImageVideoTypeUi.VIDEO,
                            mimeType = "video/mp4",
                            byteSize = 0
                        )
                    ),
                    locationPart = null,
                    textPart = MessagePartUi.TextUi(
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
