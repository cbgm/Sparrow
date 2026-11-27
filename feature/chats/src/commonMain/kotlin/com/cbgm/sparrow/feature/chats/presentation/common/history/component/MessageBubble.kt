package com.cbgm.sparrow.feature.chats.presentation.common.history.component

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.layout.SubcomposeLayout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.cbgm.sparrow.core.base.message.domain.model.MessageContentStatus
import com.cbgm.sparrow.core.base.message.domain.model.MessageDeliveryStatus
import com.cbgm.sparrow.core.base.messagepart.ui.model.ContactUi
import com.cbgm.sparrow.core.base.messagepart.ui.model.ExpenseBoardUi
import com.cbgm.sparrow.core.base.messagepart.ui.model.FileUi
import com.cbgm.sparrow.core.base.messagepart.ui.model.ImageUi
import com.cbgm.sparrow.core.base.messagepart.ui.model.ImageVideoUi
import com.cbgm.sparrow.core.base.messagepart.ui.model.LocationUi
import com.cbgm.sparrow.core.base.messagepart.ui.model.PollUi
import com.cbgm.sparrow.core.base.messagepart.ui.model.TextUi
import com.cbgm.sparrow.core.base.messagepart.ui.model.VideoUi
import com.cbgm.sparrow.core.base.messagepart.ui.model.VoiceUi
import com.cbgm.sparrow.core.ui.animation.rememberHighlightColor
import com.cbgm.sparrow.core.ui.component.DeliveryDotsState
import com.cbgm.sparrow.core.ui.component.SparrowDeliveryDots
import com.cbgm.sparrow.core.ui.component.SparrowOverlayAnchor
import com.cbgm.sparrow.core.ui.component.captureSparrowOverlayAnchor
import com.cbgm.sparrow.core.ui.theme.Alpha
import com.cbgm.sparrow.core.ui.theme.Dimens
import com.cbgm.sparrow.core.ui.theme.SparrowTheme
import com.cbgm.sparrow.core.ui.theme.circle
import com.cbgm.sparrow.core.ui.theme.messageBubble
import com.cbgm.sparrow.core.ui.theme.spacing
import com.cbgm.sparrow.core.ui.theme.warning
import com.cbgm.sparrow.feature.attachments.domain.model.SharedContact
import com.cbgm.sparrow.feature.chats.domain.model.MessageSecurity
import com.cbgm.sparrow.feature.chats.presentation.common.history.mapper.standalonePartOrNull
import com.cbgm.sparrow.feature.chats.presentation.common.history.mapper.textOrNull
import com.cbgm.sparrow.feature.chats.presentation.common.history.mapper.textPartOrNull
import com.cbgm.sparrow.feature.chats.presentation.common.history.model.MessageBubbleUi
import com.cbgm.sparrow.feature.chats.presentation.common.history.model.MessageContextAnchor
import com.cbgm.sparrow.feature.chats.presentation.common.history.model.MessageReactionUi
import com.cbgm.sparrow.feature.chats.presentation.common.history.model.MessageReplyUi
import com.cbgm.sparrow.feature.expenses.presentation.message.ExpenseBoardMessageContent
import com.cbgm.sparrow.feature.linkpreview.presentation.model.TextContentPart
import com.cbgm.sparrow.feature.linkpreview.presentation.model.toTextContentParts
import com.cbgm.sparrow.feature.polls.presentation.message.PollMessageContent
import com.cbgm.sparrow.feature.safety.presentation.details.model.MessageSafetyWarningUi
import com.cbgm.sparrow.feature.voice.presentation.message.VoiceMessageContent
import com.cbgm.sparrow.resources.Res
import com.cbgm.sparrow.resources.feature_chats_delivered
import com.cbgm.sparrow.resources.feature_chats_failed_tap_to_retry
import com.cbgm.sparrow.resources.feature_chats_invalid_message_packet
import com.cbgm.sparrow.resources.feature_chats_not_encrypted
import com.cbgm.sparrow.resources.feature_chats_queued
import com.cbgm.sparrow.resources.feature_chats_read
import com.cbgm.sparrow.resources.feature_chats_sender_not_in_contacts
import com.cbgm.sparrow.resources.feature_chats_sending
import com.cbgm.sparrow.resources.feature_chats_sent
import com.cbgm.sparrow.resources.feature_chats_unable_decrypt_secure_message
import com.cbgm.sparrow.resources.feature_chats_unable_read_plaintext
import com.cbgm.sparrow.resources.feature_chats_waiting_for_authorization
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun MessageBubble(
    message: MessageBubbleUi,
    onRetryClick: () -> Unit,
    modifier: Modifier = Modifier,
    onSafetyDetailsClick: (MessageSafetyWarningUi) -> Unit = {},
    onAttachmentClick: (String) -> Unit = {},
    onContactClick: (SharedContact) -> Unit = {},
    onReplyPreviewClick: (String) -> Unit = {},
    onContextMessageRequested: (MessageContextAnchor) -> Unit = {},
    onReactionsClick: (SparrowOverlayAnchor) -> Unit = {},
    onPollVoteSubmit: (String, String, Set<String>) -> Unit = { _, _, _ -> },
    onPollClose: (String, String) -> Unit = { _, _ -> },
    isSearchHighlighted: Boolean = false,
    showMetadata: Boolean = true,
    isContextSelected: Boolean = false,
    contextMenuEnabled: Boolean = true,
    leadingContent: (@Composable () -> Unit)? = null
) {
    var anchor by remember(message.id) { mutableStateOf<MessageContextAnchor?>(null) }

    val contextModifier =
        modifier
            .captureMessageContextAnchor(
                messageId = message.id,
                isMine = message.isMine,
                onAnchorChanged = { anchor = it }
            )
            .alpha(if (isContextSelected) 0f else 1f)

    val onLongPress: () -> Unit = {
        if (contextMenuEnabled) anchor?.let(onContextMessageRequested)
    }

    val content: @Composable (Modifier) -> Unit = { contentModifier ->
        MessageBubbleContent(
            message = message,
            onRetryClick = onRetryClick,
            onSafetyDetailsClick = onSafetyDetailsClick,
            onAttachmentClick = onAttachmentClick,
            onContactClick = onContactClick,
            onReplyPreviewClick = onReplyPreviewClick,
            onLongPress = onLongPress,
            onReactionsClick = onReactionsClick,
            onPollVoteSubmit = onPollVoteSubmit,
            onPollClose = onPollClose,
            modifier = contentModifier,
            isSearchHighlighted = isSearchHighlighted,
            showMetadata = showMetadata
        )
    }

    if (leadingContent != null) {
        Row(
            modifier = contextModifier.fillMaxWidth(),
            verticalAlignment = Alignment.Bottom
        ) {
            leadingContent()
            content(Modifier.weight(1f))
        }
    } else {
        content(contextModifier)
    }
}

