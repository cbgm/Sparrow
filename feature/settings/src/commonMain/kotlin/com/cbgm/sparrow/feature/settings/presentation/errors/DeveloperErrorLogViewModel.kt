package com.cbgm.sparrow.feature.settings.presentation.errors

import androidx.lifecycle.viewModelScope
import com.cbgm.sparrow.core.ui.presentation.BaseViewModel
import com.cbgm.sparrow.feature.settings.domain.usecase.ClearDeveloperErrorsUseCase
import com.cbgm.sparrow.feature.settings.domain.usecase.ObserveDeveloperErrorsUseCase
import com.cbgm.sparrow.feature.settings.presentation.errors.mapper.toUiModel
import com.cbgm.sparrow.feature.settings.presentation.errors.model.DeveloperErrorLogUiEvent
import com.cbgm.sparrow.feature.settings.presentation.errors.model.DeveloperErrorLogUiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class DeveloperErrorLogViewModel(
    observeDeveloperErrorsUseCase: ObserveDeveloperErrorsUseCase,
    private val clearDeveloperErrorsUseCase: ClearDeveloperErrorsUseCase
) : BaseViewModel() {
    private val isClearing = MutableStateFlow(false)
    private val showClearConfirmation = MutableStateFlow(false)

    val uiState: StateFlow<DeveloperErrorLogUiState> =
        combine(
            observeDeveloperErrorsUseCase(),
            isClearing,
            showClearConfirmation
        ) { errors, clearing, showConfirmation ->
            DeveloperErrorLogUiState(
                errors = errors.map { error -> error.toUiModel() },
                isClearing = clearing,
                showClearConfirmation = showConfirmation
            )
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(stopTimeoutMillis = 5_000),
            initialValue = DeveloperErrorLogUiState()
        )

    fun onUiEvent(event: DeveloperErrorLogUiEvent) {
        when (event) {
            DeveloperErrorLogUiEvent.BackClicked -> navigator.popBackStack()
            DeveloperErrorLogUiEvent.ClearErrorsClicked -> showClearConfirmation.value = true
            DeveloperErrorLogUiEvent.ClearErrorsDismissed -> showClearConfirmation.value = false
            DeveloperErrorLogUiEvent.ClearErrorsConfirmed -> clearErrors()
        }
    }

    private fun clearErrors() {
        if (isClearing.value) return

        showClearConfirmation.value = false
        viewModelScope.launch {
            isClearing.value = true
            try {
                clearDeveloperErrorsUseCase()
            } finally {
                isClearing.value = false
            }
        }
    }
}
