package com.cbgm.sparrow.feature.contacts.presentation.invitations

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.cbgm.sparrow.core.ui.navigation.AppRoute
import com.cbgm.sparrow.core.ui.navigation.requireRouteArgument
import com.cbgm.sparrow.core.ui.presentation.BaseViewModel
import com.cbgm.sparrow.feature.contacts.domain.model.ContactInvitation
import com.cbgm.sparrow.feature.contacts.domain.model.ContactInvitationsContext
import com.cbgm.sparrow.feature.contacts.domain.usecase.AcceptContactInvitationUseCase
import com.cbgm.sparrow.feature.contacts.domain.usecase.DeclineAndBlockContactInvitationUseCase
import com.cbgm.sparrow.feature.contacts.domain.usecase.DeclineContactInvitationUseCase
import com.cbgm.sparrow.feature.contacts.domain.usecase.DeleteDeclinedOutgoingInvitationUseCase
import com.cbgm.sparrow.feature.contacts.domain.usecase.MarkContactInvitationsViewedUseCase
import com.cbgm.sparrow.feature.contacts.domain.usecase.ObserveContactInvitationsContextUseCase
import com.cbgm.sparrow.feature.contacts.presentation.invitations.mapper.toUiState
import com.cbgm.sparrow.feature.contacts.presentation.invitations.model.ContactInvitationEffect
import com.cbgm.sparrow.feature.contacts.presentation.invitations.model.ContactInvitationTab
import com.cbgm.sparrow.feature.contacts.presentation.invitations.model.ContactInvitationUiEvent
import com.cbgm.sparrow.feature.contacts.presentation.invitations.model.ContactInvitationUiState
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ContactInvitationViewModel(
    savedStateHandle: SavedStateHandle,
    observeInvitationsContext: ObserveContactInvitationsContextUseCase,
    private val acceptContactInvitation: AcceptContactInvitationUseCase,
    private val declineContactInvitation: DeclineContactInvitationUseCase,
    private val declineAndBlockContactInvitation: DeclineAndBlockContactInvitationUseCase,
    private val deleteDeclinedOutgoingInvitation: DeleteDeclinedOutgoingInvitationUseCase,
    private val markInvitationsViewed: MarkContactInvitationsViewedUseCase
) : BaseViewModel() {
    private val initialTab =
        if (savedStateHandle.requireRouteArgument<Boolean>(AppRoute.ContactInvitations::showOutgoing.name)) {
            ContactInvitationTab.OUTGOING
        } else {
            ContactInvitationTab.INCOMING
        }

    private val selectedTab = MutableStateFlow(initialTab)

    private val invitationsContext =
        observeInvitationsContext()
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(stopTimeoutMillis = 5_000),
                initialValue = ContactInvitationsContext()
            )

    private val processingInvitationId = MutableStateFlow<String?>(null)

    val uiState: StateFlow<ContactInvitationUiState> =
        combine(
            selectedTab,
            invitationsContext,
            processingInvitationId
        ) { tab, context, processingId ->
            toUiState(
                selectedTab = tab,
                incomingInvitations = context.incoming,
                outgoingInvitations = context.outgoing,
                profilePictures = context.profilePictures,
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
                invitationsContext
            ) { tab, context ->
                val selected =
                    when (tab) {
                        ContactInvitationTab.INCOMING -> context.incoming
                        ContactInvitationTab.OUTGOING -> context.outgoing
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
        val context =
            invitationsContext.first { context ->
                context.incoming.none { invitation -> invitation.invitationId == invitationId } &&
                    context.outgoing.none { invitation -> invitation.invitationId == invitationId }
            }

        return context.incoming.isEmpty() && context.outgoing.isEmpty()
    }
}
