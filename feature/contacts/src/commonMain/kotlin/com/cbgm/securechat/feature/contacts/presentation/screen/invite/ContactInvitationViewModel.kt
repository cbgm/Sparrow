package com.cbgm.securechat.feature.contacts.presentation.screen.invite

import androidx.lifecycle.viewModelScope
import com.cbgm.securechat.core.ui.presentation.BaseViewModel
import com.cbgm.securechat.feature.contacts.domain.model.PendingContactInvitation
import com.cbgm.securechat.feature.contacts.domain.usecase.AcceptContactInvitation
import com.cbgm.securechat.feature.contacts.domain.usecase.DeclineAndBlockContactInvitation
import com.cbgm.securechat.feature.contacts.domain.usecase.DeclineContactInvitation
import com.cbgm.securechat.feature.contacts.domain.usecase.ObservePendingContactInvitations
import com.cbgm.securechat.feature.contacts.presentation.model.ContactInvitationEffect
import com.cbgm.securechat.feature.contacts.presentation.model.ContactInvitationUiEvent
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ContactInvitationViewModel(
    observePendingContactInvitations: ObservePendingContactInvitations,
    private val acceptContactInvitation: AcceptContactInvitation,
    private val declineContactInvitation: DeclineContactInvitation,
    private val declineAndBlockContactInvitation: DeclineAndBlockContactInvitation
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

            operation()
                .onFailure { error ->
                    _effects.send(
                        ContactInvitationEffect.ShowError(
                            message = error.message ?: "Contact invitation could not be updated"
                        )
                    )
                }

            _processingInvitationId.value = null
        }
    }
}
