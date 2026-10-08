package com.cbgm.sparrow.feature.applock.presentation.model

sealed interface AppLockUiState {
    data object Loading : AppLockUiState

    data class Locked(
        val isAuthenticating: Boolean = false,
        val authenticationRequestId: Int = 0
    ) : AppLockUiState

    data object Unlocked : AppLockUiState
}