@Composable
private fun MessageBubbleContent(
    message: MessageBubbleUi,
    onRetryClick: () -> Unit,
    onSafetyDetailsClick: (MessageSafetyWarningUi) -> Unit,
    onAttachmentClick: (String) -> Unit,
    onContactClick: (SharedContact) -> Unit,
    onReplyPreviewClick: (String) -> Unit,
    onLongPress: () -> Unit,
    onReactionsClick: (SparrowOverlayAnchor) -> Unit,
    onPollVoteSubmit: (String, String, Set<String>) -> Unit,
    onPollClose: (String, String) -> Unit,
    modifier: Modifier = Modifier,
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
            modifier = Modifier.fillMaxWidth(fraction = 0.85f),
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
                    onAttachmentClick = onAttachmentClick,
                    onContactClick = onContactClick,
                    onReplyPreviewClick = onReplyPreviewClick,
                    onLongPress = onLongPress,
                    onPollVoteSubmit = onPollVoteSubmit,
                    onPollClose = onPollClose,
                    onSafetyDetailsClick = {
                        safetyWarning?.let(onSafetyDetailsClick)
                    },
                    innerMetadata =
                        if (showMetadata) {
                            { onPicture -> Metadata(message = message, onRetryClick = onRetryClick, onPicture = onPicture) }
                        } else {
                            null
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
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
}

private enum class PrimaryContent { EXPENSE_BOARD, POLL, VOICE, CONTACT, LOCATION, IMAGE_VIDEO, FILE, TEXT, NONE }

private fun MessageBubbleUi.primaryContent(showTextBubble: Boolean): PrimaryContent =
    when (parts.standalonePartOrNull()) {
        is ExpenseBoardUi -> PrimaryContent.EXPENSE_BOARD
        is PollUi -> PrimaryContent.POLL
        is VoiceUi -> PrimaryContent.VOICE
        is ContactUi -> PrimaryContent.CONTACT
        is LocationUi -> PrimaryContent.LOCATION
        null -> when {
            parts.any { it is ImageVideoUi } -> PrimaryContent.IMAGE_VIDEO
            parts.any { it is FileUi } -> PrimaryContent.FILE
            showTextBubble -> PrimaryContent.TEXT
            else -> PrimaryContent.NONE
        }
        else -> PrimaryContent.NONE
    }

@Composable
private fun BubbleBody(
    message: MessageBubbleUi,
    state: BubbleState,
    isSearchHighlighted: Boolean = false,
    safetyWarning: MessageSafetyWarningUi? = null,
    onAttachmentClick: (String) -> Unit = {},
    onContactClick: (SharedContact) -> Unit = {},
    onReplyPreviewClick: (String) -> Unit = {},
    onLongPress: () -> Unit = {},
    onPollVoteSubmit: (String, String, Set<String>) -> Unit = { _, _, _ -> },
    onPollClose: (String, String) -> Unit = { _, _ -> },
    onSafetyDetailsClick: () -> Unit = {},
    innerMetadata: BubbleMetadata? = null
) {
    val showTextBubble =
        message.parts.standalonePartOrNull() == null &&
            (state.text.isNotBlank() || state.isContentFailed || safetyWarning != null)

    val reply = message.reply
    val primaryContent = message.primaryContent(showTextBubble)

    fun replyFor(target: PrimaryContent) = reply?.takeIf { primaryContent == target }

    Column(
        verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.micro),
        horizontalAlignment = if (message.isMine) Alignment.End else Alignment.Start
    ) {
        BubbleContent(
            message = message,
            state = state,
            isSearchHighlighted = isSearchHighlighted,
            safetyWarning = safetyWarning,
            showTextBubble = showTextBubble,
            replyFor = ::replyFor,
            onAttachmentClick = onAttachmentClick,
            onContactClick = onContactClick,
            onReplyPreviewClick = onReplyPreviewClick,
            onLongPress = onLongPress,
            onPollVoteSubmit = onPollVoteSubmit,
            onPollClose = onPollClose,
            onSafetyDetailsClick = onSafetyDetailsClick,
            innerMetadata = innerMetadata
        )
    }
}

