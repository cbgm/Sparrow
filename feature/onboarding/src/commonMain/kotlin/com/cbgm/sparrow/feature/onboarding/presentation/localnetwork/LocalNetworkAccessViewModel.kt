package com.cbgm.sparrow.feature.onboarding.presentation.localnetwork

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cbgm.sparrow.feature.transport.domain.usecase.ObserveLocalNetworkNeededUseCase
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

/** Whether a configured server is on the local network, so the local network permission is needed. */
class LocalNetworkAccessViewModel(
    observeLocalNetworkNeeded: ObserveLocalNetworkNeededUseCase
) : ViewModel() {
    val needed: StateFlow<Boolean> =
        observeLocalNetworkNeeded().stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLISECONDS), false)

    private companion object {
        const val STOP_TIMEOUT_MILLISECONDS = 5_000L
    }
}
