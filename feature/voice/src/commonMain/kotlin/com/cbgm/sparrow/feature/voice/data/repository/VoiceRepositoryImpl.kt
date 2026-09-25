package com.cbgm.sparrow.feature.voice.data.repository

import com.cbgm.sparrow.core.coroutines.ApplicationCoroutineScope
import com.cbgm.sparrow.core.logging.SparrowLog
import com.cbgm.sparrow.feature.voice.device.VoicePlayer
import com.cbgm.sparrow.feature.voice.device.VoiceRecorder
import com.cbgm.sparrow.feature.voice.domain.model.VoiceComposerPhase
import com.cbgm.sparrow.feature.voice.domain.model.VoiceComposerState
import com.cbgm.sparrow.feature.voice.domain.model.VoicePlaybackState
import com.cbgm.sparrow.feature.voice.domain.model.VoiceRecording
import com.cbgm.sparrow.feature.voice.domain.repository.VoiceRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.ExperimentalTime
import kotlin.time.TimeMark
import kotlin.time.TimeSource

@OptIn(ExperimentalTime::class)
internal class VoiceRepositoryImpl(
    private val applicationScope: ApplicationCoroutineScope,
    private val recorder: VoiceRecorder,
    private val player: VoicePlayer
) : VoiceRepository {
    private val logger = SparrowLog.withTag("VoiceRepository")
    private val composerState = MutableStateFlow(VoiceComposerState())
    private val playbackState = MutableStateFlow(VoicePlaybackState())

    private var recording: VoiceRecording? = null
    private var recordingStartedAt: TimeMark? = null
    private var recordingTicker: Job? = null
    private var playbackTicker: Job? = null
    private var previewPlaying = false
    private var previewPrepared = false
    private var preparedMessageAttachmentId: String? = null
    private var scrubbingAttachmentId: String? = null
    private var resumeAfterScrub = false

    override fun observeComposerState(): Flow<VoiceComposerState> = composerState

    override fun observePlaybackState(attachmentId: String): Flow<VoicePlaybackState> =
        playbackState
            .map { state ->
                state.takeIf { it.attachmentId == attachmentId }
                    ?: VoicePlaybackState(attachmentId = attachmentId)
            }
            .distinctUntilChanged()

    override suspend fun startRecording(): Result<Unit> {
        if (composerState.value.phase != VoiceComposerPhase.READY) return Result.success(Unit)
        stopPlayback()
        return recorder.start()
            .onSuccess {
                recording = null
                recordingStartedAt = TimeSource.Monotonic.markNow()
                composerState.value = VoiceComposerState(phase = VoiceComposerPhase.RECORDING)
                recordingTicker?.cancel()
                recordingTicker = applicationScope.launch {
                    while (true) {
                        delay(PROGRESS_INTERVAL_MILLISECONDS.milliseconds)
                        composerState.value =
                            composerState.value.copy(
                                durationMilliseconds = recordingStartedAt?.elapsedNow()?.inWholeMilliseconds ?: 0L
                            )
                    }
                }
            }
            .onFailure { error -> logger.error(error) { "Could not start voice recording" } }
    }

    override suspend fun stopRecording(): Result<Unit> {
        if (composerState.value.phase != VoiceComposerPhase.RECORDING) return Result.success(Unit)
        recordingTicker?.cancel()
        return recorder.stop()
            .map { result ->
                recording = result
                recordingStartedAt = null
                composerState.value =
                    VoiceComposerState(
                        phase = VoiceComposerPhase.RECORDED,
                        durationMilliseconds = result.durationMilliseconds
                    )
            }
            .onFailure { error -> logger.error(error) { "Could not stop voice recording" } }
    }

    override fun togglePreview(): Result<Unit> {
        val current = recording ?: return Result.failure(IllegalStateException("No voice recording is available"))
        if (composerState.value.phase != VoiceComposerPhase.RECORDED) return Result.success(Unit)

        if (previewPlaying) {
            player.pause()
            playbackTicker?.cancel()
            previewPlaying = false
            composerState.value = composerState.value.copy(isPlaying = false)
            return Result.success(Unit)
        }

        stopMessagePlayback()
        val result = if (previewPrepared) runCatching { player.resume() } else player.play(current.bytes)
        return result
            .onSuccess {
                previewPrepared = true
                previewPlaying = true
                composerState.value = composerState.value.copy(isPlaying = true)
                startPreviewTicker(current.durationMilliseconds)
            }
            .onFailure { error -> logger.error(error) { "Could not play voice recording preview" } }
    }

    override suspend fun cancelRecording() {
        recorder.cancel()
        resetComposer()
    }

    override fun resetComposer() {
        recordingTicker?.cancel()
        if (previewPrepared) {
            playbackTicker?.cancel()
            player.stop()
        }
        previewPlaying = false
        previewPrepared = false
        recording = null
        recordingStartedAt = null
        composerState.value = VoiceComposerState()
    }

    override fun currentRecording(): VoiceRecording? = recording

    override fun toggleMessagePlayback(
        attachmentId: String,
        bytes: ByteArray,
        durationMilliseconds: Long
    ): Result<Unit> {
        clearMessageScrub()
        val current = playbackState.value
        if (
            current.attachmentId == attachmentId &&
            preparedMessageAttachmentId == attachmentId &&
            player.isPlaying
        ) {
            player.pause()
            playbackTicker?.cancel()
            playbackState.value = current.copy(
                positionMilliseconds = player.currentPositionMilliseconds,
                isPlaying = false
            )
            return Result.success(Unit)
        }

        stopPreviewPlayback()
        val result =
            when {
                current.attachmentId == attachmentId &&
                    preparedMessageAttachmentId == attachmentId &&
                    current.positionMilliseconds >= durationMilliseconds &&
                    durationMilliseconds > 0L ->
                    runCatching {
                        player.seekTo(0L)
                        player.resume()
                    }

                current.attachmentId == attachmentId && preparedMessageAttachmentId == attachmentId ->
                    runCatching { player.resume() }

                else -> {
                    player.stop()
                    preparedMessageAttachmentId = null
                    player.play(bytes)
                }
            }

        return result
            .onSuccess {
                preparedMessageAttachmentId = attachmentId
                playbackState.value =
                    VoicePlaybackState(
                        attachmentId = attachmentId,
                        positionMilliseconds = player.currentPositionMilliseconds,
                        isPlaying = true
                    )
                startMessageTicker(attachmentId, durationMilliseconds)
            }
            .onFailure { error -> logger.error(error) { "Could not play voice message $attachmentId" } }
    }

    override fun startMessageScrub(attachmentId: String) {
        playbackTicker?.cancel()
        stopPreviewPlayback()

        val current = playbackState.value
        scrubbingAttachmentId = attachmentId
        resumeAfterScrub =
            current.attachmentId == attachmentId &&
            preparedMessageAttachmentId == attachmentId &&
            current.isPlaying

        if (current.attachmentId == attachmentId && preparedMessageAttachmentId == attachmentId) {
            player.pause()
        } else {
            player.stop()
            preparedMessageAttachmentId = null
            playbackState.value = VoicePlaybackState()
        }
    }

    override fun finishMessageScrub(
        attachmentId: String,
        bytes: ByteArray,
        durationMilliseconds: Long,
        positionMilliseconds: Long
    ): Result<Unit> {
        if (scrubbingAttachmentId != attachmentId) return Result.success(Unit)
        val targetPosition = positionMilliseconds.coerceIn(0L, durationMilliseconds.coerceAtLeast(0L))
        val shouldResume = resumeAfterScrub && targetPosition < durationMilliseconds

        return runCatching {
            if (preparedMessageAttachmentId != attachmentId) {
                player.prepare(bytes).getOrThrow()
                preparedMessageAttachmentId = attachmentId
            }
            player.seekTo(targetPosition)
            if (shouldResume) player.resume()
        }.onSuccess {
            playbackState.value =
                VoicePlaybackState(
                    attachmentId = attachmentId,
                    positionMilliseconds = targetPosition,
                    isPlaying = shouldResume
                )
            clearMessageScrub()
            if (shouldResume) startMessageTicker(attachmentId, durationMilliseconds)
        }.onFailure { error ->
            player.stop()
            preparedMessageAttachmentId = null
            playbackState.value = VoicePlaybackState()
            clearMessageScrub()
            logger.error(error) { "Could not seek voice message $attachmentId" }
        }
    }

    private fun stopPlayback() {
        playbackTicker?.cancel()
        stopPreviewPlayback()
        stopMessagePlayback()
        player.stop()
    }

    private fun stopPreviewPlayback() {
        if (!previewPrepared && !previewPlaying) return
        previewPlaying = false
        previewPrepared = false
        playbackTicker?.cancel()
        composerState.value = composerState.value.copy(isPlaying = false, playbackProgress = 0f)
        player.stop()
    }

    private fun stopMessagePlayback() {
        if (preparedMessageAttachmentId == null && playbackState.value.attachmentId == null) return
        playbackTicker?.cancel()
        preparedMessageAttachmentId = null
        clearMessageScrub()
        playbackState.value = VoicePlaybackState()
        player.stop()
    }

    private fun clearMessageScrub() {
        scrubbingAttachmentId = null
        resumeAfterScrub = false
    }

    private fun startPreviewTicker(durationMilliseconds: Long) {
        playbackTicker?.cancel()
        playbackTicker = applicationScope.launch {
            while (previewPlaying) {
                delay(PROGRESS_INTERVAL_MILLISECONDS.milliseconds)
                val position = player.currentPositionMilliseconds
                if (!player.isPlaying) {
                    previewPlaying = false
                    previewPrepared = false
                    player.stop()
                    composerState.value = composerState.value.copy(isPlaying = false, playbackProgress = 0f)
                    break
                }
                composerState.value =
                    composerState.value.copy(
                        playbackProgress =
                            if (durationMilliseconds > 0L) {
                                (position.toFloat() / durationMilliseconds.toFloat()).coerceIn(0f, 1f)
                            } else {
                                0f
                            }
                    )
            }
        }
    }

    private fun startMessageTicker(attachmentId: String, durationMilliseconds: Long) {
        playbackTicker?.cancel()
        playbackTicker = applicationScope.launch {
            while (playbackState.value.attachmentId == attachmentId) {
                delay(PROGRESS_INTERVAL_MILLISECONDS.milliseconds)
                val position = player.currentPositionMilliseconds
                val playing = player.isPlaying
                if (!playing) {
                    playbackState.value =
                        VoicePlaybackState(
                            attachmentId = attachmentId,
                            positionMilliseconds = durationMilliseconds,
                            isPlaying = false
                        )
                    player.stop()
                    preparedMessageAttachmentId = null
                    delay(TRANSCRIPT_COMPLETION_HOLD_MILLISECONDS.milliseconds)
                    val current = playbackState.value
                    if (
                        current.attachmentId == attachmentId &&
                        !current.isPlaying &&
                        current.positionMilliseconds >= durationMilliseconds
                    ) {
                        playbackState.value = VoicePlaybackState()
                    }
                    break
                }
                playbackState.value =
                    VoicePlaybackState(
                        attachmentId = attachmentId,
                        positionMilliseconds = position,
                        isPlaying = true
                    )
            }
        }
    }

    private companion object {
        const val PROGRESS_INTERVAL_MILLISECONDS = 100L
        const val TRANSCRIPT_COMPLETION_HOLD_MILLISECONDS = 300L
    }
}
