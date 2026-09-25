package com.cbgm.sparrow.feature.voice.device

import com.cbgm.sparrow.feature.voice.domain.model.VoiceRecording

interface VoiceRecorder {
    suspend fun start(): Result<Unit>

    suspend fun stop(): Result<VoiceRecording>

    suspend fun cancel()
}
