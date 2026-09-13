package com.cbgm.sparrow.feature.voice.domain.usecase

import com.cbgm.sparrow.feature.voice.domain.model.VoiceTranscript
import com.cbgm.sparrow.feature.voice.domain.repository.VoiceTranscriptionRepository

class TranscribeVoiceAudioUseCase(
    private val repository: VoiceTranscriptionRepository
) {
    suspend operator fun invoke(
        bytes: ByteArray,
        onProgress: (Int) -> Unit
    ): Result<VoiceTranscript> =
        repository.transcribe(
            bytes = bytes,
            onProgress = onProgress
        )
}
