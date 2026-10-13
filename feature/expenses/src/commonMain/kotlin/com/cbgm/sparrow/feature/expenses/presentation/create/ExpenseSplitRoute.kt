package com.cbgm.sparrow.feature.expenses.presentation.create

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun ExpenseSplitRoute(
    viewModelStoreOwner: ViewModelStoreOwner
) {
    // The split destination edits the draft owned by the parent Add Expenses screen.
    // Popping this destination keeps the original draft and queued expenses intact.
    val viewModel: CreateExpenseViewModel = koinViewModel(viewModelStoreOwner = viewModelStoreOwner)
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    ExpenseSplitScreen(uiState = uiState, onUiEvent = viewModel::onUiEvent)
}
