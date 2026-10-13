package com.cbgm.sparrow.feature.chats.presentation.group

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cbgm.sparrow.feature.expenses.presentation.overview.ExpenseOverviewScreen
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun GroupExpenseOverviewRoute(viewModel: GroupExpenseOverviewViewModel = koinViewModel()) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    ExpenseOverviewScreen(uiState = uiState, onUiEvent = viewModel::onUiEvent)
}
