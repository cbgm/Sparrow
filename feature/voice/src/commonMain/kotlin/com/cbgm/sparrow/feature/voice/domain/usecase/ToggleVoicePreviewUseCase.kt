package com.cbgm.sparrow.feature.voice.domain.usecase

import com.cbgm.sparrow.feature.voice.domain.repository.VoiceRepository

class ToggleVoicePreviewUseCase(
    private val repository: VoiceRepository
) {
    operator fun invoke(): Result<Unit> = repository.togglePreview()
}
