package com.cbgm.sparrow.feature.voice.presentation.composer

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
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
import com.cbgm.sparrow.feature.voice.domain.model.VoiceComposerPhase
import com.cbgm.sparrow.feature.voice.presentation.component.VoiceWaveform
import com.cbgm.sparrow.feature.voice.presentation.component.formatVoiceDuration
import com.cbgm.sparrow.feature.voice.presentation.composer.model.VoiceComposerUiState
import com.cbgm.sparrow.resources.Res
import com.cbgm.sparrow.resources.feature_voice_tap_to_record
import org.jetbrains.compose.resources.stringResource

@Composable
fun VoiceComposer(
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
                .clip(MaterialTheme.shapes.medium)
                .background(MaterialTheme.colorScheme.surfaceContainerLow)
                .border(
                    Dimens.Base.borderStrokeWidth,
                    MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f),
                    MaterialTheme.shapes.medium
                )
                .padding(horizontal = MaterialTheme.spacing.small),
        verticalAlignment = Alignment.CenterVertically
    ) {
        SwitchButton(
            isEnabled = inputEnabled,
            state = state,
            onPlayPauseClick = onPlayPauseClick,
            onStopClick = onStopClick,
            onRecordClick = onRecordClick
        )

        if (state.phase == VoiceComposerPhase.READY) {
            Text(
                text = stringResource(Res.string.feature_voice_tap_to_record),
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
                animated = state.phase == VoiceComposerPhase.RECORDING,
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

@Composable
private fun SwitchButton(
    state: VoiceComposerUiState,
    isEnabled: Boolean,
    onStopClick: () -> Unit,
    onRecordClick: () -> Unit,
    onPlayPauseClick: () -> Unit
) {
    val action =
        when (state.phase) {
            VoiceComposerPhase.READY -> VoiceAction.Record
            VoiceComposerPhase.RECORDING -> VoiceAction.Stop
            VoiceComposerPhase.RECORDED -> if (state.isPlaying) VoiceAction.Pause else VoiceAction.Play
        }

    Box(
        modifier =
            Modifier
                .size(Dimens.MessageInput.sendButtonWidth)
                .clip(CircleShape)
                .clickable(enabled = isEnabled) {
                    when (action) {
                        VoiceAction.Record -> onRecordClick()
                        VoiceAction.Stop -> onStopClick()
                        VoiceAction.Play,
                        VoiceAction.Pause -> onPlayPauseClick()
                    }
                },
        contentAlignment = Alignment.Center
    ) {
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
                if (isEnabled) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.primary.copy(alpha = Alpha.Disabled)
                },
            modifier = Modifier.size(Dimens.MessageInput.iconSize)
        )
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
private fun VoiceComposerReadyPreview() {
    SparrowTheme {
        VoiceComposer(
            state = VoiceComposerUiState(),
            inputEnabled = true,
            onRecordClick = {},
            onStopClick = {},
            onPlayPauseClick = {}
        )
    }
}

@Preview
@Composable
private fun VoiceComposerRecordingPreview() {
    SparrowTheme {
        VoiceComposer(
            state =
                VoiceComposerUiState(
                    phase = VoiceComposerPhase.RECORDING,
                    durationMilliseconds = 12_000L
                ),
            inputEnabled = true,
            onRecordClick = {},
            onStopClick = {},
            onPlayPauseClick = {}
        )
    }
}

@Preview
@Composable
private fun VoiceComposerRecordedPreview() {
    SparrowTheme {
        VoiceComposer(
            state =
                VoiceComposerUiState(
                    phase = VoiceComposerPhase.RECORDED,
                    durationMilliseconds = 18_000L,
                    playbackProgress = 0.38f
                ),
            inputEnabled = true,
            onRecordClick = {},
            onStopClick = {},
            onPlayPauseClick = {}
        )
    }
}
