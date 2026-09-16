package com.cbgm.sparrow.feature.contacts.presentation.invitations

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.cbgm.sparrow.core.ui.navigation.AppRoute
import com.cbgm.sparrow.core.ui.navigation.requireRouteArgument
import com.cbgm.sparrow.core.ui.presentation.BaseViewModel
import com.cbgm.sparrow.feature.contacts.presentation.invitations.mapper.toContactInvitationUiState
import com.cbgm.sparrow.feature.contacts.presentation.invitations.mapper.toContactInvitationsUiData
import com.cbgm.sparrow.feature.contacts.presentation.invitations.mapper.toInvitationDirection
import com.cbgm.sparrow.feature.contacts.presentation.invitations.model.ContactInvitationEffect
import com.cbgm.sparrow.feature.contacts.presentation.invitations.model.ContactInvitationTab
import com.cbgm.sparrow.feature.contacts.presentation.invitations.model.ContactInvitationUi
import com.cbgm.sparrow.feature.contacts.presentation.invitations.model.ContactInvitationUiEvent
import com.cbgm.sparrow.feature.contacts.presentation.invitations.model.ContactInvitationUiState
import com.cbgm.sparrow.feature.contacts.presentation.invitations.model.ContactInvitationsUiData
import com.cbgm.sparrow.feature.invite.domain.usecase.AcceptInvitationUseCase
import com.cbgm.sparrow.feature.invite.domain.usecase.DeclineAndBlockInvitationUseCase
import com.cbgm.sparrow.feature.invite.domain.usecase.DeclineInvitationUseCase
import com.cbgm.sparrow.feature.invite.domain.usecase.DeleteDeclinedOutgoingInvitationUseCase
import com.cbgm.sparrow.feature.invite.domain.usecase.MarkInvitationsViewedUseCase
import com.cbgm.sparrow.feature.invite.domain.usecase.ObserveInvitationsContextUseCase
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ContactInvitationViewModel(
    savedStateHandle: SavedStateHandle,
    observeInvitationsContext: ObserveInvitationsContextUseCase,
    private val acceptInvitation: AcceptInvitationUseCase,
    private val declineInvitation: DeclineInvitationUseCase,
    private val declineAndBlockInvitation: DeclineAndBlockInvitationUseCase,
    private val deleteDeclinedOutgoingInvitation: DeleteDeclinedOutgoingInvitationUseCase,
    private val markInvitationsViewed: MarkInvitationsViewedUseCase
) : BaseViewModel() {
    private val initialTab =
        if (savedStateHandle.requireRouteArgument<Boolean>(AppRoute.ContactInvitations::showOutgoing.name)) {
            ContactInvitationTab.OUTGOING
        } else {
            ContactInvitationTab.INCOMING
        }

    private val selectedTab = MutableStateFlow(initialTab)

    private val invitations =
        observeInvitationsContext()
            .map { context -> context.toContactInvitationsUiData() }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(stopTimeoutMillis = 5_000),
                initialValue = ContactInvitationsUiData()
            )

    private val processingInvitationId = MutableStateFlow<String?>(null)

    val uiState: StateFlow<ContactInvitationUiState> =
        combine(
            selectedTab,
            invitations,
            processingInvitationId
        ) { tab, invitationData, processingId ->
            toContactInvitationUiState(
                selectedTab = tab,
                invitations = invitationData,
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
        observeIncomingInvitations()
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
                invitations
            ) { tab, invitationData ->
                val selected =
                    when (tab) {
                        ContactInvitationTab.INCOMING -> invitationData.incoming
                        ContactInvitationTab.OUTGOING -> invitationData.outgoing
                    }
                tab to selected.any(ContactInvitationUi::hasUnreadUpdate)
            }.distinctUntilChanged()
                .collect { (tab, hasUnreadUpdate) ->
                    if (hasUnreadUpdate) {
                        markInvitationsViewed(tab.toInvitationDirection())
                    }
                }
        }
    }

    private fun observeIncomingInvitations() {
        viewModelScope.launch {
            combine(
                selectedTab,
                invitations
            ) { tab, invitationData ->
                tab == ContactInvitationTab.INCOMING && invitationData.incoming.isEmpty()
            }.drop(1)
                .distinctUntilChanged()
                .filter { isEmpty -> isEmpty }
                .collect {
                    navigator.popBackStack()
                }
        }
    }

    private fun accept(invitationId: String) {
        updateInvitation(invitationId = invitationId) {
            acceptInvitation(invitationId)
        }
    }

    private fun decline(invitationId: String) {
        updateInvitation(invitationId = invitationId) {
            declineInvitation(invitationId)
        }
    }

    private fun declineAndBlock(invitationId: String) {
        updateInvitation(invitationId = invitationId) {
            declineAndBlockInvitation(invitationId)
        }
    }

    private fun deleteDeclinedOutgoing(invitationId: String) {
        updateInvitation(invitationId = invitationId) {
            deleteDeclinedOutgoingInvitation(invitationId)
        }
    }

    private fun updateInvitation(
        invitationId: String,
        operation: suspend () -> Result<Unit>
    ) {
        if (processingInvitationId.value != null) return

        viewModelScope.launch {
            processingInvitationId.value = invitationId
            val result = operation()
            result.onFailure { error ->
                _effects.send(
                    ContactInvitationEffect.ShowError(
                        message = error.message ?: "Invitation could not be updated"
                    )
                )
            }

            processingInvitationId.value = null
        }
    }
}
