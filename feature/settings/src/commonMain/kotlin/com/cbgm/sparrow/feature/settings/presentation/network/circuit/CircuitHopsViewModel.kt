package com.cbgm.sparrow.feature.settings.presentation.network.circuit

import androidx.lifecycle.viewModelScope
import com.cbgm.sparrow.core.ui.presentation.BaseViewModel
import com.cbgm.sparrow.core.util.logging.SparrowLog
import com.cbgm.sparrow.feature.settings.presentation.network.circuit.model.CircuitHopsUiEvent
import com.cbgm.sparrow.feature.settings.presentation.network.circuit.model.CircuitHopsUiState
import com.cbgm.sparrow.feature.settings.presentation.network.circuit.model.CircuitUiStatus
import com.cbgm.sparrow.feature.transport.background.ConnectionMode
import com.cbgm.sparrow.feature.transport.circuit.CircuitCount
import com.cbgm.sparrow.feature.transport.circuit.CircuitHopCount
import com.cbgm.sparrow.feature.transport.circuit.CircuitStatus
import com.cbgm.sparrow.feature.transport.circuit.DecoyCount
import com.cbgm.sparrow.feature.transport.domain.usecase.CanConnectWhileClosedUseCase
import com.cbgm.sparrow.feature.transport.domain.usecase.ObserveCircuitCountUseCase
import com.cbgm.sparrow.feature.transport.domain.usecase.ObserveCircuitFallbackUseCase
import com.cbgm.sparrow.feature.transport.domain.usecase.ObserveCircuitHopCountUseCase
import com.cbgm.sparrow.feature.transport.domain.usecase.ObserveCircuitStatusUseCase
import com.cbgm.sparrow.feature.transport.domain.usecase.ObserveConnectionModeUseCase
import com.cbgm.sparrow.feature.transport.domain.usecase.ObserveDecoyCountUseCase
import com.cbgm.sparrow.feature.transport.domain.usecase.SetCircuitCountUseCase
import com.cbgm.sparrow.feature.transport.domain.usecase.SetCircuitFallbackUseCase
import com.cbgm.sparrow.feature.transport.domain.usecase.SetCircuitHopCountUseCase
import com.cbgm.sparrow.feature.transport.domain.usecase.SetConnectionModeUseCase
import com.cbgm.sparrow.feature.transport.domain.usecase.SetDecoyCountUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class CircuitHopsViewModel(
    observeCircuitHopCount: ObserveCircuitHopCountUseCase,
    observeCircuitStatus: ObserveCircuitStatusUseCase,
    observeCircuitFallback: ObserveCircuitFallbackUseCase,
    observeCircuitCount: ObserveCircuitCountUseCase,
    observeConnectionMode: ObserveConnectionModeUseCase,
    observeDecoyCount: ObserveDecoyCountUseCase,
    canConnectWhileClosed: CanConnectWhileClosedUseCase,
    private val setCircuitHopCount: SetCircuitHopCountUseCase,
    private val setCircuitFallback: SetCircuitFallbackUseCase,
    private val setCircuitCount: SetCircuitCountUseCase,
    private val setConnectionMode: SetConnectionModeUseCase,
    private val setDecoyCount: SetDecoyCountUseCase
) : BaseViewModel() {
    private val connectsWhileClosed = canConnectWhileClosed()
    private val draftHopCount = MutableStateFlow<Int?>(null)

    val uiState: StateFlow<CircuitHopsUiState> =
        combine(
            observeCircuitHopCount(),
            observeCircuitStatus(),
            draftHopCount,
            observeCircuitFallback(),
            observeCircuitCount()
        ) { saved, status, draft, sendsDirectly, circuitCount ->
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
                        ?.estimateFor(selected),
                sendsDirectlyWithoutCircuit = sendsDirectly,
                selectedCircuitCount = circuitCount,
                minimumCircuitCount = CircuitCount.MINIMUM,
                maximumCircuitCount = CircuitCount.MAXIMUM,
                connectsWhileClosed = connectsWhileClosed
            )
        }.combine(observeConnectionMode()) { state, mode -> state.copy(connectionMode = mode) }
            .combine(observeDecoyCount()) { state, decoyCount ->
                state.copy(selectedDecoyCount = decoyCount, minimumDecoyCount = DecoyCount.MINIMUM, maximumDecoyCount = DecoyCount.MAXIMUM)
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLISECONDS), CircuitHopsUiState())

    fun onUiEvent(event: CircuitHopsUiEvent) {
        when (event) {
            CircuitHopsUiEvent.BackClicked -> navigator.popBackStack()
            is CircuitHopsUiEvent.HopCountChanged -> draftHopCount.value = event.hopCount
            CircuitHopsUiEvent.HopCountChosen -> saveDraft()
            is CircuitHopsUiEvent.SendDirectlyChanged -> saveFallback(event.sendsDirectly)
            is CircuitHopsUiEvent.CircuitCountChanged -> saveCircuitCount(event.circuitCount)
            is CircuitHopsUiEvent.DecoyCountChanged -> saveDecoyCount(event.decoyCount)
            is CircuitHopsUiEvent.ConnectionModeChanged -> saveConnectionMode(event.mode)
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

    private fun saveCircuitCount(circuitCount: Int) {
        viewModelScope.launch {
            setCircuitCount(circuitCount).onFailure { error ->
                SparrowLog.error("CircuitHopsViewModel", "Circuit count could not be saved", error)
            }
        }
    }

    private fun saveDecoyCount(decoyCount: Int) {
        viewModelScope.launch {
            setDecoyCount(decoyCount).onFailure { error ->
                SparrowLog.error("CircuitHopsViewModel", "Decoy count could not be saved", error)
            }
        }
    }

    private fun saveConnectionMode(mode: ConnectionMode) {
        viewModelScope.launch {
            setConnectionMode(mode).onFailure { error ->
                SparrowLog.error("CircuitHopsViewModel", "Connection mode could not be saved", error)
            }
        }
    }

    private fun saveFallback(sendsDirectly: Boolean) {
        viewModelScope.launch {
            setCircuitFallback(sendsDirectly).onFailure { error ->
                SparrowLog.error("CircuitHopsViewModel", "Fallback choice could not be saved", error)
            }
        }
    }

    private fun CircuitStatus.toUiStatus(): CircuitUiStatus =
        when (this) {
            CircuitStatus.Building -> CircuitUiStatus.Building
            is CircuitStatus.Unavailable -> CircuitUiStatus.Unavailable
            is CircuitStatus.Connected ->
                CircuitUiStatus.Connected(hopCount, requestedHopCount, latencyMilliseconds, circuitsUp, requestedCircuitCount, sessionOperators)
        }

    /** Each hop adds about the same share of the measured round trip. */
    private fun CircuitUiStatus.Connected.estimateFor(hops: Int): Long = latencyMilliseconds * hops / hopCount

    private companion object {
        const val STOP_TIMEOUT_MILLISECONDS = 5_000L
    }
}
