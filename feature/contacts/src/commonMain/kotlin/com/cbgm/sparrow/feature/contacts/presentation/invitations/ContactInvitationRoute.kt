package com.cbgm.sparrow.feature.contacts.presentation.invitations

import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cbgm.sparrow.feature.contacts.presentation.invitations.model.ContactInvitationEffect
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun ContactInvitationRoute(
    viewModel: ContactInvitationViewModel = koinViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(viewModel) {
        viewModel.effects.collect { effect ->
            when (effect) {
                is ContactInvitationEffect.ShowError -> snackbarHostState.showSnackbar(effect.message)
            }
        }
    }

    ContactInvitationsScreen(
        uiState = uiState,
        snackbarHostState = snackbarHostState,
        onUiEvent = viewModel::onUiEvent
    )
}
