package com.cbgm.sparrow.feature.expenses.presentation.detail.model

sealed interface ExpenseDetailUiEvent {
    data object BackClicked : ExpenseDetailUiEvent

    data object EditClicked : ExpenseDetailUiEvent

    data object DeleteConfirmed : ExpenseDetailUiEvent
}
