package com.cbgm.securechat.feature.contacts.presentation.screen

import androidx.lifecycle.viewModelScope
import com.cbgm.securechat.core.security.DirectIdentitySetupMode
import com.cbgm.securechat.core.security.DirectIdentitySetupModeRepository
import com.cbgm.securechat.core.ui.presentation.BaseViewModel
import com.cbgm.securechat.feature.contacts.domain.identity.IdentityInvitationService
import com.cbgm.securechat.feature.contacts.domain.model.PendingContactInvitation
import com.cbgm.securechat.feature.contacts.presentation.model.ContactInvitationEffect
import com.cbgm.securechat.feature.contacts.presentation.model.ContactInvitationUiEvent
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ContactInvitationViewModel(
    private val identityInvitationService: IdentityInvitationService,
    modeRepository: DirectIdentitySetupModeRepository
) : BaseViewModel() {
    val pendingInvitations: StateFlow<List<PendingContactInvitation>> =
        combine(
            identityInvitationService.observePendingIncoming(),
            modeRepository.observeMode()
        ) { invitations, mode ->
            if (mode == DirectIdentitySetupMode.AUTOMATIC_INVITATION) {
                invitations
            } else {
                emptyList()
            }
        }.stateIn(
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
            is ContactInvitationUiEvent.AcceptClicked -> accept(event.invitationId)
            is ContactInvitationUiEvent.DeclineClicked -> decline(event.invitationId)
            is ContactInvitationUiEvent.DeclineAndBlockClicked -> declineAndBlock(event.invitationId)
        }
    }

    private fun accept(invitationId: String) {
        updateInvitation(invitationId) {
            identityInvitationService.accept(invitationId)
        }
    }

    private fun decline(invitationId: String) {
        updateInvitation(invitationId) {
            identityInvitationService.decline(invitationId)
        }
    }

    private fun declineAndBlock(invitationId: String) {
        updateInvitation(invitationId) {
            identityInvitationService.declineAndBlock(invitationId)
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
