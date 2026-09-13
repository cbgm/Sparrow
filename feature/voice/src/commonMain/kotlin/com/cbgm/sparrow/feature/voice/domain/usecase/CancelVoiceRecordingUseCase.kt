package com.cbgm.sparrow.feature.voice.domain.usecase

import com.cbgm.sparrow.feature.voice.domain.repository.VoiceRepository

class CancelVoiceRecordingUseCase(
    private val repository: VoiceRepository
) {
    suspend operator fun invoke() = repository.cancelRecording()
}
