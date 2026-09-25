package com.cbgm.sparrow.feature.settings.presentation.overview

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
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

    SettingsScreen(
        uiState = uiState,
        modelDownloadPercent = modelDownloadPercent,
        onUiEvent = onUiEvent,
        scrollState = scrollState,
        innerPadding = innerPadding,
        modifier = modifier
    )
}
