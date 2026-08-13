package com.cbgm.securechat.feature.settings.presentation.developer

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun DeveloperMenuRoute(
    modifier: Modifier = Modifier,
    viewModel: DeveloperMenuViewModel = koinViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    DeveloperMenuScreen(
        uiState = uiState,
        onUiEvent = viewModel::onUiEvent,
        modifier = modifier
    )
}
