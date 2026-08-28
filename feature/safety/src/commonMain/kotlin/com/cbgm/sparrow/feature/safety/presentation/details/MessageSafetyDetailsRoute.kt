package com.cbgm.sparrow.feature.safety.presentation.details

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun MessageSafetyDetailsRoute(
    viewModel: MessageSafetyDetailsViewModel = koinViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    MessageSafetyDetailsScreen(
        uiState = uiState,
        onUiEvent = viewModel::onUiEvent
    )
}
