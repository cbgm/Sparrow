package com.cbgm.sparrow.feature.settings.presentation.errors.model

data class DeveloperErrorLogUiState(
    val errors: List<DeveloperErrorUi> = emptyList(),
    val isClearing: Boolean = false,
    val showClearConfirmation: Boolean = false
)
