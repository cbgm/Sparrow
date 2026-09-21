package com.cbgm.sparrow.navigation.presentation.main

import androidx.lifecycle.viewModelScope
import com.cbgm.sparrow.core.ui.navigation.AppRoute
import com.cbgm.sparrow.core.ui.presentation.BaseViewModel
import com.cbgm.sparrow.feature.conversationorchestration.domain.usecase.StartRecoveryInvitationUseCase
import com.cbgm.sparrow.feature.invite.domain.usecase.ObservePendingInvitationCountUseCase
import com.cbgm.sparrow.feature.search.domain.model.SemanticSearchState
import com.cbgm.sparrow.feature.search.domain.usecase.ObserveSemanticSearchStateUseCase
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

class MainViewModel(
    observePendingInvitationCount: ObservePendingInvitationCountUseCase,
    observeSemanticSearchState: ObserveSemanticSearchStateUseCase,
    private val startRecoveryInvitation: StartRecoveryInvitationUseCase
) : BaseViewModel() {
    val invitationCount: StateFlow<Int> =
        observePendingInvitationCount()
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(stopTimeoutMillis = 5_000),
                initialValue = 0
            )

    val isMessageSearchAvailable: StateFlow<Boolean> =
        observeSemanticSearchState()
            .map { state -> state is SemanticSearchState.Ready }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(stopTimeoutMillis = 5_000),
                initialValue = false
            )

    /** UI asks for a NEW invitation only after the user approved the replacement. */
    suspend fun sendRecoveryInvitation(peerId: String): Result<Unit> =
        startRecoveryInvitation(peerId)

    fun openMessageSearch() {
        if (isMessageSearchAvailable.value) {
            navigator.navigateTo(AppRoute.MessageSearch)
        }
    }

    fun openInvitations() {
        navigator.navigateTo(AppRoute.Invitations())
    }
}
