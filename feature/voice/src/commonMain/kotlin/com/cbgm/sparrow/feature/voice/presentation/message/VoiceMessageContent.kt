package com.cbgm.sparrow.feature.voice.presentation.message

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cbgm.sparrow.core.ui.theme.Alpha
import com.cbgm.sparrow.core.ui.theme.Dimens
import com.cbgm.sparrow.core.ui.theme.SparrowTheme
import com.cbgm.sparrow.core.ui.theme.spacing
import com.cbgm.sparrow.feature.voice.domain.model.VoiceMessageTarget
import com.cbgm.sparrow.feature.voice.domain.model.VoiceTranscript
import com.cbgm.sparrow.feature.voice.domain.model.VoiceTranscriptCue
import com.cbgm.sparrow.feature.voice.domain.model.VoiceTranscriptionState
import com.cbgm.sparrow.feature.voice.presentation.component.VoiceWaveform
import com.cbgm.sparrow.feature.voice.presentation.component.formatVoiceDuration
import com.cbgm.sparrow.resources.Res
import com.cbgm.sparrow.resources.feature_voice_transcribe
import com.cbgm.sparrow.resources.feature_voice_transcribing
import com.cbgm.sparrow.resources.feature_voice_transcription_downloading
import com.cbgm.sparrow.resources.feature_voice_transcription_preparing
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf
import kotlin.math.abs
import kotlin.math.floor
import kotlin.math.roundToInt

@Composable
fun VoiceMessageContent(
    target: VoiceMessageTarget,
    modifier: Modifier = Modifier
) {
    val viewModel =
        koinViewModel<VoiceMessageViewModel>(key = target.stableKey) {
            parametersOf(target)
        }
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    VoiceMessageContentBody(
        durationMilliseconds = target.durationMilliseconds,
        playbackPositionMilliseconds = uiState.playbackPositionMilliseconds,
        isPlaying = uiState.isPlaying,
        waveform = emptyList(),
        transcript = uiState.transcript,
        transcriptionEnabled = uiState.transcriptionEnabled,
        transcriptionState = uiState.transcriptionState,
        onPlayPauseClick = viewModel::togglePlayback,
        onTranscribeClick = viewModel::transcribe,
        onSeekStart = viewModel::startScrub,
        onSeekEnd = viewModel::finishScrub,
        modifier = modifier
    )
}

