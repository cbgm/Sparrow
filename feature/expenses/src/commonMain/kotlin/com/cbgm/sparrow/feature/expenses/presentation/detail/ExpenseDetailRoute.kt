package com.cbgm.sparrow.feature.expenses.presentation.detail

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun ExpenseDetailRoute(viewModel: ExpenseDetailViewModel = koinViewModel()) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    ExpenseDetailScreen(uiState = uiState, onUiEvent = viewModel::onUiEvent)
}