@Composable
private fun BubbleContent(
    message: MessageBubbleUi,
    state: BubbleState,
    isSearchHighlighted: Boolean,
    safetyWarning: MessageSafetyWarningUi?,
    showTextBubble: Boolean,
    replyFor: (PrimaryContent) -> MessageReplyUi?,
    onAttachmentClick: (String) -> Unit,
    onContactClick: (SharedContact) -> Unit,
    onReplyPreviewClick: (String) -> Unit,
    onLongPress: () -> Unit,
    onPollVoteSubmit: (String, String, Set<String>) -> Unit,
    onPollClose: (String, String) -> Unit,
    onSafetyDetailsClick: () -> Unit,
    innerMetadata: BubbleMetadata?
) {
    when (val part = message.parts.standalonePartOrNull()) {
        is ExpenseBoardUi -> {
            MessageBubbleSurface(
                message = message,
                state = state,
                isSearchHighlighted = isSearchHighlighted,
                reply = replyFor(PrimaryContent.EXPENSE_BOARD),
                metadata = innerMetadata,
                onReplyPreviewClick = onReplyPreviewClick,
                onLongPress = onLongPress
            ) {
                ExpenseBoardMessageContent(board = part)
            }
        }

        is PollUi -> {
            val pollPart = part
            MessageBubbleSurface(
                message = message,
                state = state,
                isSearchHighlighted = isSearchHighlighted,
                reply = replyFor(PrimaryContent.POLL),
                metadata = innerMetadata,
                onReplyPreviewClick = onReplyPreviewClick,
                onLongPress = onLongPress
            ) {
                PollMessageContent(
                    part = pollPart,
                    onVoteSubmit = { selectedOptionIds ->
                        onPollVoteSubmit(message.id, pollPart.id, selectedOptionIds)
                    },
                    onClosePoll = {
                        onPollClose(message.id, pollPart.id)
                    },
                    onMediaClick = { index ->
                        pollPart.images.getOrNull(index)?.let { onAttachmentClick(it.id) }
                    }
                )
            }
        }

        is VoiceUi -> {
            MessageBubbleSurface(
                message = message,
                state = state,
                isSearchHighlighted = isSearchHighlighted,
                reply = replyFor(PrimaryContent.VOICE),
                metadata = innerMetadata,
                onReplyPreviewClick = onReplyPreviewClick,
                onLongPress = onLongPress
            ) {
                VoiceMessageContent(part = part)
            }
        }

        is ContactUi -> {
            MessageBubbleSurface(
                message = message,
                state = state,
                hasInnerPadding = false,
                isSearchHighlighted = isSearchHighlighted,
                reply = replyFor(PrimaryContent.CONTACT),
                metadata = innerMetadata,
                onReplyPreviewClick = onReplyPreviewClick,
                onLongPress = onLongPress
            ) {
                ContactMessageBubbleBody(
                    contactPart = part,
                    onContactClick = onContactClick
                )
            }
        }

        is LocationUi -> {
            MessageBubbleSurface(
                message = message,
                state = state,
                hasInnerPadding = false,
                isSearchHighlighted = isSearchHighlighted,
                reply = replyFor(PrimaryContent.LOCATION),
                metadata = innerMetadata,
                onReplyPreviewClick = onReplyPreviewClick,
                onLongPress = onLongPress
            ) {
                LocationMessageBubbleBody(
                    locationPart = part,
                    onAttachmentClick = onAttachmentClick
                )
            }
        }

        null -> {
            val imageVideoParts = message.parts.filterIsInstance<ImageVideoUi>()
            val fileParts = message.parts.filterIsInstance<FileUi>()
            val textPart =
                message.parts.textPartOrNull()
                    ?: TextUi(
                        id = message.id,
                        text = state.text,
                        isContentFailed = state.isContentFailed
                    )
            // Several bubbles for one message: the last one carries the time and delivery.
            val last =
                when {
                    showTextBubble -> PrimaryContent.TEXT
                    fileParts.isNotEmpty() -> PrimaryContent.FILE
                    else -> PrimaryContent.IMAGE_VIDEO
                }
            if (imageVideoParts.isNotEmpty()) {
                MessageBubbleSurface(
                    message = message,
                    state = state,
                    isSearchHighlighted = isSearchHighlighted,
                    reply = replyFor(PrimaryContent.IMAGE_VIDEO),
                    metadata = innerMetadata?.takeIf { last == PrimaryContent.IMAGE_VIDEO },
                    metadataPlacement = MetadataPlacement.ON_PICTURE,
                    onReplyPreviewClick = onReplyPreviewClick,
                    onLongPress = onLongPress
                ) {
                    PhotoVideoMessageBubbleBody(
                        imageVideoParts = imageVideoParts,
                        onAttachmentClick = onAttachmentClick
                    )
                }
            }

            if (fileParts.isNotEmpty()) {
                MessageBubbleSurface(
                    message = message,
                    state = state,
                    isSearchHighlighted = isSearchHighlighted,
                    reply = replyFor(PrimaryContent.FILE),
                    metadata = innerMetadata?.takeIf { last == PrimaryContent.FILE },
                    onReplyPreviewClick = onReplyPreviewClick,
                    onLongPress = onLongPress
                ) {
                    FileMessageBubbleBody(
                        fileParts = fileParts
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
                    metadata = innerMetadata,
                    metadataPlacement = textPart.metadataPlacement(safetyWarning),
                    onReplyPreviewClick = onReplyPreviewClick,
                    onLongPress = onLongPress
                ) {
                    TextMessageBubbleBody(
                        textPart = textPart,
                        safetyWarning = safetyWarning,
                        onSafetyDetailsClick = onSafetyDetailsClick
                    )
                }
            }
        }

        else -> Unit
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
    metadata: BubbleMetadata? = null,
    metadataPlacement: MetadataPlacement = MetadataPlacement.BELOW,
    content: @Composable () -> Unit
) {
    val bubbleShapes = MaterialTheme.shapes.messageBubble
    val bubbleColor =
        rememberHighlightColor(
            isHighlighted = isSearchHighlighted,
            baseColor = state.bubbleColor,
            highlightColor = MaterialTheme.colorScheme.primary.copy(alpha = Alpha.Subtle)
        )
    val bubbleShape = if (message.isMine) bubbleShapes.mine else bubbleShapes.theirs

    val verticalPadding =
        if (hasInnerPadding) MaterialTheme.spacing.base else MaterialTheme.spacing.zero
    val horizontalPadding =
        if (hasInnerPadding) MaterialTheme.spacing.small else MaterialTheme.spacing.zero

    Surface(
        modifier = modifier
            .combinedClickable(
                onClick = {},
                onLongClick = onLongPress
            ),
        color = bubbleColor,
        contentColor = state.contentColor,
        shape = bubbleShape,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.40f))
            .takeIf { state.isContentFailed }
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
                    Modifier.padding(
                        horizontal = horizontalPadding,
                        vertical = verticalPadding
                    )
            ) {
                when {
                    metadata == null -> content()
                    metadataPlacement == MetadataPlacement.END_OF_TEXT -> TextWithTrailingMetadata(metadata = { metadata(false) }, content = content)
                    metadataPlacement == MetadataPlacement.ON_PICTURE ->
                        Box {
                            content()
                            MetadataPill(modifier = Modifier.align(Alignment.BottomEnd)) { metadata(true) }
                        }
                    hasInnerPadding ->
                        Column {
                            content()
                            Box(modifier = Modifier.align(Alignment.End).padding(top = MaterialTheme.spacing.micro)) { metadata(false) }
                        }
                    else -> content()
                }
            }
            // Content that fills the bubble to its edges gets the row inset below it, still inside the bubble.
            metadata?.takeIf { !hasInnerPadding && metadataPlacement == MetadataPlacement.BELOW }?.let { row ->
                Box(
                    modifier =
                        Modifier
                            .align(Alignment.End)
                            .padding(horizontal = MaterialTheme.spacing.small, vertical = MaterialTheme.spacing.micro)
                ) { row(false) }
            }
        }
    }
}

