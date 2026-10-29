package com.cbgm.sparrow.feature.identity.presentation.repair.model

sealed interface IdentityRepairUiEvent {
    data object RestoreClicked : IdentityRepairUiEvent

    data class PasswordChanged(
        val value: String
    ) : IdentityRepairUiEvent

    data object PasswordConfirmed : IdentityRepairUiEvent

    data object PasswordDismissed : IdentityRepairUiEvent

    data object ResetClicked : IdentityRepairUiEvent

    data object ResetConfirmed : IdentityRepairUiEvent

    data object ResetDismissed : IdentityRepairUiEvent
}
