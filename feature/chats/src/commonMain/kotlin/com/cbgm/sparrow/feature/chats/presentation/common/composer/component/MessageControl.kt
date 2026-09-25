package com.cbgm.sparrow.feature.chats.presentation.common.composer.component

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Reply
import androidx.compose.material.icons.filled.Close
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cbgm.sparrow.core.ui.theme.Dimens
import com.cbgm.sparrow.core.ui.theme.SparrowTheme
import com.cbgm.sparrow.core.ui.theme.spacing
import com.cbgm.sparrow.feature.attachments.presentation.component.AttachmentBar
import com.cbgm.sparrow.feature.chats.presentation.common.composer.model.ComposerPreviewUi
import com.cbgm.sparrow.feature.chats.presentation.common.composer.model.IndicatorUiType
import com.cbgm.sparrow.feature.chats.presentation.common.composer.model.MessageInputActions
import com.cbgm.sparrow.feature.chats.presentation.common.composer.model.MessageInputState
import com.cbgm.sparrow.feature.media.presentation.component.MediaSelectionPreview
import com.cbgm.sparrow.feature.media.presentation.component.previewMediaSelections
import com.cbgm.sparrow.feature.voice.domain.model.VoiceComposerPhase
import com.cbgm.sparrow.feature.voice.presentation.composer.VoiceComposer
import com.cbgm.sparrow.feature.voice.presentation.composer.VoiceComposerViewModel
import com.cbgm.sparrow.resources.Res
import com.cbgm.sparrow.resources.feature_chats_chat_recording_voice
import com.cbgm.sparrow.resources.feature_chats_chat_typing
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun MessageControl(
    state: MessageInputState,
    actions: MessageInputActions,
    containerColor: Color,
    modifier: Modifier = Modifier
) {
    var isAttachmentBarVisible by remember { mutableStateOf(false) }
    var locationProgressWasVisible by remember { mutableStateOf(false) }
    var isVoiceComposerVisible by remember { mutableStateOf(false) }

    val isEditing = state.composerPreview?.type == ComposerPreviewUi.Type.EDIT

    LaunchedEffect(isEditing) {
        if (isEditing) {
            isAttachmentBarVisible = false
            isVoiceComposerVisible = false
        }
    }

    LaunchedEffect(state.isLocationInProgress) {
        if (state.isLocationInProgress) {
            locationProgressWasVisible = true
        } else if (locationProgressWasVisible) {
            isAttachmentBarVisible = false
            locationProgressWasVisible = false
        }
    }

    Surface(
        modifier = modifier.fillMaxWidth(),
        color = containerColor
    ) {
        val basePaddingHorizontal = MaterialTheme.spacing.base

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .windowInsetsPadding(WindowInsets.navigationBars)
                .imePadding()
        ) {
            Text(
                text =
                    when (state.indicatorType) {
                        IndicatorUiType.VOICE ->
                            stringResource(
                                Res.string.feature_chats_chat_recording_voice,
                                state.contactName
                            )

                        IndicatorUiType.TYPING ->
                            stringResource(
                                Res.string.feature_chats_chat_typing,
                                state.contactName
                            )

                        IndicatorUiType.NONE -> ""
                    },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        horizontal = basePaddingHorizontal + MaterialTheme.spacing.base.times(6),
                        vertical = MaterialTheme.spacing.micro
                    ),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onBackground,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            state.composerPreview?.let { preview ->
                ComposerPreview(
                    previewUi = preview,
                    onCancel = actions.onCancelPreview,
                    modifier = Modifier.padding(bottom = MaterialTheme.spacing.base)
                )
            }
            if (!isVoiceComposerVisible) {
                MediaSelectionPreview(
                    media = state.selectedMedia,
                    onClick = actions.onSelectionClick,
                    onRemove = actions.onMediaRemove,
                    modifier = Modifier.padding(
                        start = basePaddingHorizontal,
                        end = basePaddingHorizontal,
                        bottom = MaterialTheme.spacing.micro
                    )
                )

                MessageInput(
                    value = state.messageText,
                    onValueChange = actions.onValueChange,
                    onSendClick = actions.onSendClick,
                    onVoiceClick = {
                        if (!isEditing) {
                            isAttachmentBarVisible = false
                            isVoiceComposerVisible = true
                        }
                    },
                    inputEnabled = state.isInputEnabled,
                    sendEnabled = state.isSendEnabled,
                    hasAttachments = state.selectedMedia.isNotEmpty(),
                    attachmentsEnabled = !isEditing,
                    isEditing = isEditing,
                    onAttachmentClick = { isAttachmentBarVisible = !isAttachmentBarVisible },
                    isAttachmentVisible = isAttachmentBarVisible,
                    modifier = Modifier.padding(horizontal = basePaddingHorizontal)
                )
            } else {
                VoiceComposerContent(
                    state = state,
                    actions = actions,
                    onDismiss = { isVoiceComposerVisible = false },
                    modifier = Modifier.padding(horizontal = basePaddingHorizontal)
                )
            }

            if (isAttachmentBarVisible && !isVoiceComposerVisible) {
                AttachmentBar(
                    onClickCamera = {
                        isAttachmentBarVisible = false
                        actions.onClickCamera()
                    },
                    onClickFile = {
                        isAttachmentBarVisible = false
                        actions.onClickFile()
                    },
                    onClickGallery = {
                        isAttachmentBarVisible = false
                        actions.onClickGallery()
                    },
                    onClickContact = {
                        isAttachmentBarVisible = false
                        actions.onClickContact()
                    },
                    onClickLocation = {
                        actions.onClickLocation()
                    },
                    isGalleryEnabled = state.isGalleryEnabled,
                    isCameraEnabled = state.isCameraEnabled,
                    isFileEnabled = state.isFileEnabled,
                    isLocationInProgress = state.isLocationInProgress,
                    modifier = Modifier.padding(horizontal = basePaddingHorizontal)
                )
            }
        }
    }
}

