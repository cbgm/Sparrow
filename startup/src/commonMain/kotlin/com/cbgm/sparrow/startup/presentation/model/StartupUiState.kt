package com.cbgm.sparrow.startup.presentation.model

sealed interface StartupUiState {
    data object Loading : StartupUiState

    data class Ready(
        val connection: StartupConnection
    ) : StartupUiState

    data object IdentityRequired : StartupUiState

    data class Error(
        val message: String
    ) : StartupUiState
}

enum class StartupConnection {
    ONLINE,
    OFFLINE
}
