package com.cbgm.sparrow.feature.voice.domain.model

data class VoiceComposerState(
    val phase: VoiceComposerPhase = VoiceComposerPhase.READY,
    val durationMilliseconds: Long = 0L,
    val isPlaying: Boolean = false,
    val playbackProgress: Float = 0f,
    val waveform: List<Float> = emptyList()
)

enum class VoiceComposerPhase {
    READY,
    RECORDING,
    RECORDED
}

data class VoicePlaybackState(
    val attachmentId: String? = null,
    val positionMilliseconds: Long = 0L,
    val isPlaying: Boolean = false
)

sealed interface VoiceTranscriptionState {
    data object Idle : VoiceTranscriptionState

    data object Downloading : VoiceTranscriptionState

    data object Preparing : VoiceTranscriptionState

    data class Transcribing(
        val progressPercent: Int? = null
    ) : VoiceTranscriptionState {
        init {
            require(progressPercent == null || progressPercent in 1..100) {
                "Transcription progress must be between 1 and 100 when available"
            }
        }
    }

    data class Success(
        val transcript: VoiceTranscript
    ) : VoiceTranscriptionState

    data class Error(
        val throwable: Throwable
    ) : VoiceTranscriptionState
}
