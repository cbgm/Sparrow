package com.cbgm.sparrow.feature.chats.presentation.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.tooling.preview.Preview
import com.cbgm.sparrow.core.ui.theme.Alpha
import com.cbgm.sparrow.core.ui.theme.Dimens
import com.cbgm.sparrow.core.ui.theme.SparrowTheme
import com.cbgm.sparrow.core.ui.theme.spacing
import com.cbgm.sparrow.feature.chats.presentation.component.model.VoiceComposerPhase
import com.cbgm.sparrow.feature.chats.presentation.component.model.VoiceComposerUiState
import com.cbgm.sparrow.resources.Res
import com.cbgm.sparrow.resources.feature_chats_voice_tap_to_record
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun VoiceMessageInput(
    state: VoiceComposerUiState,
    inputEnabled: Boolean,
    onRecordClick: () -> Unit,
    onStopClick: () -> Unit,
    onPlayPauseClick: () -> Unit,
    onSendClick: () -> Unit,
    onCancelClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .windowInsetsPadding(WindowInsets.navigationBars)
                .imePadding(),
        verticalAlignment = Alignment.Bottom
    ) {
        VoiceComposerContent(
            state = state,
            inputEnabled = inputEnabled,
            onRecordClick = onRecordClick,
            onStopClick = onStopClick,
            onPlayPauseClick = onPlayPauseClick,
            modifier = Modifier.weight(1f)
        )

        SendButton(
            buttonWidth = Dimens.MessageInput.sendButtonWidth,
            buttonHeight = Dimens.MessageInput.buttonHeight,
            isRound = false,
            onSendClick = onSendClick,
            enabled = inputEnabled && state.phase == VoiceComposerPhase.RECORDED,
            isEditing = false,
            modifier = Modifier.align(Alignment.CenterVertically)
        )

        RoundedInputButton(
            modifier = Modifier
                .padding(start = MaterialTheme.spacing.base)
                .align(Alignment.CenterVertically),
            onClick = onCancelClick,
            icon = Icons.Default.Close
        )
    }
}

@Composable
private fun VoiceComposerContent(
    state: VoiceComposerUiState,
    inputEnabled: Boolean,
    onRecordClick: () -> Unit,
    onStopClick: () -> Unit,
    onPlayPauseClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier =
            modifier
                .height(Dimens.MessageInput.composerHeight)
                .clip(MaterialTheme.shapes.extraSmall)
                .background(MaterialTheme.colorScheme.surfaceContainer)
                .padding(horizontal = MaterialTheme.spacing.small),
        verticalAlignment = Alignment.CenterVertically
    ) {
        val action =
            when (state.phase) {
                VoiceComposerPhase.READY -> VoiceAction.Record
                VoiceComposerPhase.RECORDING -> VoiceAction.Stop
                VoiceComposerPhase.RECORDED ->
                    if (state.isPlaying) VoiceAction.Pause else VoiceAction.Play
            }

        Icon(
            imageVector =
                when (action) {
                    VoiceAction.Record -> Icons.Default.Mic
                    VoiceAction.Stop -> Icons.Default.Stop
                    VoiceAction.Play -> Icons.Default.PlayArrow
                    VoiceAction.Pause -> Icons.Default.Pause
                },
            contentDescription = null,
            tint =
                if (inputEnabled) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.primary.copy(alpha = Alpha.Disabled)
                },
            modifier =
                Modifier
                    .size(Dimens.MessageInput.iconSize)
                    .clickable(enabled = inputEnabled) {
                        when (action) {
                            VoiceAction.Record -> onRecordClick()
                            VoiceAction.Stop -> onStopClick()
                            VoiceAction.Play,
                            VoiceAction.Pause -> onPlayPauseClick()
                        }
                    }
        )

        if (state.phase == VoiceComposerPhase.READY) {
            Text(
                text = stringResource(Res.string.feature_chats_voice_tap_to_record),
                modifier = Modifier.padding(start = MaterialTheme.spacing.small),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            VoiceWaveform(
                waveform = state.waveform,
                progress = state.playbackProgress,
                playedColor = MaterialTheme.colorScheme.primary,
                remainingColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = Alpha.Subtle),
                modifier =
                    Modifier
                        .weight(1f)
                        .height(Dimens.MessageInput.iconSize)
                        .padding(horizontal = MaterialTheme.spacing.small)
            )

            Text(
                text = formatVoiceDuration(state.durationMilliseconds),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

private enum class VoiceAction {
    Record,
    Stop,
    Play,
    Pause
}

@Preview
@Composable
private fun VoiceMessageInputRecordedPreview() {
    SparrowTheme {
        VoiceMessageInput(
            state =
                VoiceComposerUiState(
                    phase = VoiceComposerPhase.RECORDED,
                    durationMilliseconds = 18_000L,
                    playbackProgress = 0.38f
                ),
            inputEnabled = true,
            onRecordClick = {},
            onStopClick = {},
            onPlayPauseClick = {},
            onSendClick = {},
            onCancelClick = {}
        )
    }
}
