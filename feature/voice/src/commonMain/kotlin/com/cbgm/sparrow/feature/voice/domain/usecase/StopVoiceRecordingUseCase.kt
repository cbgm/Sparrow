package com.cbgm.sparrow.feature.voice.domain.usecase

import com.cbgm.sparrow.feature.voice.domain.repository.VoiceRepository

class StopVoiceRecordingUseCase(
    private val repository: VoiceRepository
) {
    suspend operator fun invoke(): Result<Unit> = repository.stopRecording()
}
