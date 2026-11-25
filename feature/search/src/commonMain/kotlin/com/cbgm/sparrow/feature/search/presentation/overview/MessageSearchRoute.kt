package com.cbgm.sparrow.feature.search.presentation.overview

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

@Composable
fun MessageSearchRoute(
    savedStateHandle: SavedStateHandle? = null,
    viewModel: MessageSearchViewModel = koinViewModel(
        parameters = { parametersOf(savedStateHandle) }
    )
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    MessageSearchScreen(
        uiState = uiState,
        onUiEvent = viewModel::onUiEvent
    )
}
