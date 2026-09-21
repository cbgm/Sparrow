package com.cbgm.sparrow.feature.settings.presentation.overview

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cbgm.sparrow.feature.settings.presentation.overview.model.SettingsEffect
import com.cbgm.sparrow.feature.settings.presentation.overview.model.SettingsUiEvent
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun SettingsRoute(
    modifier: Modifier = Modifier,
    scrollState: ScrollState,
    innerPadding: PaddingValues,
    viewModel: SettingsViewModel = koinViewModel()
) {
    // Keep the collected State unread here; each row observes only its derived field.
    val uiState = viewModel.uiState.collectAsStateWithLifecycle()
    // Only the two model-related rows consume this rapidly changing State.
    val modelDownloadPercent = viewModel.modelDownloadPercent.collectAsStateWithLifecycle()
    val onUiEvent = remember(viewModel) {
        { event: SettingsUiEvent ->
            viewModel.onUiEvent(event)
        }
    }
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
        modelDownloadPercent = modelDownloadPercent,
        snackbarHostState = snackbarHostState,
        onUiEvent = onUiEvent,
        scrollState = scrollState,
        innerPadding = innerPadding,
        modifier = modifier
    )
}
