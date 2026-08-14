package com.cbgm.sparrow.presentation.screen

import androidx.lifecycle.viewModelScope
import com.cbgm.sparrow.core.ui.navigation.AppRoute
import com.cbgm.sparrow.core.ui.presentation.BaseViewModel
import com.cbgm.sparrow.feature.contacts.domain.usecase.ObservePendingContactInvitationCountUseCase
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

class MainViewModel(
    observePendingContactInvitationCount: ObservePendingContactInvitationCountUseCase
) : BaseViewModel() {
    val invitationCount: StateFlow<Int> =
        observePendingContactInvitationCount()
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(stopTimeoutMillis = 5_000),
                initialValue = 0
            )

    fun openContactInvitations() {
        if (invitationCount.value > 0) {
            navigator.navigateTo(AppRoute.ContactInvitations)
        }
    }
}
