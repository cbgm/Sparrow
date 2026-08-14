package com.cbgm.securechat.feature.settings.presentation.licenses

import androidx.lifecycle.viewModelScope
import com.cbgm.securechat.core.ui.presentation.BaseViewModel
import com.cbgm.securechat.feature.settings.domain.usecase.GetLicensesUseCase
import com.cbgm.securechat.feature.settings.presentation.licenses.model.LicensesUiEvent
import com.cbgm.securechat.feature.settings.presentation.licenses.model.LicensesUiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class LicensesViewModel(
    private val getLicenses: GetLicensesUseCase
) : BaseViewModel() {
    private val _uiState = MutableStateFlow(LicensesUiState())
    val uiState: StateFlow<LicensesUiState> = _uiState.asStateFlow()

    fun onUiEvent(event: LicensesUiEvent) {
        when (event) {
            LicensesUiEvent.BackClicked -> navigator.popBackStack()
        }
    }

    init {
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    libraries = getLicenses()
                )
            }
        }
    }
}
