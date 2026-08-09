package com.cbgm.securechat.feature.settings.presentation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.cbgm.securechat.feature.settings.presentation.screen.LicensesScreen
import com.cbgm.securechat.feature.settings.presentation.screen.licenses.LicensesViewModel
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun LicensesRoute(
    modifier: Modifier = Modifier,
    onBack: () -> Unit,
    licensesViewModel: LicensesViewModel = koinViewModel()
) {
    val uiState by licensesViewModel.uiState.collectAsState()

    LicensesScreen(
        uiState = uiState,
        onBack = onBack,
        modifier = modifier
    )
}
