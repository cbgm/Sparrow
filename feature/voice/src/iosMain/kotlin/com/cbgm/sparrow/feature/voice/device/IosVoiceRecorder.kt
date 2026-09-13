package com.cbgm.sparrow.feature.voice.device

import kotlinx.cinterop.ExperimentalForeignApi
import com.cbgm.sparrow.feature.voice.domain.model.VoiceRecording
import kotlinx.coroutines.suspendCancellableCoroutine
import platform.AVFAudio.AVAudioEngine
import platform.AVFAudio.AVAudioSession
import platform.AVFAudio.AVAudioSessionCategoryPlayAndRecord
import kotlin.coroutines.resume

@OptIn(ExperimentalForeignApi::class)
class IosVoiceRecorder : VoiceRecorder {
    private var audioEngine: AVAudioEngine? = null
    private var pcmChunks = mutableListOf<ByteArray>()
    private var recordedPcmBytes = 0L
    private var sampleRate = DEFAULT_SAMPLE_RATE

    override suspend fun start(): Result<Unit> = runCatching {
        check(audioEngine == null) { "Voice recording is already active" }
        check(requestMicrophonePermission()) { "Microphone permission is required" }

        val session = AVAudioSession.sharedInstance()
        check(session.setCategory(AVAudioSessionCategoryPlayAndRecord, error = null)) {
            "Could not configure the audio session"
        }
        check(session.setActive(true, error = null)) {
            "Could not activate the audio session"
        }

        val engine = AVAudioEngine()
        val inputNode = engine.inputNode
        val format = inputNode.outputFormatForBus(0u)
        sampleRate = format.sampleRate.toInt().coerceAtLeast(1)
        pcmChunks = mutableListOf()
        recordedPcmBytes = 0L

        inputNode.installTapOnBus(
            bus = 0u,
            bufferSize = BUFFER_SIZE,
            format = format
        ) { buffer, _ ->
            val pcmBuffer = buffer ?: return@installTapOnBus
            val channels = pcmBuffer.floatChannelData ?: return@installTapOnBus
            val channel = channels[0] ?: return@installTapOnBus
            val frameCount = pcmBuffer.frameLength.toInt()

            val chunk = ByteArray(frameCount * BYTES_PER_SAMPLE)
            repeat(frameCount) { index ->
                val sample =
                    (channel[index].coerceIn(-1f, 1f) * Short.MAX_VALUE.toFloat())
                        .toInt()
                writeInt16(chunk, index * BYTES_PER_SAMPLE, sample)
            }
            pcmChunks += chunk
            recordedPcmBytes += chunk.size
        }

        engine.prepare()
        check(engine.startAndReturnError(null)) { "Could not start voice recording" }
        audioEngine = engine
    }.onFailure {
        releaseEngine()
    }

    override suspend fun stop(): Result<VoiceRecording> = runCatching {
        val engine = checkNotNull(audioEngine) { "Voice recording is not active" }
        engine.stop()
        engine.inputNode.removeTapOnBus(0u)
        audioEngine = null

        val pcmBytes = mergePcmChunks()
        check(pcmBytes.isNotEmpty()) { "Voice recording is empty" }

        VoiceRecording(
            bytes = createPcmWave(pcmBytes, sampleRate),
            mimeType = VOICE_MIME_TYPE,
            durationMilliseconds =
                recordedPcmBytes * 1_000L / (sampleRate.toLong() * CHANNEL_COUNT * BYTES_PER_SAMPLE)
        )
    }.onFailure {
        releaseEngine()
    }

    override suspend fun cancel() {
        releaseEngine()
        pcmChunks.clear()
        recordedPcmBytes = 0L
    }

    private suspend fun requestMicrophonePermission(): Boolean =
        suspendCancellableCoroutine { continuation ->
            AVAudioSession.sharedInstance().requestRecordPermission { granted ->
                if (continuation.isActive) continuation.resume(granted)
            }
        }

    private fun releaseEngine() {
        val engine = audioEngine ?: return
        engine.stop()
        engine.inputNode.removeTapOnBus(0u)
        audioEngine = null
    }

    private fun mergePcmChunks(): ByteArray {
        val result = ByteArray(recordedPcmBytes.toInt())
        var offset = 0
        pcmChunks.forEach { chunk ->
            chunk.copyInto(result, destinationOffset = offset)
            offset += chunk.size
        }
        pcmChunks.clear()
        return result
    }

    private fun createPcmWave(
        pcmBytes: ByteArray,
        sampleRate: Int
    ): ByteArray {
        val output = ByteArray(WAVE_HEADER_SIZE + pcmBytes.size)
        writeAscii(output, 0, "RIFF")
        writeInt32(output, 4, 36 + pcmBytes.size)
        writeAscii(output, 8, "WAVE")
        writeAscii(output, 12, "fmt ")
        writeInt32(output, 16, 16)
        writeInt16(output, 20, 1)
        writeInt16(output, 22, CHANNEL_COUNT)
        writeInt32(output, 24, sampleRate)
        writeInt32(output, 28, sampleRate * CHANNEL_COUNT * BYTES_PER_SAMPLE)
        writeInt16(output, 32, CHANNEL_COUNT * BYTES_PER_SAMPLE)
        writeInt16(output, 34, BITS_PER_SAMPLE)
        writeAscii(output, 36, "data")
        writeInt32(output, 40, pcmBytes.size)
        pcmBytes.copyInto(output, destinationOffset = WAVE_HEADER_SIZE)
        return output
    }

    private fun writeAscii(target: ByteArray, offset: Int, value: String) {
        value.forEachIndexed { index, character -> target[offset + index] = character.code.toByte() }
    }

    private fun writeInt16(target: ByteArray, offset: Int, value: Int) {
        target[offset] = value.toByte()
        target[offset + 1] = (value ushr 8).toByte()
    }

    private fun writeInt32(target: ByteArray, offset: Int, value: Int) {
        target[offset] = value.toByte()
        target[offset + 1] = (value ushr 8).toByte()
        target[offset + 2] = (value ushr 16).toByte()
        target[offset + 3] = (value ushr 24).toByte()
    }

    private companion object {
        const val DEFAULT_SAMPLE_RATE = 44_100
        const val BUFFER_SIZE = 1_024u
        const val CHANNEL_COUNT = 1
        const val BITS_PER_SAMPLE = 16
        const val BYTES_PER_SAMPLE = BITS_PER_SAMPLE / 8
        const val WAVE_HEADER_SIZE = 44
        const val VOICE_MIME_TYPE = "audio/wav"
    }
}
