package com.cbgm.securechat.feature.contacts.presentation

import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cbgm.securechat.feature.contacts.presentation.model.ContactInvitationEffect
import com.cbgm.securechat.feature.contacts.presentation.model.ContactInvitationUiEvent
import com.cbgm.securechat.feature.contacts.presentation.screen.ContactInvitationViewModel
import com.cbgm.securechat.feature.contacts.presentation.screen.ContactInvitationsScreen
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun ContactInvitationRoute(
    viewModel: ContactInvitationViewModel = koinViewModel()
) {
    val invitations by viewModel.pendingInvitations.collectAsStateWithLifecycle()
    val processingInvitationId by viewModel.processingInvitationId.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(viewModel) {
        viewModel.effects.collect { effect ->
            when (effect) {
                is ContactInvitationEffect.ShowError -> snackbarHostState.showSnackbar(effect.message)
            }
        }
    }

    ContactInvitationsScreen(
        invitations = invitations,
        processingInvitationId = processingInvitationId,
        snackbarHostState = snackbarHostState,
        onClose = { viewModel.onUiEvent(ContactInvitationUiEvent.CloseClicked) },
        onUiEvent = viewModel::onUiEvent
    )
}
