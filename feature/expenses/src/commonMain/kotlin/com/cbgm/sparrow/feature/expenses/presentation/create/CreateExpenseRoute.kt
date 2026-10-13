package com.cbgm.sparrow.feature.expenses.presentation.create

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun CreateExpenseRoute(viewModel: CreateExpenseViewModel = koinViewModel()) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    CreateExpenseScreen(uiState = uiState, onUiEvent = viewModel::onUiEvent)
}
