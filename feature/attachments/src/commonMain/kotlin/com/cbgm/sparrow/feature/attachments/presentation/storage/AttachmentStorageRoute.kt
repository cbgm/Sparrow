package com.cbgm.sparrow.feature.attachments.presentation.storage

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun AttachmentStorageRoute(
    viewModel: AttachmentStorageViewModel = koinViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    AttachmentStorageScreen(
        uiState = uiState,
        onUiEvent = viewModel::onUiEvent
    )
}
