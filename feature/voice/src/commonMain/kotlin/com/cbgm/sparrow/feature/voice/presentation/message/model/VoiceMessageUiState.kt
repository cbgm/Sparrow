package com.cbgm.sparrow.feature.voice.presentation.message.model

import com.cbgm.sparrow.feature.voice.domain.model.VoiceTranscript
import com.cbgm.sparrow.feature.voice.domain.model.VoiceTranscriptionState

data class VoiceMessageUiState(
    val playbackPositionMilliseconds: Long = 0L,
    val isPlaying: Boolean = false,
    val transcriptionEnabled: Boolean = false,
    val transcript: VoiceTranscript? = null,
    val transcriptionState: VoiceTranscriptionState = VoiceTranscriptionState.Idle
)
