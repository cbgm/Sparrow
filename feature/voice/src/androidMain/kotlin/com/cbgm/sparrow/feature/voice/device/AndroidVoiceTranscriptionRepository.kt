package com.cbgm.sparrow.feature.voice.device

import com.cbgm.sparrow.core.logging.SparrowLog
import com.cbgm.sparrow.core.result.safeSuspendCall
import com.cbgm.sparrow.feature.voice.domain.model.VoiceTranscript
import com.cbgm.sparrow.feature.voice.domain.model.VoiceTranscriptCue
import com.cbgm.sparrow.feature.voice.domain.repository.VoiceTranscriptionRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlin.math.floor

internal class AndroidVoiceTranscriptionRepository(
    private val modelStore: AndroidWhisperModelStore
) : VoiceTranscriptionRepository {
    private val logger = SparrowLog.withTag("VoiceTranscription")
    private val whisperNative by lazy(::WhisperNative)
    private val transcriptionMutex = Mutex()
    private var modelHandle: Long = 0L

    override suspend fun prepare(): Result<Unit> =
        safeSuspendCall {
            val modelFile = withContext(Dispatchers.IO) { modelStore.requireModel() }
            withContext(Dispatchers.Default) {
                transcriptionMutex.withLock { requireModelHandle(modelFile.absolutePath) }
            }
            Unit
        }.onFailure { error ->
            logger.error(error) { "Could not prepare voice transcription" }
        }

    override suspend fun transcribe(
        bytes: ByteArray,
        onProgress: (Int) -> Unit
    ): Result<VoiceTranscript> = safeSuspendCall {
        require(bytes.isNotEmpty()) { "Voice message is empty" }

        val wave = bytes.toPcmWaveAudio()
        val samples = wave.toWhisperSamples()
        withContext(Dispatchers.Default) {
            transcriptionMutex.withLock {
                val handle = checkNotNull(modelHandle.takeIf { it != 0L }) {
                    "Voice transcription has not been prepared"
                }
                val fallbackText =
                    whisperNative.transcribe(
                        modelHandle = handle,
                        samples = samples,
                        progressCallback = WhisperProgressCallback { progressPercent ->
                            onProgress(progressPercent.coerceIn(0, 100))
                        }
                    ).trim()
                onProgress(100)
                val cues = whisperNative.readTranscriptCues(handle).normalizeEdges()
                val text = cues.joinToString(separator = "") { cue -> cue.text }
                val resolvedText = text.ifBlank { fallbackText }

                check(resolvedText.isNotBlank()) { "No speech could be transcribed" }

                VoiceTranscript(
                    text = resolvedText,
                    cues =
                        if (cues.isNotEmpty()) {
                            cues
                        } else {
                            listOf(
                                VoiceTranscriptCue(
                                    text = resolvedText,
                                    startMilliseconds = 0L,
                                    endMilliseconds = wave.durationMilliseconds
                                )
                            )
                        }
                )
            }
        }
    }.onFailure { error ->
        logger.error(error) { "Could not transcribe voice message" }
    }

    private fun requireModelHandle(modelPath: String): Long {
        if (modelHandle != 0L) return modelHandle

        modelHandle = whisperNative.loadModel(modelPath)
        check(modelHandle != 0L) { "Could not load the voice transcription model" }
        return modelHandle
    }
}

private fun WhisperNative.readTranscriptCues(modelHandle: Long): List<VoiceTranscriptCue> =
    buildList {
        repeat(segmentCount(modelHandle)) { segmentIndex ->
            val text = segmentText(modelHandle, segmentIndex)
            val startMilliseconds = segmentStartMilliseconds(modelHandle, segmentIndex)
            val endMilliseconds = segmentEndMilliseconds(modelHandle, segmentIndex)
            if (text.isNotEmpty() && endMilliseconds > startMilliseconds) {
                add(
                    VoiceTranscriptCue(
                        text = text,
                        startMilliseconds = startMilliseconds.coerceAtLeast(0L),
                        endMilliseconds = endMilliseconds.coerceAtLeast(startMilliseconds + 1L)
                    )
                )
            }
        }
    }

private fun List<VoiceTranscriptCue>.normalizeEdges(): List<VoiceTranscriptCue> =
    mapIndexedNotNull { index, cue ->
        val text =
            when {
                size == 1 -> cue.text.trim()
                index == 0 -> cue.text.trimStart()
                index == lastIndex -> cue.text.trimEnd()
                else -> cue.text
            }
        text.takeIf(String::isNotEmpty)?.let { normalizedText -> cue.copy(text = normalizedText) }
    }

private val PcmWaveAudio.durationMilliseconds: Long
    get() =
        ((pcmBytes.size.toLong() / PCM_BYTES_PER_SAMPLE) * 1_000L / sampleRate)
            .coerceAtLeast(1L)

private fun PcmWaveAudio.toWhisperSamples(): FloatArray {
    val sourceSampleCount = pcmBytes.size / PCM_BYTES_PER_SAMPLE
    val source = FloatArray(sourceSampleCount) { index ->
        val byteOffset = index * PCM_BYTES_PER_SAMPLE
        val sample =
            (
                (pcmBytes[byteOffset].toInt() and 0xff) or
                    ((pcmBytes[byteOffset + 1].toInt() and 0xff) shl 8)
            )
                .toShort()
        sample.toFloat() / 32768f
    }

    if (sampleRate == WHISPER_SAMPLE_RATE_HZ) return source

    val targetSampleCount =
        ((source.size.toLong() * WHISPER_SAMPLE_RATE_HZ) / sampleRate)
            .toInt()
            .coerceAtLeast(1)
    val sourceStep = sampleRate.toDouble() / WHISPER_SAMPLE_RATE_HZ.toDouble()

    return FloatArray(targetSampleCount) { targetIndex ->
        val sourcePosition = targetIndex * sourceStep
        val leftIndex = floor(sourcePosition).toInt().coerceIn(0, source.lastIndex)
        val rightIndex = (leftIndex + 1).coerceAtMost(source.lastIndex)
        val fraction = (sourcePosition - leftIndex).toFloat()
        source[leftIndex] + (source[rightIndex] - source[leftIndex]) * fraction
    }
}

private const val PCM_BYTES_PER_SAMPLE = 2
private const val WHISPER_SAMPLE_RATE_HZ = 16_000
