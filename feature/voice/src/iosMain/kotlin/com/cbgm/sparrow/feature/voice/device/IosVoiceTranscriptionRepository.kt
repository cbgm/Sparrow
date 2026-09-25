package com.cbgm.sparrow.feature.voice.device

import com.cbgm.sparrow.core.logging.SparrowLog
import com.cbgm.sparrow.core.result.safeSuspendCall
import com.cbgm.sparrow.feature.voice.domain.model.VoiceTranscriptCue
import com.cbgm.sparrow.feature.voice.domain.model.VoiceTranscript
import com.cbgm.sparrow.feature.voice.domain.repository.VoiceTranscriptionRepository
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.suspendCancellableCoroutine
import platform.AVFAudio.AVAudioFormat
import platform.AVFAudio.AVAudioPCMBuffer
import platform.AVFAudio.AVAudioPCMFormatInt16
import platform.Speech.SFSpeechAudioBufferRecognitionRequest
import platform.Speech.SFSpeechRecognitionTask
import platform.Speech.SFSpeechRecognizer
import platform.Speech.SFSpeechRecognizerAuthorizationStatusAuthorized
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

@OptIn(ExperimentalForeignApi::class)
class IosVoiceTranscriptionRepository : VoiceTranscriptionRepository {
    private val logger = SparrowLog.withTag("VoiceTranscription")

    override suspend fun prepare(): Result<Unit> =
        safeSuspendCall {
            check(requestSpeechAuthorization()) { "Speech recognition permission is required" }
            val recognizer = checkNotNull(SFSpeechRecognizer()) { "Speech recognizer is unavailable" }
            check(recognizer.supportsOnDeviceRecognition) {
                "On-device speech recognition is not available for the current language"
            }
        }.onFailure { error -> logger.error(error) { "Could not prepare voice transcription" } }

    override suspend fun transcribe(
        bytes: ByteArray,
        onProgress: (Int) -> Unit
    ): Result<VoiceTranscript> = safeSuspendCall {
        onProgress(0)
        val wave = bytes.toPcmWaveAudio()
        val recognizer = checkNotNull(SFSpeechRecognizer()) { "Speech recognizer is unavailable" }
        val request = SFSpeechAudioBufferRecognitionRequest()
        request.shouldReportPartialResults = false
        request.requiresOnDeviceRecognition = true
        request.appendAudioPCMBuffer(wave.toAudioBuffer())
        request.endAudio()

        val transcript =
            suspendCancellableCoroutine { continuation ->
                var task: SFSpeechRecognitionTask? = null
                var finished = false

                fun finish(result: Result<VoiceTranscript>) {
                    if (finished) return
                    finished = true
                    task?.cancel()
                    if (!continuation.isActive) return
                    result.fold(
                        onSuccess = continuation::resume,
                        onFailure = continuation::resumeWithException
                    )
                }

                task =
                    recognizer.recognitionTaskWithRequest(request) { result, error ->
                        if (error != null) {
                            finish(Result.failure(IllegalStateException(error.localizedDescription)))
                            return@recognitionTaskWithRequest
                        }

                        val text = result?.bestTranscription?.formattedString?.trim().orEmpty()
                        if (text.isNotBlank()) {
                            finish(
                                Result.success(
                                    VoiceTranscript(
                                        text = text,
                                        cues =
                                            listOf(
                                                VoiceTranscriptCue(
                                                    text = text,
                                                    startMilliseconds = 0L,
                                                    endMilliseconds = wave.durationMilliseconds
                                                )
                                            )
                                    )
                                )
                            )
                        }
                    }

                continuation.invokeOnCancellation { task?.cancel() }
            }
        onProgress(100)
        transcript
    }.onFailure { error -> logger.error(error) { "Could not transcribe voice message" } }

    private suspend fun requestSpeechAuthorization(): Boolean =
        suspendCancellableCoroutine { continuation ->
            SFSpeechRecognizer.requestAuthorization { status ->
                if (continuation.isActive) {
                    continuation.resume(status == SFSpeechRecognizerAuthorizationStatusAuthorized)
                }
            }
        }

    private fun PcmWaveAudio.toAudioBuffer(): AVAudioPCMBuffer {
        val format =
            checkNotNull(
                AVAudioFormat(
                    commonFormat = AVAudioPCMFormatInt16,
                    sampleRate = sampleRate.toDouble(),
                    channels = channelCount.toUInt(),
                    interleaved = false
                )
            ) { "Could not create the transcription audio format" }

        val frameCount = pcmBytes.size / 2
        val buffer =
            checkNotNull(
                AVAudioPCMBuffer(
                    pcmFormat = format,
                    frameCapacity = frameCount.toUInt()
                )
            ) { "Could not create the transcription audio buffer" }
        buffer.frameLength = frameCount.toUInt()

        val samples = checkNotNull(buffer.int16ChannelData?.get(0)) {
            "Could not access the transcription audio buffer"
        }
        repeat(frameCount) { index ->
            val byteOffset = index * 2
            val sample =
                (pcmBytes[byteOffset].toInt() and 0xff) or
                    ((pcmBytes[byteOffset + 1].toInt() and 0xff) shl 8)
            samples[index] = sample.toShort()
        }
        return buffer
    }
}

private val PcmWaveAudio.durationMilliseconds: Long
    get() = ((pcmBytes.size.toLong() / 2L) * 1_000L / sampleRate).coerceAtLeast(1L)
