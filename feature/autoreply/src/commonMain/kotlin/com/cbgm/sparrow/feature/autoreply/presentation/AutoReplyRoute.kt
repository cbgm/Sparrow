package com.cbgm.sparrow.feature.autoreply.presentation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun AutoReplySettingsRoute(
    viewModel: AutoReplyViewModel = koinViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    AutoReplySettingsScreen(
        uiState = uiState,
        onUiEvent = viewModel::onUiEvent
    )
}
