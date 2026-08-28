package com.cbgm.sparrow.feature.search.presentation.overview

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun MessageSearchRoute(
    viewModel: MessageSearchViewModel = koinViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    MessageSearchScreen(
        uiState = uiState,
        onUiEvent = viewModel::onUiEvent
    )
}
