package com.cbgm.sparrow.navigation.presentation.main

import androidx.lifecycle.viewModelScope
import com.cbgm.sparrow.core.ui.navigation.AppRoute
import com.cbgm.sparrow.core.ui.presentation.BaseViewModel
import com.cbgm.sparrow.feature.identity.domain.usecase.ObservePendingRemoteIdentityChangesUseCase
import com.cbgm.sparrow.feature.invite.domain.usecase.ObservePendingInvitationCountUseCase
import com.cbgm.sparrow.feature.search.domain.model.SemanticSearchState
import com.cbgm.sparrow.feature.search.domain.usecase.ObserveSemanticSearchStateUseCase
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

class MainViewModel(
    observePendingInvitationCount: ObservePendingInvitationCountUseCase,
    observeSemanticSearchState: ObserveSemanticSearchStateUseCase,
    observeRecoveryRequests: ObservePendingRemoteIdentityChangesUseCase
) : BaseViewModel() {
    val invitationCount: StateFlow<Int> =
        combine(observePendingInvitationCount(), observeRecoveryRequests()) { invites, recoveries ->
            invites + recoveries.size
        }.stateIn(
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

    fun openMessageSearch() {
        if (isMessageSearchAvailable.value) {
            navigator.navigateTo(AppRoute.MessageSearch)
        }
    }

    fun openInvitations() {
        navigator.navigateTo(AppRoute.Invitations())
    }
}
