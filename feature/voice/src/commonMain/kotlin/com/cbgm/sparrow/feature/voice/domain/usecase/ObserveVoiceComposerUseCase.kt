package com.cbgm.sparrow.feature.voice.domain.usecase

import com.cbgm.sparrow.feature.voice.domain.model.VoiceComposerState
import com.cbgm.sparrow.feature.voice.domain.repository.VoiceRepository
import kotlinx.coroutines.flow.Flow

class ObserveVoiceComposerUseCase(
    private val repository: VoiceRepository
) {
    operator fun invoke(): Flow<VoiceComposerState> = repository.observeComposerState()
}
