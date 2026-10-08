package com.cbgm.sparrow.feature.chats.presentation.group

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cbgm.sparrow.feature.expenses.presentation.create.CreateExpenseScreen
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun CreateGroupExpenseRoute(viewModel: CreateGroupExpenseViewModel = koinViewModel()) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    CreateExpenseScreen(uiState = uiState, onUiEvent = viewModel::onUiEvent)
}
