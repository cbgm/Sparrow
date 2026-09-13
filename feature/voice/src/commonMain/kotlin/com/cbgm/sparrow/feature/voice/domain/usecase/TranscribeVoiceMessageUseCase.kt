package com.cbgm.sparrow.feature.voice.domain.usecase

import com.cbgm.sparrow.core.logging.SparrowLog
import com.cbgm.sparrow.feature.attachments.domain.model.AttachmentTranscript
import com.cbgm.sparrow.feature.attachments.domain.model.AttachmentTranscriptCue
import com.cbgm.sparrow.feature.attachments.domain.usecase.LoadAttachmentBytesUseCase
import com.cbgm.sparrow.feature.attachments.domain.usecase.SaveMessageAttachmentTranscriptUseCase
import com.cbgm.sparrow.feature.voice.domain.model.VoiceMessageTarget
import com.cbgm.sparrow.feature.voice.domain.model.VoiceTranscriptionState
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
import kotlin.time.Duration.Companion.milliseconds

class TranscribeVoiceMessageUseCase(
    private val loadAttachmentBytes: LoadAttachmentBytesUseCase,
    private val prepareVoiceTranscription: PrepareVoiceTranscriptionUseCase,
    private val transcribeVoiceAudio: TranscribeVoiceAudioUseCase,
    private val saveTranscript: SaveMessageAttachmentTranscriptUseCase
) {
    private val logger = SparrowLog.withTag("VoiceTranscription")

    operator fun invoke(target: VoiceMessageTarget): Flow<VoiceTranscriptionState> = channelFlow {
        var phase = VoiceTranscriptionPhase.DOWNLOADING
        try {
            send(VoiceTranscriptionState.Downloading)
            val bytes = runPhase(phase, DOWNLOAD_TIMEOUT_MILLISECONDS) {
                loadAttachmentBytes(target.attachmentTarget).getOrThrow()
            }

            phase = VoiceTranscriptionPhase.PREPARING
            send(VoiceTranscriptionState.Preparing)
            runPhase(phase, PREPARE_TIMEOUT_MILLISECONDS) {
                prepareVoiceTranscription().getOrThrow()
            }

            phase = VoiceTranscriptionPhase.TRANSCRIBING
            val nativeProgress = MutableStateFlow(0)
            var displayedProgress = 1
            send(VoiceTranscriptionState.Transcribing(progressPercent = displayedProgress))

            val progressJob = launch {
                while (true) {
                    delay(progressTickDelayMilliseconds(displayedProgress).milliseconds)

                    val checkpoint = nativeProgress.value.coerceIn(0, MAX_IN_FLIGHT_PROGRESS_PERCENT)
                    val nextProgress =
                        if (checkpoint > displayedProgress) {
                            checkpoint
                        } else {
                            (displayedProgress + 1).coerceAtMost(MAX_IN_FLIGHT_PROGRESS_PERCENT)
                        }

                    if (nextProgress != displayedProgress) {
                        displayedProgress = nextProgress
                        send(
                            VoiceTranscriptionState.Transcribing(
                                progressPercent = displayedProgress
                            )
                        )
                    }

                    // whisper.cpp's real progress callback advances only when an internal
                    // audio seek window completes. Interpolate slowly between checkpoints
                    // so the UI never looks frozen, while reserving 100% for completion.
                }
            }

            val transcript =
                try {
                    runPhase(phase, TRANSCRIPTION_TIMEOUT_MILLISECONDS) {
                        transcribeVoiceAudio(
                            bytes = bytes,
                            onProgress = { progressPercent ->
                                val normalizedProgress =
                                    progressPercent.coerceIn(0, MAX_IN_FLIGHT_PROGRESS_PERCENT)
                                if (normalizedProgress > nativeProgress.value) {
                                    nativeProgress.value = normalizedProgress
                                }
                            }
                        ).getOrThrow()
                    }
                } finally {
                    progressJob.cancelAndJoin()
                }

            send(VoiceTranscriptionState.Transcribing(progressPercent = 100))

            check(transcript.text.isNotBlank()) { "No speech could be transcribed" }
            saveTranscript(
                attachmentId = target.attachmentId,
                transcription =
                    AttachmentTranscript(
                        text = transcript.text,
                        cues = transcript.cues.map { cue ->
                            AttachmentTranscriptCue(
                                text = cue.text,
                                startMilliseconds = cue.startMilliseconds,
                                endMilliseconds = cue.endMilliseconds
                            )
                        }
                    )
            ).getOrThrow()

            send(VoiceTranscriptionState.Success(transcript))
        } catch (error: Throwable) {
            if (error is CancellationException && error !is TimeoutCancellationException) throw error
            val resolved =
                error as? VoiceTranscriptionPhaseException ?: VoiceTranscriptionPhaseException(phase, error)
            logger.error(resolved) { "Voice transcription failed during ${resolved.phase}" }
            send(VoiceTranscriptionState.Error(resolved))
        }
    }

    private suspend fun <T> runPhase(
        phase: VoiceTranscriptionPhase,
        timeoutMilliseconds: Long,
        block: suspend () -> T
    ): T =
        try {
            withTimeout(timeoutMilliseconds.milliseconds) { block() }
        } catch (error: TimeoutCancellationException) {
            throw VoiceTranscriptionPhaseException(
                phase = phase,
                cause = IllegalStateException("Voice transcription $phase timed out", error)
            )
        } catch (error: CancellationException) {
            throw error
        } catch (error: Throwable) {
            throw VoiceTranscriptionPhaseException(phase, error)
        }

    private fun progressTickDelayMilliseconds(progressPercent: Int): Long =
        PROGRESS_TICK_BASE_DELAY_MILLISECONDS +
            progressPercent * PROGRESS_TICK_PER_PERCENT_DELAY_MILLISECONDS

    private companion object {
        const val DOWNLOAD_TIMEOUT_MILLISECONDS = 60_000L
        const val PREPARE_TIMEOUT_MILLISECONDS = 210_000L
        const val TRANSCRIPTION_TIMEOUT_MILLISECONDS = 180_000L
        const val MAX_IN_FLIGHT_PROGRESS_PERCENT = 99
        const val PROGRESS_TICK_BASE_DELAY_MILLISECONDS = 350L
        const val PROGRESS_TICK_PER_PERCENT_DELAY_MILLISECONDS = 15L
    }
}

private enum class VoiceTranscriptionPhase {
    DOWNLOADING,
    PREPARING,
    TRANSCRIBING
}

private class VoiceTranscriptionPhaseException(
    val phase: VoiceTranscriptionPhase,
    cause: Throwable
) : IllegalStateException("Voice transcription failed during $phase: ${cause.message}", cause)
