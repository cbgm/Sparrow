package com.cbgm.sparrow.feature.voice.device

internal data class PcmWaveAudio(
    val sampleRate: Int,
    val channelCount: Int,
    val bitsPerSample: Int,
    val pcmBytes: ByteArray
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as PcmWaveAudio

        if (sampleRate != other.sampleRate) return false
        if (channelCount != other.channelCount) return false
        if (bitsPerSample != other.bitsPerSample) return false
        if (!pcmBytes.contentEquals(other.pcmBytes)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = sampleRate
        result = 31 * result + channelCount
        result = 31 * result + bitsPerSample
        result = 31 * result + pcmBytes.contentHashCode()
        return result
    }
}

internal fun ByteArray.toPcmWaveAudio(): PcmWaveAudio {
    require(size >= 44) { "Voice message is not a valid WAV payload" }
    require(ascii(0, 4) == "RIFF" && ascii(8, 4) == "WAVE") {
        "Voice message is not a PCM WAV payload"
    }

    var offset = 12
    var sampleRate: Int? = null
    var channelCount: Int? = null
    var bitsPerSample: Int? = null
    var pcmBytes: ByteArray? = null

    while (offset + 8 <= size) {
        val chunkId = ascii(offset, 4)
        val chunkSize = int32Le(offset + 4)
        val chunkStart = offset + 8
        val chunkEnd = chunkStart + chunkSize
        require(chunkSize >= 0 && chunkEnd <= size) { "Invalid WAV chunk" }

        when (chunkId) {
            "fmt " -> {
                require(chunkSize >= 16) { "Invalid WAV format chunk" }
                require(int16Le(chunkStart) == 1) { "Only PCM voice messages can be transcribed" }
                channelCount = int16Le(chunkStart + 2)
                sampleRate = int32Le(chunkStart + 4)
                bitsPerSample = int16Le(chunkStart + 14)
            }

            "data" -> pcmBytes = copyOfRange(chunkStart, chunkEnd)
        }

        offset = chunkEnd + (chunkSize and 1)
    }

    val resolvedSampleRate = requireNotNull(sampleRate) { "WAV sample rate is missing" }
    val resolvedChannelCount = requireNotNull(channelCount) { "WAV channel count is missing" }
    val resolvedBitsPerSample = requireNotNull(bitsPerSample) { "WAV sample format is missing" }
    val resolvedPcmBytes = requireNotNull(pcmBytes) { "WAV audio data is missing" }

    require(resolvedSampleRate > 0) { "Invalid WAV sample rate" }
    require(resolvedChannelCount == 1) { "Only mono voice messages can be transcribed" }
    require(resolvedBitsPerSample == 16) { "Only 16-bit voice messages can be transcribed" }
    require(resolvedPcmBytes.isNotEmpty()) { "Voice message is empty" }

    return PcmWaveAudio(
        sampleRate = resolvedSampleRate,
        channelCount = resolvedChannelCount,
        bitsPerSample = resolvedBitsPerSample,
        pcmBytes = resolvedPcmBytes
    )
}

private fun ByteArray.ascii(offset: Int, length: Int): String =
    buildString(length) {
        repeat(length) { index -> append(this@ascii[offset + index].toInt().toChar()) }
    }

private fun ByteArray.int16Le(offset: Int): Int =
    (this[offset].toInt() and 0xff) or
        ((this[offset + 1].toInt() and 0xff) shl 8)

private fun ByteArray.int32Le(offset: Int): Int =
    (this[offset].toInt() and 0xff) or
        ((this[offset + 1].toInt() and 0xff) shl 8) or
        ((this[offset + 2].toInt() and 0xff) shl 16) or
        ((this[offset + 3].toInt() and 0xff) shl 24)
