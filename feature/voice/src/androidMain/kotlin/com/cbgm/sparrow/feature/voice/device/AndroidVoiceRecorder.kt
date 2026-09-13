package com.cbgm.sparrow.feature.voice.device

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import androidx.core.content.ContextCompat
import com.cbgm.sparrow.feature.voice.domain.model.VoiceRecording
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.ByteArrayOutputStream

class AndroidVoiceRecorder(
    private val context: Context
) : VoiceRecorder {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var audioRecord: AudioRecord? = null
    private var readJob: Job? = null
    private var output: ByteArrayOutputStream? = null
    private var recordedPcmBytes = 0L

    override suspend fun start(): Result<Unit> = runCatching {
        check(audioRecord == null) { "Voice recording is already active" }

        if (
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.RECORD_AUDIO
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            throw SecurityException("RECORD_AUDIO permission is required for voice messages")
        }

        val minBufferSize =
            AudioRecord.getMinBufferSize(
                SAMPLE_RATE_HZ,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT
            )
        check(minBufferSize > 0) { "Could not resolve an audio recording buffer size" }

        val recorder =
            AudioRecord(
                MediaRecorder.AudioSource.MIC,
                SAMPLE_RATE_HZ,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT,
                minBufferSize * 2
            )
        check(recorder.state == AudioRecord.STATE_INITIALIZED) {
            recorder.release()
            "Could not initialize voice recording"
        }

        val stream = ByteArrayOutputStream()
        audioRecord = recorder
        output = stream
        recordedPcmBytes = 0L
        recorder.startRecording()
        readJob =
            scope.launch {
                val buffer = ByteArray(minBufferSize)
                while (isActive && recorder.recordingState == AudioRecord.RECORDSTATE_RECORDING) {
                    val read = recorder.read(buffer, 0, buffer.size)
                    if (read > 0) {
                        stream.write(buffer, 0, read)
                        recordedPcmBytes += read
                    }
                }
            }
    }

    override suspend fun stop(): Result<VoiceRecording> = runCatching {
        val recorder = checkNotNull(audioRecord) { "Voice recording is not active" }
        recorder.stop()
        readJob?.cancelAndJoin()
        readJob = null
        recorder.release()
        audioRecord = null

        val pcmBytes = checkNotNull(output).toByteArray()
        output = null
        check(pcmBytes.isNotEmpty()) { "Voice recording is empty" }

        VoiceRecording(
            bytes = createPcmWave(pcmBytes),
            mimeType = VOICE_MIME_TYPE,
            durationMilliseconds =
                (recordedPcmBytes * 1_000L) /
                    (SAMPLE_RATE_HZ.toLong() * CHANNEL_COUNT * BYTES_PER_SAMPLE)
        )
    }.onFailure {
        release()
    }

    override suspend fun cancel() {
        release()
    }

    private suspend fun release() {
        runCatching { audioRecord?.stop() }
        readJob?.cancelAndJoin()
        readJob = null
        audioRecord?.release()
        audioRecord = null
        output = null
        recordedPcmBytes = 0L
    }

    private fun createPcmWave(pcmBytes: ByteArray): ByteArray {
        val output = ByteArray(44 + pcmBytes.size)
        writeAscii(output, 0, "RIFF")
        writeInt32(output, 4, 36 + pcmBytes.size)
        writeAscii(output, 8, "WAVE")
        writeAscii(output, 12, "fmt ")
        writeInt32(output, 16, 16)
        writeInt16(output, 20, 1)
        writeInt16(output, 22, CHANNEL_COUNT)
        writeInt32(output, 24, SAMPLE_RATE_HZ)
        writeInt32(output, 28, SAMPLE_RATE_HZ * CHANNEL_COUNT * BYTES_PER_SAMPLE)
        writeInt16(output, 32, CHANNEL_COUNT * BYTES_PER_SAMPLE)
        writeInt16(output, 34, BITS_PER_SAMPLE)
        writeAscii(output, 36, "data")
        writeInt32(output, 40, pcmBytes.size)
        pcmBytes.copyInto(output, destinationOffset = 44)
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
        const val SAMPLE_RATE_HZ = 16_000
        const val CHANNEL_COUNT = 1
        const val BITS_PER_SAMPLE = 16
        const val BYTES_PER_SAMPLE = BITS_PER_SAMPLE / 8
        const val VOICE_MIME_TYPE = "audio/wav"
    }
}
