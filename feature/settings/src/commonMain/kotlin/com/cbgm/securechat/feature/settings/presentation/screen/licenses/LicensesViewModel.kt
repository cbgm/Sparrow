package com.cbgm.securechat.feature.settings.presentation.screen.licenses

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cbgm.securechat.feature.settings.domain.repository.LicensesRepository
import com.cbgm.securechat.feature.settings.presentation.model.LicensesUiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class LicensesViewModel(
    private val licensesRepository: LicensesRepository
) : ViewModel() {
    private val _uiState = MutableStateFlow(LicensesUiState())
    val uiState: StateFlow<LicensesUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    libraries = licensesRepository.getLibraries()
                )
            }
        }
    }
}
