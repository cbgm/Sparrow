package com.cbgm.sparrow.feature.invite.presentation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun InvitationRoute(
    viewModel: InvitationViewModel = koinViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    InvitationsScreen(
        uiState = uiState,
        onUiEvent = viewModel::onUiEvent
    )
}
