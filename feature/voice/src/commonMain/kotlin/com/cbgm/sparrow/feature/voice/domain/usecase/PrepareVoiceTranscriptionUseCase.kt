package com.cbgm.sparrow.feature.voice.domain.usecase

import com.cbgm.sparrow.feature.voice.domain.repository.VoiceTranscriptionRepository

class PrepareVoiceTranscriptionUseCase(
    private val repository: VoiceTranscriptionRepository
) {
    suspend operator fun invoke(): Result<Unit> = repository.prepare()
}
