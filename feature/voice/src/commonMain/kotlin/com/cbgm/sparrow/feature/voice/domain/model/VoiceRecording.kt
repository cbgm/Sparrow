package com.cbgm.sparrow.feature.voice.domain.model

data class VoiceRecording(
    val bytes: ByteArray,
    val mimeType: String,
    val durationMilliseconds: Long
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is VoiceRecording) return false
        return durationMilliseconds == other.durationMilliseconds &&
            bytes.contentEquals(other.bytes) &&
            mimeType == other.mimeType
    }

    override fun hashCode(): Int {
        var result = durationMilliseconds.hashCode()
        result = 31 * result + bytes.contentHashCode()
        result = 31 * result + mimeType.hashCode()
        return result
    }
}
