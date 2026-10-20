package com.cbgm.sparrow.feature.identity.presentation.details

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun IdentityDetailsRoute(
    modifier: Modifier = Modifier,
    viewModel: IdentityDetailsViewModel = koinViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    IdentityDetailsScreen(
        uiState = uiState,
        onUiEvent = { event ->
            viewModel.onUiEvent(event)
        },
        modifier = modifier
    )
}
