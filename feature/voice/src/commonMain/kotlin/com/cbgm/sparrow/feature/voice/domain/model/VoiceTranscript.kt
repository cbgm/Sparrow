package com.cbgm.sparrow.feature.voice.domain.model

data class VoiceTranscript(
    val text: String,
    val cues: List<VoiceTranscriptCue> = emptyList()
)

data class VoiceTranscriptCue(
    val text: String,
    val startMilliseconds: Long,
    val endMilliseconds: Long
)
