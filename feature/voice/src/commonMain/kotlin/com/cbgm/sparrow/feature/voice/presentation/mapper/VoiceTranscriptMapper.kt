package com.cbgm.sparrow.feature.voice.presentation.mapper

import com.cbgm.sparrow.feature.attachments.domain.model.AttachmentTranscript
import com.cbgm.sparrow.feature.voice.domain.model.VoiceTranscript
import com.cbgm.sparrow.feature.voice.domain.model.VoiceTranscriptCue

internal fun AttachmentTranscript.toVoiceTranscript(): VoiceTranscript =
    VoiceTranscript(
        text = text,
        cues =
            cues.map { cue ->
                VoiceTranscriptCue(
                    text = cue.text,
                    startMilliseconds = cue.startMilliseconds,
                    endMilliseconds = cue.endMilliseconds
                )
            }
    )
