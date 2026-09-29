package com.cbgm.sparrow.feature.applock.presentation

import androidx.lifecycle.viewModelScope
import com.cbgm.sparrow.core.logging.SparrowLog
import com.cbgm.sparrow.core.ui.presentation.BaseViewModel
import com.cbgm.sparrow.feature.applock.device.AppLockAuthenticationResult
import com.cbgm.sparrow.feature.applock.domain.usecase.ObserveAppLockEnabledUseCase
import com.cbgm.sparrow.feature.applock.presentation.model.AppLockUiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class AppLockViewModel(
    private val observeAppLockEnabled: ObserveAppLockEnabledUseCase
) : BaseViewModel() {
    private val logger = SparrowLog.withTag("AppLockViewModel")
    private val _uiState = MutableStateFlow<AppLockUiState>(AppLockUiState.Loading)
    val uiState: StateFlow<AppLockUiState> = _uiState.asStateFlow()

    init {
        resolveLockState()
    }

    fun requestAuthentication() {
        val state = _uiState.value as? AppLockUiState.Locked ?: return
        if (state.isAuthenticating) return

        _uiState.value = state.copy(
            isAuthenticating = true,
            authenticationRequestId = state.authenticationRequestId + 1
        )
    }

    fun onAuthenticationResult(result: AppLockAuthenticationResult) {
        when (result) {
            AppLockAuthenticationResult.Authenticated -> _uiState.value = AppLockUiState.Unlocked
            AppLockAuthenticationResult.Cancelled -> stopAuthenticating()
            AppLockAuthenticationResult.Unavailable -> {
                logger.error { "Device-owner authentication is unavailable" }
                stopAuthenticating()
            }
            is AppLockAuthenticationResult.Failed -> {
                logger.error { result.message.ifBlank { "Device-owner authentication failed" } }
                stopAuthenticating()
            }
        }
    }

    private fun resolveLockState() {
        viewModelScope.launch {
            _uiState.value = if (observeAppLockEnabled().first()) {
                AppLockUiState.Locked(isAuthenticating = true, authenticationRequestId = 1)
            } else {
                AppLockUiState.Unlocked
            }
        }
    }

    private fun stopAuthenticating() {
        val state = _uiState.value as? AppLockUiState.Locked ?: return
        _uiState.value = state.copy(isAuthenticating = false)
    }
}
