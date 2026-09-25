package com.cbgm.sparrow.feature.voice.domain.usecase

import com.cbgm.sparrow.feature.voice.domain.model.VoiceComposerPhase
import com.cbgm.sparrow.feature.voice.domain.repository.VoiceRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

class ObserveVoiceRecordingActiveUseCase(
    private val repository: VoiceRepository
) {
    operator fun invoke(): Flow<Boolean> =
        repository.observeComposerState()
            .map { it.phase == VoiceComposerPhase.RECORDING }
            .distinctUntilChanged()
}
