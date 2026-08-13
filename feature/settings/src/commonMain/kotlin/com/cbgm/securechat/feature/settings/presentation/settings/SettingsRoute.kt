package com.cbgm.securechat.feature.settings.presentation.settings

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cbgm.securechat.feature.settings.presentation.settings.model.SettingsEffect
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun SettingsRoute(
    modifier: Modifier = Modifier,
    scrollState: ScrollState,
    innerPadding: PaddingValues,
    viewModel: SettingsViewModel = koinViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(viewModel) {
        viewModel.effects.collect { event ->
            when (event) {
                is SettingsEffect.ShowSnackbar -> snackbarHostState.showSnackbar(event.message)
            }
        }
    }

    SettingsScreen(
        uiState = uiState,
        snackbarHostState = snackbarHostState,
        onUiEvent = viewModel::onUiEvent,
        scrollState = scrollState,
        innerPadding = innerPadding,
        modifier = modifier
    )
}
