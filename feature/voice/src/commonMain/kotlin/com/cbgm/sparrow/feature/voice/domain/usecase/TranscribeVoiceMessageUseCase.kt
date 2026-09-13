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
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
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
            var lastProgressPercent = -1
            send(VoiceTranscriptionState.Transcribing(progressPercent = 0))
            lastProgressPercent = 0

            val transcript = runPhase(phase, TRANSCRIPTION_TIMEOUT_MILLISECONDS) {
                transcribeVoiceAudio(
                    bytes = bytes,
                    onProgress = { progressPercent ->
                        val normalizedProgress = progressPercent.coerceIn(0, 100)
                        if (normalizedProgress != lastProgressPercent) {
                            lastProgressPercent = normalizedProgress
                            trySend(
                                VoiceTranscriptionState.Transcribing(
                                    progressPercent = normalizedProgress
                                )
                            )
                        }
                    }
                ).getOrThrow()
            }

            if (lastProgressPercent < 100) {
                send(VoiceTranscriptionState.Transcribing(progressPercent = 100))
            }

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
                if (error is VoiceTranscriptionPhaseException) {
                    error
                } else {
                    VoiceTranscriptionPhaseException(phase, error)
                }
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

    private companion object {
        const val DOWNLOAD_TIMEOUT_MILLISECONDS = 60_000L
        const val PREPARE_TIMEOUT_MILLISECONDS = 210_000L
        const val TRANSCRIPTION_TIMEOUT_MILLISECONDS = 180_000L
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
