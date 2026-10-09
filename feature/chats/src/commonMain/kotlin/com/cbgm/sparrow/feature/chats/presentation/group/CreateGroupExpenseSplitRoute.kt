package com.cbgm.sparrow.feature.chats.presentation.group

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cbgm.sparrow.feature.expenses.presentation.create.ExpenseSplitScreen
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun CreateGroupExpenseSplitRoute(
    viewModelStoreOwner: ViewModelStoreOwner
) {
    // The split destination edits the draft owned by the parent Add Expenses screen.
    // Popping this destination keeps the original draft and queued expenses intact.
    val viewModel: CreateGroupExpenseViewModel = koinViewModel(viewModelStoreOwner = viewModelStoreOwner)
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    ExpenseSplitScreen(uiState = uiState, onUiEvent = viewModel::onUiEvent)
}
