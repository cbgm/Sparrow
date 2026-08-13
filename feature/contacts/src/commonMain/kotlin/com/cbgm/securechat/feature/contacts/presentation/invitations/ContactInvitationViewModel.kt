package com.cbgm.securechat.feature.contacts.presentation.invitations

import androidx.lifecycle.viewModelScope
import com.cbgm.securechat.core.ui.presentation.BaseViewModel
import com.cbgm.securechat.feature.contacts.domain.model.PendingContactInvitation
import com.cbgm.securechat.feature.contacts.domain.usecase.AcceptContactInvitationUseCase
import com.cbgm.securechat.feature.contacts.domain.usecase.DeclineAndBlockContactInvitationUseCase
import com.cbgm.securechat.feature.contacts.domain.usecase.DeclineContactInvitationUseCase
import com.cbgm.securechat.feature.contacts.domain.usecase.ObservePendingContactInvitationsUseCase
import com.cbgm.securechat.feature.contacts.presentation.invitations.model.ContactInvitationEffect
import com.cbgm.securechat.feature.contacts.presentation.invitations.model.ContactInvitationUiEvent
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ContactInvitationViewModel(
    observePendingContactInvitations: ObservePendingContactInvitationsUseCase,
    private val acceptContactInvitation: AcceptContactInvitationUseCase,
    private val declineContactInvitation: DeclineContactInvitationUseCase,
    private val declineAndBlockContactInvitation: DeclineAndBlockContactInvitationUseCase
) : BaseViewModel() {
    val pendingInvitations: StateFlow<List<PendingContactInvitation>> =
        observePendingContactInvitations()
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(stopTimeoutMillis = 5_000),
                initialValue = emptyList()
            )

    private val _processingInvitationId = MutableStateFlow<String?>(null)
    val processingInvitationId: StateFlow<String?> = _processingInvitationId.asStateFlow()

    private val _effects = Channel<ContactInvitationEffect>(capacity = Channel.BUFFERED)
    val effects = _effects.receiveAsFlow()

    fun onUiEvent(event: ContactInvitationUiEvent) {
        when (event) {
            ContactInvitationUiEvent.CloseClicked -> navigator.popBackStack()
            is ContactInvitationUiEvent.AcceptClicked -> accept(event.invitationId)
            is ContactInvitationUiEvent.DeclineClicked -> decline(event.invitationId)
            is ContactInvitationUiEvent.DeclineAndBlockClicked -> declineAndBlock(event.invitationId)
        }
    }

    private fun accept(invitationId: String) {
        updateInvitation(invitationId) {
            acceptContactInvitation(invitationId)
        }
    }

    private fun decline(invitationId: String) {
        updateInvitation(invitationId) {
            declineContactInvitation(invitationId)
        }
    }

    private fun declineAndBlock(invitationId: String) {
        updateInvitation(invitationId) {
            declineAndBlockContactInvitation(invitationId)
        }
    }

    private fun updateInvitation(
        invitationId: String,
        operation: suspend () -> Result<Unit>
    ) {
        if (_processingInvitationId.value != null) return

        viewModelScope.launch {
            _processingInvitationId.value = invitationId

            val result = operation()

            result.onFailure { error ->
                _effects.send(
                    ContactInvitationEffect.ShowError(
                        message = error.message ?: "Contact invitation could not be updated"
                    )
                )
            }

            val shouldClose =
                result.isSuccess && wasLastInvitationHandled(invitationId)

            _processingInvitationId.value = null

            if (shouldClose) {
                navigator.popBackStack()
            }
        }
    }

    private suspend fun wasLastInvitationHandled(invitationId: String): Boolean {
        val remainingInvitations =
            pendingInvitations.first { invitations ->
                invitations.none { invitation -> invitation.invitationId == invitationId }
            }

        return remainingInvitations.isEmpty()
    }
}
