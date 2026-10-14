package com.cbgm.sparrow.feature.expenses.presentation.create

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigationevent.NavigationEventInfo
import androidx.navigationevent.compose.NavigationBackHandler
import androidx.navigationevent.compose.rememberNavigationEventState
import com.cbgm.sparrow.core.messagepart.domain.model.MessageAttachmentPolicy
import com.cbgm.sparrow.feature.expenses.presentation.create.model.CreateExpenseUiEvent
import com.cbgm.sparrow.feature.media.presentation.selection.rememberDocumentScanLauncher
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun CreateExpenseRoute(viewModel: CreateExpenseViewModel = koinViewModel()) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val receiptScanner =
        rememberDocumentScanLauncher(
            maxImageDimension = MessageAttachmentPolicy.MAX_IMAGE_DIMENSION,
            maxImageBytes = MessageAttachmentPolicy.MAX_IMAGE_BYTES,
            onResult = { viewModel.onUiEvent(CreateExpenseUiEvent.ReceiptScanned(it)) }
        )
    if (uiState.isSplitOpen) {
        NavigationBackHandler(
            state = rememberNavigationEventState(NavigationEventInfo.None),
            onBackCompleted = { viewModel.onUiEvent(CreateExpenseUiEvent.BackClicked) }
        )
        ExpenseSplitScreen(uiState = uiState, onUiEvent = viewModel::onUiEvent)
    } else {
        CreateExpenseScreen(
            uiState = uiState,
            onUiEvent = viewModel::onUiEvent,
            onScanReceipt = receiptScanner::launch
        )
    }
}
