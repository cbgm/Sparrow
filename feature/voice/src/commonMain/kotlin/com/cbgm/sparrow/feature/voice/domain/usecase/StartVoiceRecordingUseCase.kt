package com.cbgm.sparrow.feature.voice.domain.usecase

import com.cbgm.sparrow.feature.voice.domain.repository.VoiceRepository

class StartVoiceRecordingUseCase(
    private val repository: VoiceRepository
) {
    suspend operator fun invoke(): Result<Unit> = repository.startRecording()
}
