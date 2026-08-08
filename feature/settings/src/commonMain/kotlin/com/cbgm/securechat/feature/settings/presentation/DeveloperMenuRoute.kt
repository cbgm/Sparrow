package com.cbgm.securechat.feature.settings.presentation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cbgm.securechat.feature.settings.presentation.screen.DeveloperMenuScreen
import com.cbgm.securechat.feature.settings.presentation.screen.DeveloperMenuViewModel
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun DeveloperMenuRoute(
    modifier: Modifier = Modifier,
    onBack: () -> Unit,
    viewModel: DeveloperMenuViewModel = koinViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    DeveloperMenuScreen(
        uiState = uiState,
        onBack = onBack,
        onClearLocalData = viewModel::onClearLocalData,
        onDisableDeveloperMode = {
            viewModel.onDisableDeveloperMode()
            onBack()
        },
        modifier = modifier
    )
}
