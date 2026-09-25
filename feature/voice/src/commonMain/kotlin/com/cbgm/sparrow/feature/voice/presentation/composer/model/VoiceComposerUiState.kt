package com.cbgm.sparrow.feature.voice.presentation.composer.model

import com.cbgm.sparrow.feature.voice.domain.model.VoiceComposerPhase

data class VoiceComposerUiState(
    val phase: VoiceComposerPhase = VoiceComposerPhase.READY,
    val durationMilliseconds: Long = 0L,
    val isPlaying: Boolean = false,
    val playbackProgress: Float = 0f,
    val waveform: List<Float> = emptyList()
)
