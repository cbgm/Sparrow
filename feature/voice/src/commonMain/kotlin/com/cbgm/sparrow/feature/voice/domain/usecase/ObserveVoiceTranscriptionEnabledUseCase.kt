package com.cbgm.sparrow.feature.voice.domain.usecase

import com.cbgm.sparrow.feature.voice.domain.repository.VoiceTranscriptionSettingsRepository
import kotlinx.coroutines.flow.Flow

class ObserveVoiceTranscriptionEnabledUseCase(
    private val repository: VoiceTranscriptionSettingsRepository
) {
    operator fun invoke(): Flow<Boolean> = repository.observeEnabled()
}