@Composable
private fun VoiceMessageContentBody(
    durationMilliseconds: Long,
    playbackPositionMilliseconds: Long,
    isPlaying: Boolean,
    waveform: List<Float>,
    transcript: VoiceTranscript?,
    transcriptionEnabled: Boolean,
    transcriptionState: VoiceTranscriptionState,
    onPlayPauseClick: () -> Unit,
    onTranscribeClick: () -> Unit,
    onSeekStart: () -> Unit,
    onSeekEnd: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val scrubState =
        rememberVoiceScrubState(
            playbackPositionMilliseconds = playbackPositionMilliseconds,
            durationMilliseconds = durationMilliseconds,
            isPlaying = isPlaying
        )

    Column(
        modifier = modifier.padding(
            end = MaterialTheme.spacing.base
        ),
        verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.micro)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.small)
        ) {
            IconButton(
                onClick = onPlayPauseClick,
                modifier = Modifier
                    .size(Dimens.MessageInput.sendButtonWidth)
                    .clip(MaterialTheme.shapes.large)
            ) {
                Icon(
                    imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
            }

            VoiceWaveform(
                waveform = waveform,
                progress = scrubState.progress,
                playedColor = MaterialTheme.colorScheme.primary,
                remainingColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = Alpha.Subtle),
                modifier =
                    Modifier
                        .weight(1f)
                        .height(Dimens.MessageInput.buttonHeight)
                        .padding(
                            end = MaterialTheme.spacing.small,
                            start = MaterialTheme.spacing.base
                        ),
                onScrubStart = { scrubProgress ->
                    scrubState.start(scrubProgress)
                    onSeekStart()
                },
                onScrub = scrubState::update,
                onScrubEnd = { scrubProgress ->
                    onSeekEnd(scrubState.end(scrubProgress))
                }
            )

            val displayedDurationMilliseconds =
                if (scrubState.displayPositionMilliseconds > 0L) {
                    scrubState.displayPositionMilliseconds.coerceAtMost(durationMilliseconds)
                } else {
                    durationMilliseconds
                }

            Text(
                text = formatVoiceDuration(displayedDurationMilliseconds),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        if (transcriptionEnabled) {
            when {
                transcriptionState is VoiceTranscriptionState.Downloading ||
                    transcriptionState is VoiceTranscriptionState.Preparing ||
                    transcriptionState is VoiceTranscriptionState.Transcribing ->
                    TranscriptionHint(transcriptionState)

                !transcript?.text.isNullOrBlank() ->
                    TranscriptScroll(
                        transcript = requireNotNull(transcript),
                        isPlaying = isPlaying,
                        durationMilliseconds = durationMilliseconds,
                        displayPlaybackPositionMilliseconds = scrubState.displayPositionMilliseconds,
                        isScrubbing = scrubState.isScrubbing
                    )

                else ->
                    Text(
                        text = stringResource(Res.string.feature_voice_transcribe),
                        modifier = Modifier.padding(start = MaterialTheme.spacing.base)
                            .clickable(onClick = onTranscribeClick),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
            }
            Spacer(Modifier.height(MaterialTheme.spacing.micro))
        }
    }
}

@Composable
private fun TranscriptionHint(state: VoiceTranscriptionState) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.small),
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(start = MaterialTheme.spacing.base)
    ) {
        CircularProgressIndicator(
            modifier = Modifier.size(Dimens.MessageBubble.progressSize),
            strokeWidth = Dimens.MessageBubble.progressStrokeWidth
        )
        Text(
            text =
                when (state) {
                    VoiceTranscriptionState.Downloading ->
                        stringResource(Res.string.feature_voice_transcription_downloading)

                    VoiceTranscriptionState.Preparing ->
                        stringResource(Res.string.feature_voice_transcription_preparing)

                    is VoiceTranscriptionState.Transcribing ->
                        state.progressPercent?.let { progressPercent ->
                            "${stringResource(Res.string.feature_voice_transcribing)} $progressPercent%"
                        } ?: stringResource(Res.string.feature_voice_transcribing)

                    else -> ""
                },
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun TranscriptScroll(
    transcript: VoiceTranscript,
    displayPlaybackPositionMilliseconds: Long,
    isPlaying: Boolean,
    isScrubbing: Boolean,
    durationMilliseconds: Long
) {
    val transcriptionScrollState = rememberScrollState()
    val transcriptionViewportWidth = remember { mutableIntStateOf(0) }
    var transcriptionLayout by remember(transcript.text) { mutableStateOf<TextLayoutResult?>(null) }

    val playedCharacterPosition =
        remember(
            transcript,
            displayPlaybackPositionMilliseconds,
            durationMilliseconds
        ) {
            calculatePlayedCharacterPosition(
                transcript = transcript.text,
                cues = transcript.cues,
                playbackPositionMilliseconds = displayPlaybackPositionMilliseconds,
                durationMilliseconds = durationMilliseconds
            )
        }
    val playedTextX =
        calculatePlayedTextX(
            transcript = transcript.text,
            characterPosition = playedCharacterPosition,
            textLayout = transcriptionLayout
        )
    val transcriptionTargetScroll =
        calculateTranscriptTargetScroll(
            playbackHeadX = playedTextX,
            maxScroll = transcriptionScrollState.maxValue,
            viewportWidth = transcriptionViewportWidth.intValue
        )

    SyncTranscriptScroll(
        scrollState = transcriptionScrollState,
        isPlaying = isPlaying,
        isScrubbing = isScrubbing,
        playbackPositionMilliseconds = displayPlaybackPositionMilliseconds,
        finalCueEndMilliseconds =
            transcript.cues.lastOrNull()?.endMilliseconds
                ?: durationMilliseconds.takeIf { it > 0L },
        targetScroll = transcriptionTargetScroll
    )

    Box(
        modifier =
            Modifier
                .padding(start = MaterialTheme.spacing.base)
                .fillMaxWidth()
                .onSizeChanged { transcriptionViewportWidth.intValue = it.width }
    ) {
        Box(modifier = Modifier.horizontalScroll(transcriptionScrollState)) {
            Text(
                text = transcript.text,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = Alpha.Subtle),
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Clip,
                onTextLayout = { transcriptionLayout = it }
            )
            Text(
                text = transcript.text,
                modifier =
                    Modifier.drawWithContent {
                        clipRect(right = playedTextX.coerceIn(0f, size.width)) {
                            this@drawWithContent.drawContent()
                        }
                    },
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Clip
            )
        }
    }
}

private fun Long.positionAt(progress: Float): Long =
    (coerceAtLeast(0L).toFloat() * progress.coerceIn(0f, 1f)).roundToInt().toLong()

private class VoiceScrubState {
    private var durationMilliseconds by mutableLongStateOf(0L)
    private var interpolatedPlaybackPositionMilliseconds by mutableLongStateOf(0L)

    var scrubPositionMilliseconds by mutableStateOf<Long?>(null)
        private set

    var isScrubbing by mutableStateOf(false)
        private set

    val displayPositionMilliseconds: Long
        get() = scrubPositionMilliseconds ?: interpolatedPlaybackPositionMilliseconds

    val progress: Float
        get() =
            if (durationMilliseconds > 0L) {
                (displayPositionMilliseconds.toFloat() / durationMilliseconds.toFloat())
                    .coerceIn(0f, 1f)
            } else {
                0f
            }

    fun updateDuration(durationMilliseconds: Long) {
        this.durationMilliseconds = durationMilliseconds.coerceAtLeast(0L)
    }

    fun updatePlaybackPosition(positionMilliseconds: Long) {
        interpolatedPlaybackPositionMilliseconds =
            positionMilliseconds.coerceIn(0L, durationMilliseconds.coerceAtLeast(0L))
    }

    fun start(progress: Float) {
        isScrubbing = true
        scrubPositionMilliseconds = durationMilliseconds.positionAt(progress)
    }

    fun update(progress: Float) {
        if (!isScrubbing) return
        scrubPositionMilliseconds = durationMilliseconds.positionAt(progress)
    }

    fun end(progress: Float): Long {
        val positionMilliseconds = durationMilliseconds.positionAt(progress)
        scrubPositionMilliseconds = positionMilliseconds
        isScrubbing = false
        return positionMilliseconds
    }

    fun clearScrubPosition() {
        scrubPositionMilliseconds = null
    }
}

@Composable
private fun rememberVoiceScrubState(
    playbackPositionMilliseconds: Long,
    durationMilliseconds: Long,
    isPlaying: Boolean
): VoiceScrubState {
    val state = remember { VoiceScrubState() }
    val currentReportedPosition by rememberUpdatedState(playbackPositionMilliseconds)
    val currentDuration by rememberUpdatedState(durationMilliseconds)
    val currentIsPlaying by rememberUpdatedState(isPlaying)

    SideEffect {
        state.updateDuration(durationMilliseconds)
    }

    SideEffect {
        if (!isPlaying) {
            state.updatePlaybackPosition(playbackPositionMilliseconds)
        }
    }

    LaunchedEffect(isPlaying) {
        if (!isPlaying) return@LaunchedEffect

        var observedReportedPosition = currentReportedPosition
        var anchorPosition = currentReportedPosition
        var anchorFrameNanos = 0L

        while (currentIsPlaying) {
            withFrameNanos { frameNanos ->
                if (
                    anchorFrameNanos == 0L ||
                    currentReportedPosition != observedReportedPosition
                ) {
                    observedReportedPosition = currentReportedPosition
                    anchorPosition = currentReportedPosition
                    anchorFrameNanos = frameNanos
                }

                val elapsedMilliseconds =
                    (frameNanos - anchorFrameNanos) / NANOS_PER_MILLISECOND

                state.updatePlaybackPosition(
                    (anchorPosition + elapsedMilliseconds)
                        .coerceIn(0L, currentDuration.coerceAtLeast(0L))
                )
            }
        }
    }

    SideEffect {
        val scrubPosition = state.scrubPositionMilliseconds
        if (
            scrubPosition != null &&
            !state.isScrubbing &&
            abs(playbackPositionMilliseconds - scrubPosition) <= SCRUB_SYNC_TOLERANCE_MILLISECONDS
        ) {
            state.clearScrubPosition()
        }
    }

    return state
}

@Composable
private fun SyncTranscriptScroll(
    scrollState: ScrollState,
    isPlaying: Boolean,
    isScrubbing: Boolean,
    playbackPositionMilliseconds: Long,
    finalCueEndMilliseconds: Long?,
    targetScroll: Int
) {
    val currentTargetScroll by rememberUpdatedState(targetScroll)
    val currentIsPlaying by rememberUpdatedState(isPlaying)
    val currentIsScrubbing by rememberUpdatedState(isScrubbing)
    val isAtStart = playbackPositionMilliseconds <= 0L
    val isAtEnd =
        finalCueEndMilliseconds != null &&
            playbackPositionMilliseconds >= finalCueEndMilliseconds

    LaunchedEffect(isAtStart, isAtEnd) {
        when {
            isAtStart -> scrollState.scrollTo(0)
            isAtEnd -> scrollState.scrollTo(scrollState.maxValue)
        }
    }

    LaunchedEffect(isPlaying, isScrubbing) {
        if (!isPlaying && !isScrubbing) return@LaunchedEffect

        while (currentIsPlaying || currentIsScrubbing) {
            withFrameNanos { }
            val target = currentTargetScroll.coerceIn(0, scrollState.maxValue)
            if (scrollState.value != target) {
                scrollState.scrollTo(target)
            }
        }
    }
}

private fun calculatePlayedCharacterPosition(
    transcript: String,
    cues: List<VoiceTranscriptCue>,
    playbackPositionMilliseconds: Long,
    durationMilliseconds: Long
): Float {
    if (transcript.isEmpty()) return 0f
    if (playbackPositionMilliseconds <= 0L) return 0f

    if (cues.isEmpty()) {
        if (durationMilliseconds <= 0L) return 0f
        return transcript.length *
            (playbackPositionMilliseconds.toFloat() / durationMilliseconds.toFloat()).coerceIn(
                0f,
                1f
            )
    }

    var characterOffset = 0
    cues.forEach { cue ->
        val cueLength = cue.text.length
        when {
            playbackPositionMilliseconds < cue.startMilliseconds -> return characterOffset.toFloat()
            playbackPositionMilliseconds <= cue.endMilliseconds -> {
                val cueDuration = (cue.endMilliseconds - cue.startMilliseconds).coerceAtLeast(1L)
                val cueProgress =
                    ((playbackPositionMilliseconds - cue.startMilliseconds).toFloat() / cueDuration.toFloat())
                        .coerceIn(0f, 1f)
                return (characterOffset + cueLength * cueProgress).coerceAtMost(transcript.length.toFloat())
            }
        }
        characterOffset += cueLength
    }

    return transcript.length.toFloat()
}

private fun calculatePlayedTextX(
    transcript: String,
    characterPosition: Float,
    textLayout: TextLayoutResult?
): Float {
    if (transcript.isEmpty()) return 0f
    val layout = textLayout ?: return 0f
    if (characterPosition <= 0f) return 0f
    if (characterPosition >= transcript.length) return layout.size.width.toFloat()

    val characterIndex = floor(characterPosition).toInt().coerceIn(0, transcript.lastIndex)
    val characterFraction = characterPosition - characterIndex
    val bounds = layout.getBoundingBox(characterIndex)
    return bounds.left + bounds.width * characterFraction
}

private fun calculateTranscriptTargetScroll(
    playbackHeadX: Float,
    maxScroll: Int,
    viewportWidth: Int
): Int {
    if (maxScroll <= 0 || viewportWidth <= 0) return 0

    return (playbackHeadX - viewportWidth * TRANSCRIPT_FOLLOW_POSITION)
        .roundToInt()
        .coerceIn(0, maxScroll)
}

@Preview
@Composable
private fun VoiceMessageContentPreview() {
    SparrowTheme {
        VoiceMessageContent(
            target = VoiceMessageTarget(
                attachmentId = "1",
                durationMilliseconds = 10000L
            ),
            modifier = Modifier.fillMaxWidth()
        )
    }
}

private const val TRANSCRIPT_FOLLOW_POSITION = 0.62f
private const val NANOS_PER_MILLISECOND = 1_000_000L
private const val SCRUB_SYNC_TOLERANCE_MILLISECONDS = 150L
