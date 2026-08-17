package com.cbgm.sparrow.feature.settings.presentation.profile

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun ProfileSettingsRoute(
    modifier: Modifier = Modifier,
    viewModel: ProfileSettingsViewModel = koinViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    ProfileSettingsScreen(
        uiState = uiState,
        onUiEvent = viewModel::onUiEvent,
        modifier = modifier
    )
}
