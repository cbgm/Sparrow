package com.cbgm.sparrow.feature.voice.domain.usecase

import com.cbgm.sparrow.feature.voice.domain.model.VoicePlaybackState
import com.cbgm.sparrow.feature.voice.domain.repository.VoiceRepository
import kotlinx.coroutines.flow.Flow

class ObserveVoicePlaybackUseCase(
    private val repository: VoiceRepository
) {
    operator fun invoke(attachmentId: String): Flow<VoicePlaybackState> =
        repository.observePlaybackState(attachmentId)
}
