package com.cbgm.securechat.feature.settings.presentation.licenses

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun LicensesRoute(
    modifier: Modifier = Modifier,
    licensesViewModel: LicensesViewModel = koinViewModel()
) {
    val uiState by licensesViewModel.uiState.collectAsState()

    LicensesScreen(
        uiState = uiState,
        onUiEvent = licensesViewModel::onUiEvent,
        modifier = modifier
    )
}