/** The time and delivery of a message, drawn light on a dark pill when it sits on a picture. */
internal typealias BubbleMetadata = @Composable (onPicture: Boolean) -> Unit

/** Where a bubble shows its time and delivery; every place is inside the bubble, which grows when they do not fit. */
internal enum class MetadataPlacement {
    /** At the end of the last line of text, or on a line of its own when that line is full. */
    END_OF_TEXT,

    /** In the corner of a photo or video. */
    ON_PICTURE,

    /** At the end, under the content. */
    BELOW
}

/**
 * The text keeps room for the time and delivery at the end of its last line ([LocalTrailingMetadataSize]); the text
 * wraps the room to a new line when the last one is full, and the time sits in the bottom corner either way.
 */
@Composable
private fun TextWithTrailingMetadata(
    metadata: @Composable () -> Unit,
    content: @Composable () -> Unit
) {
    val density = LocalDensity.current
    // Matches the text's own padding in TextMessageBubbleBody, so the time lines up with the last line.
    val insetSize = MaterialTheme.spacing.micro
    SubcomposeLayout { constraints ->
        val loose = constraints.copy(minWidth = 0, minHeight = 0)
        val row = subcompose(TrailingSlot.METADATA, metadata).map { measurable -> measurable.measure(loose) }
        val rowWidth = row.maxOfOrNull { it.width } ?: 0
        val rowHeight = row.maxOfOrNull { it.height } ?: 0
        val inset = insetSize.roundToPx()
        val room = with(density) { DpSize((rowWidth + inset).toDp(), rowHeight.toDp()) }
        val body =
            subcompose(TrailingSlot.CONTENT) {
                CompositionLocalProvider(LocalTrailingMetadataSize provides room) { content() }
            }.map { measurable -> measurable.measure(loose) }
        val width = maxOf(body.maxOfOrNull { it.width } ?: 0, rowWidth + inset * 2)
        val height = maxOf(body.maxOfOrNull { it.height } ?: 0, rowHeight + inset * 2)
        layout(width, height) {
            body.forEach { placeable -> placeable.place(0, 0) }
            row.forEach { placeable -> placeable.place(width - placeable.width - inset, height - placeable.height - inset) }
        }
    }
}

