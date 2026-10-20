package com.cbgm.sparrow.feature.identity.presentation.restore.model

sealed interface IdentityRestoreUiEvent {
    data class PasswordChanged(
        val value: String
    ) : IdentityRestoreUiEvent

    data object ConfirmClicked : IdentityRestoreUiEvent

    data object DismissClicked : IdentityRestoreUiEvent
}