@Composable
private fun VoiceComposerContent(
    state: MessageInputState,
    actions: MessageInputActions,
    onDismiss: () -> Unit,
    modifier: Modifier
) {
    val voiceViewModel = koinViewModel<VoiceComposerViewModel>()
    val voiceState by voiceViewModel.uiState.collectAsStateWithLifecycle()

    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .padding(vertical = MaterialTheme.spacing.micro),
        verticalAlignment = Alignment.Bottom
    ) {
        VoiceComposer(
            state = voiceState,
            inputEnabled = state.isInputEnabled,
            onRecordClick = voiceViewModel::startRecording,
            onStopClick = voiceViewModel::stopRecording,
            onPlayPauseClick = voiceViewModel::togglePreview,
            modifier = Modifier.weight(1f)
        )

        SendButton(
            buttonWidth = Dimens.MessageInput.sendButtonWidth,
            buttonHeight = Dimens.MessageInput.buttonHeight,
            isRound = false,
            onSendClick = {
                actions.onVoiceSendClick()
                onDismiss()
            },
            enabled =
                state.isInputEnabled &&
                    voiceState.phase == VoiceComposerPhase.RECORDED,
            isEditing = false,
            modifier = Modifier.align(Alignment.CenterVertically)
        )

        RoundedInputButton(
            modifier =
                Modifier
                    .padding(start = MaterialTheme.spacing.base)
                    .align(Alignment.CenterVertically),
            onClick = {
                voiceViewModel.cancel()
                onDismiss()
            },
            icon = Icons.Default.Close
        )
    }
}

@Preview
@Composable
private fun MessageControlPreview() {
    SparrowTheme {
        MessageControl(
            containerColor = MaterialTheme.colorScheme.background,
            state = MessageInputState(
                indicatorType = IndicatorUiType.NONE,
                contactName = "Chris",
                messageText = "Here are the files",
                composerPreview =
                    ComposerPreviewUi(
                        icon = Icons.AutoMirrored.Filled.Reply,
                        iconText = "Reply",
                        additionalText = "The original message should only appear as a short excerpt in the composer",
                        type = ComposerPreviewUi.Type.REPLY
                    ),
                isInputEnabled = true,
                isSendEnabled = true,
                selectedMedia = previewMediaSelections(),
                isGalleryEnabled = true,
                isCameraEnabled = true,
                isFileEnabled = true,
                isLocationInProgress = false
            ),
            actions = MessageInputActions(
                onValueChange = {},
                onSendClick = {},
                onCancelPreview = {},
                onSelectionClick = {},
                onMediaRemove = {},
                onClickCamera = {},
                onClickFile = {},
                onClickGallery = {},
                onClickContact = {},
                onClickLocation = {}
            )
        )
    }
}
