package com.cbgm.sparrow.feature.settings.presentation.licenses.model

sealed interface LicensesUiEvent {
    data object BackClicked : LicensesUiEvent
}
