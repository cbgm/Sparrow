package com.cbgm.sparrow.feature.contacts.presentation.invitations

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.cbgm.sparrow.core.ui.navigation.AppRoute
import com.cbgm.sparrow.core.ui.navigation.requireRouteArgument
import com.cbgm.sparrow.core.ui.presentation.BaseViewModel
import com.cbgm.sparrow.feature.contacts.domain.model.ContactInvitation
import com.cbgm.sparrow.feature.contacts.domain.model.IdentityInvitationDirection
import com.cbgm.sparrow.feature.contacts.domain.usecase.AcceptContactInvitationUseCase
import com.cbgm.sparrow.feature.contacts.domain.usecase.DeclineAndBlockContactInvitationUseCase
import com.cbgm.sparrow.feature.contacts.domain.usecase.DeclineContactInvitationUseCase
import com.cbgm.sparrow.feature.contacts.domain.usecase.DeleteDeclinedOutgoingInvitationUseCase
import com.cbgm.sparrow.feature.contacts.domain.usecase.MarkContactInvitationsViewedUseCase
import com.cbgm.sparrow.feature.contacts.domain.usecase.ObserveContactInvitationsUseCase
import com.cbgm.sparrow.feature.contacts.domain.usecase.ObserveContactProfilePicturesUseCase
import com.cbgm.sparrow.feature.contacts.presentation.invitations.mapper.toUiState
import com.cbgm.sparrow.feature.contacts.presentation.invitations.model.ContactInvitationEffect
import com.cbgm.sparrow.feature.contacts.presentation.invitations.model.ContactInvitationTab
import com.cbgm.sparrow.feature.contacts.presentation.invitations.model.ContactInvitationUiEvent
import com.cbgm.sparrow.feature.contacts.presentation.invitations.model.ContactInvitationUiState
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
class ContactInvitationViewModel(
    savedStateHandle: SavedStateHandle,
    observeContactInvitations: ObserveContactInvitationsUseCase,
    private val acceptContactInvitation: AcceptContactInvitationUseCase,
    private val declineContactInvitation: DeclineContactInvitationUseCase,
    private val declineAndBlockContactInvitation: DeclineAndBlockContactInvitationUseCase,
    private val deleteDeclinedOutgoingInvitation: DeleteDeclinedOutgoingInvitationUseCase,
    private val markInvitationsViewed: MarkContactInvitationsViewedUseCase,
    observeProfilePictures: ObserveContactProfilePicturesUseCase
) : BaseViewModel() {
    private val initialTab =
        if (savedStateHandle.requireRouteArgument<Boolean>(AppRoute.ContactInvitations::showOutgoing.name)) {
            ContactInvitationTab.OUTGOING
        } else {
            ContactInvitationTab.INCOMING
        }

    private val selectedTab = MutableStateFlow(initialTab)

    private val incomingInvitations =
        observeContactInvitations(IdentityInvitationDirection.INCOMING)
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(stopTimeoutMillis = 5_000),
                initialValue = emptyList()
            )

    private val outgoingInvitations =
        observeContactInvitations(IdentityInvitationDirection.OUTGOING)
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(stopTimeoutMillis = 5_000),
                initialValue = emptyList()
            )

    private val allInvitations =
        combine(incomingInvitations, outgoingInvitations) { incoming, outgoing ->
            incoming + outgoing
        }

    private val profilePictures =
        allInvitations.flatMapLatest { invitations ->
            observeProfilePictures(invitations.map(ContactInvitation::contactId).toSet())
        }

    private val processingInvitationId = MutableStateFlow<String?>(null)

    val uiState: StateFlow<ContactInvitationUiState> =
        combine(
            selectedTab,
            incomingInvitations,
            outgoingInvitations,
            profilePictures,
            processingInvitationId
        ) { tab, incoming, outgoing, pictures, processingId ->
            toUiState(
                selectedTab = tab,
                incomingInvitations = incoming,
                outgoingInvitations = outgoing,
                profilePictures = pictures,
                processingInvitationId = processingId
            )
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(stopTimeoutMillis = 5_000),
            initialValue = ContactInvitationUiState(selectedTab = initialTab)
        )

    private val _effects = Channel<ContactInvitationEffect>(capacity = Channel.BUFFERED)
    val effects = _effects.receiveAsFlow()

    init {
        observeViewedTab()
    }

    fun onUiEvent(event: ContactInvitationUiEvent) {
        when (event) {
            ContactInvitationUiEvent.CloseClicked -> navigator.popBackStack()
            is ContactInvitationUiEvent.TabSelected -> selectedTab.value = event.tab
            is ContactInvitationUiEvent.AcceptClicked -> accept(event.invitationId)
            is ContactInvitationUiEvent.DeclineClicked -> decline(event.invitationId)
            is ContactInvitationUiEvent.DeclineAndBlockClicked -> declineAndBlock(event.invitationId)
            is ContactInvitationUiEvent.DeleteDeclinedOutgoingClicked -> deleteDeclinedOutgoing(event.invitationId)
        }
    }

    private fun observeViewedTab() {
        viewModelScope.launch {
            combine(
                selectedTab,
                incomingInvitations,
                outgoingInvitations
            ) { tab, incoming, outgoing ->
                val selected =
                    when (tab) {
                        ContactInvitationTab.INCOMING -> incoming
                        ContactInvitationTab.OUTGOING -> outgoing
                    }
                tab to selected.any(ContactInvitation::hasUnreadUpdate)
            }.distinctUntilChanged()
                .collect { (tab, hasUnreadUpdate) ->
                    if (hasUnreadUpdate) {
                        markInvitationsViewed(tab.direction)
                    }
                }
        }
    }

    private fun accept(invitationId: String) {
        updateInvitation(
            invitationId = invitationId,
            closeWhenScreenBecomesEmpty = true
        ) {
            acceptContactInvitation(invitationId)
        }
    }

    private fun decline(invitationId: String) {
        updateInvitation(
            invitationId = invitationId,
            closeWhenScreenBecomesEmpty = true
        ) {
            declineContactInvitation(invitationId)
        }
    }

    private fun declineAndBlock(invitationId: String) {
        updateInvitation(
            invitationId = invitationId,
            closeWhenScreenBecomesEmpty = true
        ) {
            declineAndBlockContactInvitation(invitationId)
        }
    }

    private fun deleteDeclinedOutgoing(invitationId: String) {
        updateInvitation(
            invitationId = invitationId,
            closeWhenScreenBecomesEmpty = false
        ) {
            deleteDeclinedOutgoingInvitation(invitationId)
        }
    }

    private fun updateInvitation(
        invitationId: String,
        closeWhenScreenBecomesEmpty: Boolean,
        operation: suspend () -> Result<Unit>
    ) {
        if (processingInvitationId.value != null) return

        viewModelScope.launch {
            processingInvitationId.value = invitationId
            val result = operation()
            result.onFailure { error ->
                _effects.send(
                    ContactInvitationEffect.ShowError(
                        message = error.message ?: "Contact invitation could not be updated"
                    )
                )
            }

            val shouldClose =
                result.isSuccess &&
                    closeWhenScreenBecomesEmpty &&
                    isScreenEmptyAfter(invitationId)

            processingInvitationId.value = null

            if (shouldClose) {
                navigator.popBackStack()
            }
        }
    }

    private suspend fun isScreenEmptyAfter(invitationId: String): Boolean {
        val (incoming, outgoing) =
            combine(incomingInvitations, outgoingInvitations) { incoming, outgoing ->
                incoming to outgoing
            }.first { (incoming, outgoing) ->
                incoming.none { invitation -> invitation.invitationId == invitationId } &&
                    outgoing.none { invitation -> invitation.invitationId == invitationId }
            }

        return incoming.isEmpty() && outgoing.isEmpty()
    }
}
