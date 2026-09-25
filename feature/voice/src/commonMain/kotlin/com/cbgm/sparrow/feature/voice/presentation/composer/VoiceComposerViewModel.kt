package com.cbgm.sparrow.feature.voice.presentation.composer

import androidx.lifecycle.viewModelScope
import com.cbgm.sparrow.core.ui.presentation.BaseViewModel
import com.cbgm.sparrow.feature.voice.domain.usecase.CancelVoiceRecordingUseCase
import com.cbgm.sparrow.feature.voice.domain.usecase.ObserveVoiceComposerUseCase
import com.cbgm.sparrow.feature.voice.domain.usecase.StartVoiceRecordingUseCase
import com.cbgm.sparrow.feature.voice.domain.usecase.StopVoiceRecordingUseCase
import com.cbgm.sparrow.feature.voice.domain.usecase.ToggleVoicePreviewUseCase
import com.cbgm.sparrow.feature.voice.presentation.composer.model.VoiceComposerUiState
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class VoiceComposerViewModel(
    observeVoiceComposer: ObserveVoiceComposerUseCase,
    private val startVoiceRecording: StartVoiceRecordingUseCase,
    private val stopVoiceRecording: StopVoiceRecordingUseCase,
    private val toggleVoicePreview: ToggleVoicePreviewUseCase,
    private val cancelVoiceRecording: CancelVoiceRecordingUseCase
) : BaseViewModel() {
    val uiState =
        observeVoiceComposer()
            .map { state ->
                VoiceComposerUiState(
                    phase = state.phase,
                    durationMilliseconds = state.durationMilliseconds,
                    isPlaying = state.isPlaying,
                    playbackProgress = state.playbackProgress,
                    waveform = state.waveform
                )
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000L),
                initialValue = VoiceComposerUiState()
            )

    fun startRecording() {
        viewModelScope.launch { startVoiceRecording() }
    }

    fun stopRecording() {
        viewModelScope.launch { stopVoiceRecording() }
    }

    fun togglePreview() {
        toggleVoicePreview()
    }

    fun cancel() {
        viewModelScope.launch { cancelVoiceRecording() }
    }
}
