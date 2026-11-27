package com.cbgm.sparrow.startup.presentation.start.model

sealed interface StartupUiState {
    data object Loading : StartupUiState

    data object Ready : StartupUiState

    data object IdentityRequired : StartupUiState

    data object IdentityUnusable : StartupUiState

    data class Error(
        val message: String
    ) : StartupUiState
}
