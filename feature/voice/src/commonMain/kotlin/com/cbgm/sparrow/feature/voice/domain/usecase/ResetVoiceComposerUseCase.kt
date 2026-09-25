package com.cbgm.sparrow.feature.voice.domain.usecase

import com.cbgm.sparrow.feature.voice.domain.repository.VoiceRepository

class ResetVoiceComposerUseCase(
    private val repository: VoiceRepository
) {
    operator fun invoke() = repository.resetComposer()
}
