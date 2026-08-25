package com.cbgm.sparrow.feature.attachments.presentation.management

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun AttachmentManagementRoute(
    viewModel: AttachmentManagementViewModel = koinViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    AttachmentManagementScreen(
        uiState = uiState,
        onUiEvent = viewModel::onUiEvent
    )
}
