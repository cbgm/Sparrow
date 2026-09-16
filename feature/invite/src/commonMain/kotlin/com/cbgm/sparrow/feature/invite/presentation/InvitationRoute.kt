package com.cbgm.sparrow.feature.invite.presentation

import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cbgm.sparrow.feature.invite.presentation.model.InvitationEffect
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun InvitationRoute(
    viewModel: InvitationViewModel = koinViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(viewModel) {
        viewModel.effects.collect { effect ->
            when (effect) {
                is InvitationEffect.ShowError -> snackbarHostState.showSnackbar(effect.message)
            }
        }
    }

    InvitationsScreen(
        uiState = uiState,
        snackbarHostState = snackbarHostState,
        onUiEvent = viewModel::onUiEvent
    )
}
