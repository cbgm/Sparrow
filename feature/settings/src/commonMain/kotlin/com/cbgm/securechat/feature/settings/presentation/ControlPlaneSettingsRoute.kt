package com.cbgm.securechat.feature.settings.presentation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cbgm.securechat.feature.settings.presentation.screen.ControlPlaneSettingsScreen
import com.cbgm.securechat.feature.settings.presentation.screen.ControlPlaneSettingsViewModel
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun ControlPlaneSettingsRoute(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ControlPlaneSettingsViewModel = koinViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    ControlPlaneSettingsScreen(
        uiState = uiState,
        onEvent = viewModel::onEvent,
        onBack = onBack,
        modifier = modifier
    )
}
