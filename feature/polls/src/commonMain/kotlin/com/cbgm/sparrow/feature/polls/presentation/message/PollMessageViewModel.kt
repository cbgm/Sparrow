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
                    canInteract = false
                )
            }
        }
    }

    fun onOptionClick(
        optionId: String,
        onSubmit: (Set<String>) -> Unit
    ) {
        val state = _uiState.value
        if (!state.canInteract || state.options.none { it.id == optionId }) return

        val selectedOptionIds =
            if (part.allowMultipleSelection) {
                if (optionId in state.submittedOptionIds) {
                    state.submittedOptionIds - optionId
                } else {
                    state.submittedOptionIds + optionId
                }
            } else {
                setOf(optionId)
            }

        if (selectedOptionIds.isEmpty() || selectedOptionIds == state.submittedOptionIds) return

        _uiState.value =
            state.copy(
                submittedOptionIds = selectedOptionIds,
                options =
                    state.options.map { option ->
                        option.copy(isSelected = option.id in selectedOptionIds)
                    }
            )
        onSubmit(selectedOptionIds)
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
