package com.cbgm.securechat.feature.settings.presentation.licenses.model

sealed interface LicensesUiEvent {
    data object BackClicked : LicensesUiEvent
}
