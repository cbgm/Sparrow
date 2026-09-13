package com.cbgm.sparrow.feature.voice.domain.repository

import com.cbgm.sparrow.feature.voice.domain.model.VoiceTranscript

interface VoiceTranscriptionRepository {
    suspend fun prepare(): Result<Unit>

    suspend fun transcribe(bytes: ByteArray): Result<VoiceTranscript>
}
