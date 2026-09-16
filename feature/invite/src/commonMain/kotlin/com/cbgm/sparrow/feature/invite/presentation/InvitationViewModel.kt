package com.cbgm.sparrow.feature.invite.presentation

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.cbgm.sparrow.core.ui.navigation.AppRoute
import com.cbgm.sparrow.core.ui.navigation.requireRouteArgument
import com.cbgm.sparrow.core.ui.presentation.BaseViewModel
import com.cbgm.sparrow.feature.invite.domain.usecase.AcceptInvitationUseCase
import com.cbgm.sparrow.feature.invite.domain.usecase.DeclineAndBlockInvitationUseCase
import com.cbgm.sparrow.feature.invite.domain.usecase.DeclineInvitationUseCase
import com.cbgm.sparrow.feature.invite.domain.usecase.DeleteDeclinedOutgoingInvitationUseCase
import com.cbgm.sparrow.feature.invite.domain.usecase.MarkInvitationsViewedUseCase
import com.cbgm.sparrow.feature.invite.domain.usecase.ObserveInvitationsContextUseCase
import com.cbgm.sparrow.feature.invite.presentation.mapper.toInvitationDirection
import com.cbgm.sparrow.feature.invite.presentation.mapper.toInvitationUiState
import com.cbgm.sparrow.feature.invite.presentation.mapper.toInvitationsUiData
import com.cbgm.sparrow.feature.invite.presentation.model.InvitationEffect
import com.cbgm.sparrow.feature.invite.presentation.model.InvitationTab
import com.cbgm.sparrow.feature.invite.presentation.model.InvitationUi
import com.cbgm.sparrow.feature.invite.presentation.model.InvitationUiEvent
import com.cbgm.sparrow.feature.invite.presentation.model.InvitationUiState
import com.cbgm.sparrow.feature.invite.presentation.model.InvitationsUiData
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

class InvitationViewModel(
    savedStateHandle: SavedStateHandle,
    observeInvitationsContext: ObserveInvitationsContextUseCase,
    private val acceptInvitation: AcceptInvitationUseCase,
    private val declineInvitation: DeclineInvitationUseCase,
    private val declineAndBlockInvitation: DeclineAndBlockInvitationUseCase,
    private val deleteDeclinedOutgoingInvitation: DeleteDeclinedOutgoingInvitationUseCase,
    private val markInvitationsViewed: MarkInvitationsViewedUseCase
) : BaseViewModel() {
    private val initialTab =
        if (savedStateHandle.requireRouteArgument<Boolean>(AppRoute.Invitations::showOutgoing.name)) {
            InvitationTab.OUTGOING
        } else {
            InvitationTab.INCOMING
        }

    private val selectedTab = MutableStateFlow(initialTab)

    private val invitations =
        observeInvitationsContext()
            .map { context -> context.toInvitationsUiData() }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(stopTimeoutMillis = 5_000),
                initialValue = InvitationsUiData()
            )

    private val processingInvitationId = MutableStateFlow<String?>(null)

    val uiState: StateFlow<InvitationUiState> =
        combine(
            selectedTab,
            invitations,
            processingInvitationId
        ) { tab, invitationData, processingId ->
            toInvitationUiState(
                selectedTab = tab,
                invitations = invitationData,
                processingInvitationId = processingId
            )
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(stopTimeoutMillis = 5_000),
            initialValue = InvitationUiState(selectedTab = initialTab)
        )

    private val _effects = Channel<InvitationEffect>(capacity = Channel.BUFFERED)
    val effects = _effects.receiveAsFlow()

    init {
        observeViewedTab()
        observeIncomingInvitations()
    }

    fun onUiEvent(event: InvitationUiEvent) {
        when (event) {
            InvitationUiEvent.CloseClicked -> navigator.popBackStack()
            is InvitationUiEvent.TabSelected -> selectedTab.value = event.tab
            is InvitationUiEvent.AcceptClicked -> accept(event.invitationId)
            is InvitationUiEvent.DeclineClicked -> decline(event.invitationId)
            is InvitationUiEvent.DeclineAndBlockClicked -> declineAndBlock(event.invitationId)
            is InvitationUiEvent.DeleteDeclinedOutgoingClicked -> deleteDeclinedOutgoing(event.invitationId)
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
                        InvitationTab.INCOMING -> invitationData.incoming
                        InvitationTab.OUTGOING -> invitationData.outgoing
                    }
                tab to selected.any(InvitationUi::hasUnreadUpdate)
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
                tab == InvitationTab.INCOMING && invitationData.incoming.isEmpty()
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
                    InvitationEffect.ShowError(
                        message = error.message ?: "Invitation could not be updated"
                    )
                )
            }

            processingInvitationId.value = null
        }
    }
}
