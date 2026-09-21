package com.cbgm.sparrow.feature.chats.presentation.common.composer.mapper

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Reply
import androidx.compose.material.icons.filled.Edit
import androidx.compose.runtime.Composable
import com.cbgm.sparrow.feature.chats.domain.model.IndicatorType
import com.cbgm.sparrow.feature.chats.domain.model.MessageComposerAvailability
import com.cbgm.sparrow.feature.chats.presentation.common.composer.model.ComposerAvailabilityUi
import com.cbgm.sparrow.feature.chats.presentation.common.composer.model.ComposerPreviewUi
import com.cbgm.sparrow.feature.chats.presentation.common.composer.model.IndicatorUiState
import com.cbgm.sparrow.feature.chats.presentation.common.composer.model.IndicatorUiType
import com.cbgm.sparrow.feature.chats.presentation.common.composer.model.MessageComposerUiState
import com.cbgm.sparrow.feature.chats.presentation.common.composer.model.MessageInputState
import com.cbgm.sparrow.resources.Res
import com.cbgm.sparrow.resources.feature_chats_edit_message
import com.cbgm.sparrow.resources.feature_chats_reply_you
import org.jetbrains.compose.resources.stringResource

fun MessageComposerAvailability.toComposerAvailabilityUi() = ComposerAvailabilityUi(
    isInputEnabled = isInputEnabled,
    isSendEnabled = isSendEnabled,
    canAddAttachment = canAddAttachment
)

fun IndicatorType.toIndicatorUiType() = when (this) {
    IndicatorType.NONE -> IndicatorUiType.NONE
    IndicatorType.TYPING -> IndicatorUiType.TYPING
    IndicatorType.VOICE -> IndicatorUiType.VOICE
}

@Composable
fun MessageComposerUiState.toComposerPreviewUi(): ComposerPreviewUi? = when {
    editingMessageId != null -> ComposerPreviewUi(
        type = ComposerPreviewUi.Type.EDIT,
        icon = Icons.Default.Edit,
        iconText = stringResource(Res.string.feature_chats_edit_message)
    )
    replyTo != null -> ComposerPreviewUi(
        type = ComposerPreviewUi.Type.REPLY,
        icon = Icons.AutoMirrored.Filled.Reply,
        iconText = replyTo.senderName.takeUnless { it.isNullOrBlank() }
            ?: stringResource(Res.string.feature_chats_reply_you),
        additionalText = " - ${replyTo.previewText.orEmpty()}"
    )
    else -> null
}

fun MessageComposerUiState.toMessageInputState(
    indicatorState: IndicatorUiState,
    composerPreview: ComposerPreviewUi?
) = MessageInputState(
    messageText = messageText,
    composerPreview = composerPreview,
    indicatorType = indicatorState.type,
    contactName = indicatorState.displayName,
    isInputEnabled = availability.isInputEnabled,
    isSendEnabled = availability.isSendEnabled,
    isLocationInProgress = isLocationInProgress,
    selectedMedia = selectedMedia,
    isGalleryEnabled = availability.canAddAttachment,
    isCameraEnabled = availability.canAddAttachment,
    isFileEnabled = availability.canAddAttachment
)
