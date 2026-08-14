package com.cbgm.sparrow.feature.chats.presentation.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.unit.dp
import com.cbgm.sparrow.core.ui.theme.SparrowTheme
import com.cbgm.sparrow.core.ui.theme.spacing
import com.cbgm.sparrow.feature.chats.domain.model.MessageContentStatus
import com.cbgm.sparrow.feature.chats.domain.model.MessageDeliveryStatus
import com.cbgm.sparrow.feature.chats.domain.model.MessageSecurity
import com.cbgm.sparrow.feature.chats.presentation.component.model.MessageBubbleModel
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
import org.jetbrains.compose.resources.stringResource

private val IncomingBubbleColor = Color(0xFF17324D)

@Composable
internal fun MessageBubble(
    message: MessageBubbleModel,
    onRetryClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val bubbleState = bubbleState(message)

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = if (message.isMine) Arrangement.End else Arrangement.Start
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(fraction = 0.78f),
            horizontalAlignment = if (message.isMine) Alignment.End else Alignment.Start
        ) {
            SenderLabel(message = message)
            BubbleBody(message = message, state = bubbleState)
            Metadata(
                message = message,
                onRetryClick = onRetryClick,
                modifier = Modifier.padding(top = 3.dp, start = 4.dp, end = 4.dp)
            )
        }
    }
}

@Composable
private fun SenderLabel(message: MessageBubbleModel) {
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
        modifier = Modifier.padding(start = 8.dp, bottom = 3.dp),
        style = MaterialTheme.typography.labelSmall,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.secondary
    )
}

@Composable
private fun BubbleBody(
    message: MessageBubbleModel,
    state: BubbleState
) {
    Surface(
        color = state.bubbleColor,
        contentColor = state.contentColor,
        shape =
            RoundedCornerShape(
                topStart = 16.dp,
                topEnd = 16.dp,
                bottomStart = if (message.isMine) 16.dp else 4.dp,
                bottomEnd = if (message.isMine) 4.dp else 16.dp
            )
    ) {
        Row(
            modifier =
                Modifier.padding(
                    horizontal = MaterialTheme.spacing.small,
                    vertical = MaterialTheme.spacing.base
                ),
            verticalAlignment = Alignment.Top
        ) {
            if (state.isContentFailed) {
                Icon(imageVector = Icons.Default.ErrorOutline, contentDescription = null)
                Spacer(modifier = Modifier.width(MaterialTheme.spacing.base))
            }

            Text(
                text = state.text,
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

@Composable
private fun Metadata(
    message: MessageBubbleModel,
    onRetryClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
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
private fun DeliveryProgress(message: MessageBubbleModel) {
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
        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
    )
}

@Composable
private fun SecurityIndicator(message: MessageBubbleModel) {
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
            modifier = Modifier.size(14.dp),
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
        MessageDeliveryStatus.QUEUED ->
            DeliveryLabel(
                text = stringResource(Res.string.feature_chats_queued),
                icon = {
                    Icon(
                        imageVector = Icons.Default.Schedule,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp)
                    )
                }
            )
        MessageDeliveryStatus.SENDING ->
            DeliveryLabel(
                text = stringResource(Res.string.feature_chats_sending),
                icon = {
                    CircularProgressIndicator(
                        modifier = Modifier.size(12.dp),
                        strokeWidth = 1.5.dp
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
                color = MaterialTheme.colorScheme.secondary
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
                modifier = Modifier.size(14.dp)
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
                    modifier = Modifier.size(14.dp),
                    tint = color
                )
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    modifier = Modifier.size(14.dp).padding(start = 1.dp),
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
            modifier = Modifier.size(14.dp),
            tint = MaterialTheme.colorScheme.error
        )
        Spacer(modifier = Modifier.width(3.dp))
        Text(
            text = stringResource(Res.string.feature_chats_failed),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.error
        )
        IconButton(
            onClick = onRetryClick,
            modifier = Modifier.size(28.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Refresh,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
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
        Spacer(modifier = Modifier.width(3.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color = textColor
        )
    }
}

@Composable
private fun bubbleState(message: MessageBubbleModel): BubbleState =
    when (message.contentStatus) {
        MessageContentStatus.READABLE ->
            BubbleState(
                text = message.text,
                isContentFailed = false,
                bubbleColor =
                    if (message.isMine) {
                        MaterialTheme.colorScheme.secondary.copy(alpha = 0.18f)
                    } else {
                        IncomingBubbleColor
                    },
                contentColor = MaterialTheme.colorScheme.onBackground
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

private data class BubbleState(
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
                MessageBubbleModel(
                    id = "preview",
                    text = "Encrypted message",
                    isMine = true,
                    security = MessageSecurity.END_TO_END_ENCRYPTED,
                    contentStatus = MessageContentStatus.READABLE,
                    deliveryStatus = MessageDeliveryStatus.DELIVERED
                ),
            onRetryClick = {}
        )
    }
}
