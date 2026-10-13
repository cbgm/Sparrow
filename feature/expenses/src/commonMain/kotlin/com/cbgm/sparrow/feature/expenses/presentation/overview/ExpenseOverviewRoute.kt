package com.cbgm.sparrow.feature.expenses.presentation.overview

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun ExpenseOverviewRoute(viewModel: ExpenseOverviewViewModel = koinViewModel()) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    ExpenseOverviewScreen(uiState = uiState, onUiEvent = viewModel::onUiEvent)
}
