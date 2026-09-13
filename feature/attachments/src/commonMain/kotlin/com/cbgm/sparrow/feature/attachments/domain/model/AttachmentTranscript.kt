package com.cbgm.sparrow.feature.attachments.domain.model

data class AttachmentTranscript(
    val text: String,
    val cues: List<AttachmentTranscriptCue> = emptyList()
)

data class AttachmentTranscriptCue(
    val text: String,
    val startMilliseconds: Long,
    val endMilliseconds: Long
)