private enum class TrailingSlot { METADATA, CONTENT }

@Composable
private fun MetadataPill(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Box(
        modifier =
            modifier
                .padding(MaterialTheme.spacing.micro)
                .background(Color.Black.copy(alpha = Alpha.MessageBubble.metadataPill), MaterialTheme.shapes.circle)
                .padding(horizontal = MaterialTheme.spacing.base, vertical = MaterialTheme.spacing.micro / 2)
    ) {
        content()
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
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        shape = MaterialTheme.shapes.large,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
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

/**
 * The time, with an open lock before it when the message was not end-to-end encrypted, and for own messages the
 * delivery dots. A message that failed shows them in red, and tapping them sends it again.
 */
@Composable
private fun Metadata(
    message: MessageBubbleUi,
    onRetryClick: () -> Unit,
    modifier: Modifier = Modifier,
    onPicture: Boolean = false
) {
    val showsDelivery = message.isMine && message.deliveryStatus != MessageDeliveryStatus.NOT_APPLICABLE
    val failed = showsDelivery && message.deliveryStatus == MessageDeliveryStatus.FAILED
    val retryLabel = stringResource(Res.string.feature_chats_failed_tap_to_retry)
    val timeColor =
        when {
            failed -> MaterialTheme.colorScheme.error
            onPicture -> Color.White
            else -> MaterialTheme.colorScheme.onSurfaceVariant
        }
    Row(
        modifier =
            modifier
                .clickable(enabled = failed, onClickLabel = retryLabel, role = Role.Button, onClick = onRetryClick),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.micro)
    ) {
        if (message.isReadableWithoutEncryption()) {
            Icon(
                imageVector = Icons.Default.LockOpen,
                contentDescription = stringResource(Res.string.feature_chats_not_encrypted),
                modifier = Modifier.size(Dimens.MessageBubble.iconSize),
                tint = MaterialTheme.colorScheme.warning
            )
        }

        if (message.time.isNotBlank()) {
            Text(
                text = message.time,
                style = MaterialTheme.typography.labelSmall,
                color = timeColor
            )
        }

        if (showsDelivery) {
            SparrowDeliveryDots(state = message.deliveryStatus.toDotsState(), description = deliveryDescription(message.deliveryStatus))
            DeliveryProgress(message = message)
        }
    }
}

/** Unreadable messages say so in the bubble itself; only a readable one sent without encryption gets the lock. */
private fun MessageBubbleUi.isReadableWithoutEncryption(): Boolean =
    contentStatus == MessageContentStatus.READABLE && security != MessageSecurity.END_TO_END_ENCRYPTED

/** Waiting for the contact's approval looks like waiting for a route: nothing went out yet. */
private fun MessageDeliveryStatus.toDotsState(): DeliveryDotsState =
    when (this) {
        MessageDeliveryStatus.WAITING_FOR_AUTHORIZATION, MessageDeliveryStatus.QUEUED, MessageDeliveryStatus.NOT_APPLICABLE -> DeliveryDotsState.QUEUED
        MessageDeliveryStatus.SENDING -> DeliveryDotsState.SENDING
        MessageDeliveryStatus.SENT -> DeliveryDotsState.SENT
        MessageDeliveryStatus.DELIVERED -> DeliveryDotsState.DELIVERED
        MessageDeliveryStatus.READ -> DeliveryDotsState.READ
        MessageDeliveryStatus.FAILED -> DeliveryDotsState.FAILED
    }

@Composable
private fun deliveryDescription(status: MessageDeliveryStatus): String =
    when (status) {
        MessageDeliveryStatus.WAITING_FOR_AUTHORIZATION -> stringResource(Res.string.feature_chats_waiting_for_authorization)
        MessageDeliveryStatus.QUEUED, MessageDeliveryStatus.NOT_APPLICABLE -> stringResource(Res.string.feature_chats_queued)
        MessageDeliveryStatus.SENDING -> stringResource(Res.string.feature_chats_sending)
        MessageDeliveryStatus.SENT -> stringResource(Res.string.feature_chats_sent)
        MessageDeliveryStatus.DELIVERED -> stringResource(Res.string.feature_chats_delivered)
        MessageDeliveryStatus.READ -> stringResource(Res.string.feature_chats_read)
        MessageDeliveryStatus.FAILED -> stringResource(Res.string.feature_chats_failed_tap_to_retry)
    }

@Composable
private fun DeliveryProgress(message: MessageBubbleUi) {
    val progress = message.deliveryProgress
    if (progress.recipientCount <= 1) return

    // Before anyone received it the dots say enough.
    val text =
        when {
            progress.readCount > 0 -> "${progress.readCount}/${progress.recipientCount}"
            progress.deliveredCount > 0 -> "${progress.deliveredCount}/${progress.recipientCount}"
            else -> return
        }

    Text(
        text = text,
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onBackground.copy(alpha = Alpha.OpaqueText)
    )
}

@Composable
private fun bubbleState(message: MessageBubbleUi): BubbleState =
    when (message.contentStatus) {
        MessageContentStatus.READABLE ->
            BubbleState(
                text = message.textOrNull() ?: "",
                isContentFailed = false,
                bubbleColor =
                    if (message.isMine) {
                        MaterialTheme.colorScheme.primaryContainer
                            .copy(alpha = Alpha.MessageBubble.mine)
                            .compositeOver(MaterialTheme.colorScheme.background)
                    } else {
                        MaterialTheme.colorScheme.surfaceContainer
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
                    parts = listOf(
                        TextUi(
                            id = "preview-text",
                            text = "Encrypted message",
                            isContentFailed = false
                        )
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
                    parts = listOf(
                        TextUi(
                            id = "preview-text",
                            text = "Test message",
                            isContentFailed = false
                        ),
                        FileUi(
                            id = "preview-file",
                            mimeType = "application/pdf",
                            byteSize = 0,
                            fileName = "test.pdf"
                        ),
                        ImageUi(
                            id = "preview-image-1",
                            mimeType = "image/jpeg",
                            byteSize = 0
                        ),
                        VideoUi(
                            id = "preview-video",
                            mimeType = "video/mp4",
                            byteSize = 0
                        )
                    )
                ),
            onRetryClick = {},
            onAttachmentClick = {}
        )
    }
}

/** At the end of the last line only when the text ends in plain words: not after a link preview, a warning or an error. */
private fun TextUi.metadataPlacement(safetyWarning: MessageSafetyWarningUi?): MetadataPlacement =
    when {
        safetyWarning != null || isContentFailed -> MetadataPlacement.BELOW
        text.toTextContentParts().lastOrNull() is TextContentPart.Text -> MetadataPlacement.END_OF_TEXT
        else -> MetadataPlacement.BELOW
    }
