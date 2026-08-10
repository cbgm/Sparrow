package com.cbgm.securechat.feature.contacts.presentation

import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cbgm.securechat.feature.contacts.presentation.component.ContactInvitationDialog
import com.cbgm.securechat.feature.contacts.presentation.model.ContactInvitationEffect
import com.cbgm.securechat.feature.contacts.presentation.model.ContactInvitationUiEvent
import com.cbgm.securechat.feature.contacts.presentation.screen.ContactInvitationViewModel
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun ContactInvitationRoute(
    viewModel: ContactInvitationViewModel = koinViewModel()
) {
    val pendingInvitations by viewModel.pendingInvitations.collectAsStateWithLifecycle()
    val processingInvitationId by viewModel.processingInvitationId.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(viewModel) {
        viewModel.effects.collect { event ->
            when (event) {
                is ContactInvitationEffect.ShowError -> {
                    snackbarHostState.showSnackbar(event.message)
                }
                else -> Unit
            }
        }
    }

    pendingInvitations.firstOrNull()?.let { invitation ->
        ContactInvitationDialog(
            invitation = invitation,
            isProcessing = processingInvitationId == invitation.invitationId,
            onAccept = {
                viewModel.onUiEvent(ContactInvitationUiEvent.AcceptClicked(invitation.invitationId))
            },
            onDecline = {
                viewModel.onUiEvent(ContactInvitationUiEvent.DeclineClicked(invitation.invitationId))
            },
            onDeclineAndBlock = {
                viewModel.onUiEvent(ContactInvitationUiEvent.DeclineAndBlockClicked(invitation.invitationId))
            }
        )
    }

    SnackbarHost(hostState = snackbarHostState)
}
