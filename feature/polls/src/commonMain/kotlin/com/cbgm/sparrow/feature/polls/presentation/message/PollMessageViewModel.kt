package com.cbgm.sparrow.feature.polls.presentation.message

import androidx.lifecycle.viewModelScope
import com.cbgm.sparrow.core.messagepart.ui.model.PollUi
import com.cbgm.sparrow.core.time.SystemClock
import com.cbgm.sparrow.core.ui.presentation.BaseViewModel
import com.cbgm.sparrow.feature.polls.presentation.message.mapper.toPollMessageUiState
import com.cbgm.sparrow.feature.polls.presentation.message.model.PollMessageUiState
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.milliseconds

class PollMessageViewModel(
    private val part: PollUi
) : BaseViewModel() {
    private val _uiState = MutableStateFlow(part.toPollMessageUiState())
    val uiState: StateFlow<PollMessageUiState> = _uiState.asStateFlow()

    private var expiryJob: Job? = null

    init {
        scheduleExpiry(part.expiresAtEpochMilliseconds)
    }

    private fun scheduleExpiry(expiresAt: Long?) {
        expiryJob?.cancel()
        if (expiresAt == null) return
        expiryJob = viewModelScope.launch {
            delay((expiresAt - SystemClock.nowEpochMilliseconds()).coerceAtLeast(0L).milliseconds)
            _uiState.update { state ->
                state.copy(
                    isExpired = true,
                    canInteract = false,
                    canSubmitVote = false
                )
            }
        }
    }

    fun onOptionClick(optionId: String) {
        _uiState.update { state ->
            if (!state.canInteract || state.options.none { it.id == optionId }) return@update state

            val selectedOptionIds =
                if (part.allowMultipleSelection) {
                    if (optionId in state.draftOptionIds) {
                        state.draftOptionIds - optionId
                    } else {
                        state.draftOptionIds + optionId
                    }
                } else if (state.draftOptionIds == setOf(optionId)) {
                    emptySet()
                } else {
                    setOf(optionId)
                }

            state.copy(
                draftOptionIds = selectedOptionIds,
                options =
                    state.options.map { option ->
                        option.copy(isSelected = option.id in selectedOptionIds)
                    },
                canSubmitVote =
                    selectedOptionIds.isNotEmpty() &&
                        selectedOptionIds != state.submittedOptionIds,
                isChangingVote = state.submittedOptionIds.isNotEmpty()
            )
        }
    }

    fun submitVote(onSubmit: (Set<String>) -> Unit) {
        val state = _uiState.value
        if (!state.canSubmitVote) return
        onSubmit(state.draftOptionIds)
    }

    fun openVoters() {
        _uiState.update { state ->
            if (!state.canShowVotes || state.votersOverlay == null) return@update state
            state.copy(isVotersOverlayVisible = true)
        }
    }

    fun dismissVoters() {
        _uiState.update { state ->
            state.copy(isVotersOverlayVisible = false)
        }
    }
}
