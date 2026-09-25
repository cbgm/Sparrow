package com.cbgm.sparrow.feature.voice.device

import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals

class PcmWaveAudioTest {
    @Test
    fun parsesRecordedVoiceWave() {
        val pcmBytes = byteArrayOf(0x01, 0x00, 0xff.toByte(), 0x7f)

        val wave = buildWave(pcmBytes).toPcmWaveAudio()

        assertEquals(16_000, wave.sampleRate)
        assertEquals(1, wave.channelCount)
        assertEquals(16, wave.bitsPerSample)
        assertContentEquals(pcmBytes, wave.pcmBytes)
    }

    private fun buildWave(pcmBytes: ByteArray): ByteArray =
        ByteArray(44 + pcmBytes.size).also { output ->
            writeAscii(output, 0, "RIFF")
            writeInt32Le(output, 4, output.size - 8)
            writeAscii(output, 8, "WAVE")
            writeAscii(output, 12, "fmt ")
            writeInt32Le(output, 16, 16)
            writeInt16Le(output, 20, 1)
            writeInt16Le(output, 22, 1)
            writeInt32Le(output, 24, 16_000)
            writeInt32Le(output, 28, 32_000)
            writeInt16Le(output, 32, 2)
            writeInt16Le(output, 34, 16)
            writeAscii(output, 36, "data")
            writeInt32Le(output, 40, pcmBytes.size)
            pcmBytes.copyInto(output, destinationOffset = 44)
        }

    private fun writeAscii(target: ByteArray, offset: Int, value: String) {
        value.forEachIndexed { index, char -> target[offset + index] = char.code.toByte() }
    }

    private fun writeInt16Le(target: ByteArray, offset: Int, value: Int) {
        target[offset] = value.toByte()
        target[offset + 1] = (value ushr 8).toByte()
    }

    private fun writeInt32Le(target: ByteArray, offset: Int, value: Int) {
        target[offset] = value.toByte()
        target[offset + 1] = (value ushr 8).toByte()
        target[offset + 2] = (value ushr 16).toByte()
        target[offset + 3] = (value ushr 24).toByte()
    }
}
