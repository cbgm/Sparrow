package com.cbgm.sparrow.feature.settings.presentation.network.circuit

import androidx.lifecycle.viewModelScope
import com.cbgm.sparrow.core.ui.presentation.BaseViewModel
import com.cbgm.sparrow.core.util.logging.SparrowLog
import com.cbgm.sparrow.feature.settings.presentation.network.circuit.model.CircuitHopsUiEvent
import com.cbgm.sparrow.feature.settings.presentation.network.circuit.model.CircuitHopsUiState
import com.cbgm.sparrow.feature.settings.presentation.network.circuit.model.CircuitUiStatus
import com.cbgm.sparrow.feature.transport.circuit.CircuitHopCount
import com.cbgm.sparrow.feature.transport.circuit.CircuitStatus
import com.cbgm.sparrow.feature.transport.domain.usecase.ObserveCircuitHopCountUseCase
import com.cbgm.sparrow.feature.transport.domain.usecase.ObserveCircuitStatusUseCase
import com.cbgm.sparrow.feature.transport.domain.usecase.SetCircuitHopCountUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class CircuitHopsViewModel(
    observeCircuitHopCount: ObserveCircuitHopCountUseCase,
    observeCircuitStatus: ObserveCircuitStatusUseCase,
    private val setCircuitHopCount: SetCircuitHopCountUseCase
) : BaseViewModel() {
    private val draftHopCount = MutableStateFlow<Int?>(null)

    val uiState: StateFlow<CircuitHopsUiState> =
        combine(observeCircuitHopCount(), observeCircuitStatus(), draftHopCount) { saved, status, draft ->
            val selected = draft ?: saved
            val uiStatus = status.toUiStatus()
            CircuitHopsUiState(
                selectedHopCount = selected,
                minimumHopCount = CircuitHopCount.MINIMUM,
                maximumHopCount = CircuitHopCount.MAXIMUM,
                status = uiStatus,
                estimatedLatencyMilliseconds =
                    (uiStatus as? CircuitUiStatus.Connected)
                        ?.takeIf { connected -> connected.hopCount != selected }
                        ?.estimateFor(selected)
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLISECONDS), CircuitHopsUiState())

    fun onUiEvent(event: CircuitHopsUiEvent) {
        when (event) {
            CircuitHopsUiEvent.BackClicked -> navigator.popBackStack()
            is CircuitHopsUiEvent.HopCountChanged -> draftHopCount.value = event.hopCount
            CircuitHopsUiEvent.HopCountChosen -> saveDraft()
        }
    }

    private fun saveDraft() {
        val chosen = draftHopCount.value ?: return
        viewModelScope.launch {
            setCircuitHopCount(chosen)
                .onSuccess { draftHopCount.value = null }
                .onFailure { error -> SparrowLog.error("CircuitHopsViewModel", "Hop count could not be saved", error) }
        }
    }

    private fun CircuitStatus.toUiStatus(): CircuitUiStatus =
        when (this) {
            CircuitStatus.Building -> CircuitUiStatus.Building
            is CircuitStatus.Unavailable -> CircuitUiStatus.Unavailable
            is CircuitStatus.Connected -> CircuitUiStatus.Connected(hopCount, requestedHopCount, latencyMilliseconds)
        }

    /** Each hop adds about the same share of the measured round trip. */
    private fun CircuitUiStatus.Connected.estimateFor(hops: Int): Long = latencyMilliseconds * hops / hopCount

    private companion object {
        const val STOP_TIMEOUT_MILLISECONDS = 5_000L
    }
}
