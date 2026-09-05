package com.cbgm.sparrow.feature.chats.presentation.component

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.cbgm.sparrow.core.ui.theme.Alpha
import com.cbgm.sparrow.core.ui.theme.Dimens
import com.cbgm.sparrow.core.ui.theme.SparrowTheme
import com.cbgm.sparrow.core.ui.theme.spacing
import com.cbgm.sparrow.feature.chats.presentation.component.model.MessagePartUi

@Composable
internal fun VoiceMessageBubbleBody(
    voice: MessagePartUi.Voice,
    onPlayPauseClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val progress =
        if (voice.durationMilliseconds > 0L) {
            voice.playbackPositionMilliseconds.toFloat() / voice.durationMilliseconds.toFloat()
        } else {
            0f
        }

    Row(
        modifier = modifier.padding(horizontal = MaterialTheme.spacing.small),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(
            onClick = onPlayPauseClick,
            modifier = Modifier.size(Dimens.MessageInput.sendButtonWidth)
        ) {
            Icon(
                imageVector = if (voice.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
        }

        VoiceWaveform(
            waveform = voice.waveform,
            progress = progress,
            playedColor = MaterialTheme.colorScheme.primary,
            remainingColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = Alpha.Subtle),
            modifier =
                Modifier
                    .weight(1f)
                    .height(Dimens.MessageInput.buttonHeight)
                    .padding(horizontal = MaterialTheme.spacing.small)
        )

        Text(
            text = formatVoiceDuration(voice.durationMilliseconds),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Preview
@Composable
private fun VoiceMessageBubbleBodyPreview() {
    SparrowTheme {
        VoiceMessageBubbleBody(
            voice =
                MessagePartUi.Voice(
                    durationMilliseconds = 18_000L,
                    playbackPositionMilliseconds = 6_000L
                ),
            onPlayPauseClick = {}
        )
    }
}
