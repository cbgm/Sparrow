package com.cbgm.sparrow.feature.voice.presentation.message

import androidx.lifecycle.viewModelScope
import com.cbgm.sparrow.core.ui.presentation.BaseViewModel
import com.cbgm.sparrow.feature.attachments.domain.model.AttachmentTranscript
import com.cbgm.sparrow.feature.attachments.domain.usecase.ObserveMessageAttachmentTranscriptUseCase
import com.cbgm.sparrow.feature.voice.domain.model.VoiceMessageTarget
import com.cbgm.sparrow.feature.voice.domain.model.VoiceTranscript
import com.cbgm.sparrow.feature.voice.domain.model.VoiceTranscriptCue
import com.cbgm.sparrow.feature.voice.domain.model.VoiceTranscriptionState
import com.cbgm.sparrow.feature.voice.domain.usecase.FinishVoiceMessageScrubUseCase
import com.cbgm.sparrow.feature.voice.domain.usecase.ObserveVoicePlaybackUseCase
import com.cbgm.sparrow.feature.voice.domain.usecase.ObserveVoiceTranscriptionEnabledUseCase
import com.cbgm.sparrow.feature.voice.domain.usecase.StartVoiceMessageScrubUseCase
import com.cbgm.sparrow.feature.voice.domain.usecase.ToggleVoiceMessagePlaybackUseCase
import com.cbgm.sparrow.feature.voice.domain.usecase.TranscribeVoiceMessageUseCase
import com.cbgm.sparrow.feature.voice.presentation.message.model.VoiceMessageUiState
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

class VoiceMessageViewModel(
    private val target: VoiceMessageTarget,
    observeVoicePlayback: ObserveVoicePlaybackUseCase,
    observeTranscriptionEnabled: ObserveVoiceTranscriptionEnabledUseCase,
    observePersistedTranscript: ObserveMessageAttachmentTranscriptUseCase,
    private val toggleVoicePlayback: ToggleVoiceMessagePlaybackUseCase,
    private val startVoiceScrub: StartVoiceMessageScrubUseCase,
    private val finishVoiceScrub: FinishVoiceMessageScrubUseCase,
    private val transcribeVoiceMessage: TranscribeVoiceMessageUseCase
) : BaseViewModel() {
    private val transcriptionState = MutableStateFlow<VoiceTranscriptionState>(VoiceTranscriptionState.Idle)
    private val _uiState = MutableStateFlow(VoiceMessageUiState())
    val uiState: StateFlow<VoiceMessageUiState> = _uiState.asStateFlow()
    private var transcriptionJob: Job? = null

    init {
        viewModelScope.launch {
            combine(
                observeVoicePlayback(target.attachmentId),
                observeTranscriptionEnabled(),
                observePersistedTranscript(target.attachmentId),
                transcriptionState
            ) { playback, enabled, persisted, transcription ->
                val completedTranscript =
                    (transcription as? VoiceTranscriptionState.Success)?.transcript
                        ?: persisted?.toVoiceTranscript()
                VoiceMessageUiState(
                    playbackPositionMilliseconds = playback.positionMilliseconds,
                    isPlaying = playback.isPlaying,
                    transcriptionEnabled = enabled,
                    transcript = completedTranscript,
                    transcriptionState = transcription
                )
            }.collect { state -> _uiState.value = state }
        }
    }

    fun togglePlayback() {
        viewModelScope.launch { toggleVoicePlayback(target) }
    }

    fun startScrub() {
        startVoiceScrub(target.attachmentId)
    }

    fun finishScrub(positionMilliseconds: Long) {
        viewModelScope.launch { finishVoiceScrub(target, positionMilliseconds) }
    }

    fun transcribe() {
        if (transcriptionJob?.isActive == true) return
        transcriptionJob =
            viewModelScope.launch {
                transcribeVoiceMessage(target).collect { state ->
                    transcriptionState.value = state
                }
            }
    }
}

private fun AttachmentTranscript.toVoiceTranscript(): VoiceTranscript =
    VoiceTranscript(
        text = text,
        cues =
            cues.map { cue ->
                VoiceTranscriptCue(
                    text = cue.text,
                    startMilliseconds = cue.startMilliseconds,
                    endMilliseconds = cue.endMilliseconds
                )
            }
    )
