package com.cbgm.sparrow.feature.settings.presentation.developer.nodes

import com.cbgm.sparrow.core.transport.TransportDiagnosticsProvider
import com.cbgm.sparrow.core.ui.presentation.BaseViewModel
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlin.time.Duration.Companion.milliseconds

class DeveloperNodesViewModel(
    private val transportDiagnosticsProvider: TransportDiagnosticsProvider
) : BaseViewModel() {
    val diagnostics = transportDiagnosticsProvider.diagnostics

    suspend fun refreshTransportDiagnosticsWhileVisible() {
        while (currentCoroutineContext().isActive) {
            transportDiagnosticsProvider.refreshDiagnostics()
            delay(1_000L.milliseconds)
        }
    }

    fun onBackClicked() {
        navigator.popBackStack()
    }
}
