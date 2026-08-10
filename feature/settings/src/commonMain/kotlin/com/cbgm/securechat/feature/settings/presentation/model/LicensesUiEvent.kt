package com.cbgm.securechat.feature.settings.presentation.model

sealed interface LicensesUiEvent {
    data object BackClicked : LicensesUiEvent
}
