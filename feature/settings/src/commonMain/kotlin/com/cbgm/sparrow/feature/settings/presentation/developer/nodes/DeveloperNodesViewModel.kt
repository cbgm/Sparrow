package com.cbgm.sparrow.feature.settings.presentation.developer.nodes

import com.cbgm.sparrow.core.ui.navigation.AppRoute
import com.cbgm.sparrow.core.ui.presentation.BaseViewModel
import com.cbgm.sparrow.feature.transport.domain.usecase.ObserveTransportDiagnosticsUseCase
import com.cbgm.sparrow.feature.transport.domain.usecase.RefreshTransportDiagnosticsUseCase
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlin.time.Duration.Companion.milliseconds

class DeveloperNodesViewModel(
    observeTransportDiagnostics: ObserveTransportDiagnosticsUseCase,
    private val refreshTransportDiagnostics: RefreshTransportDiagnosticsUseCase
) : BaseViewModel() {
    val diagnostics = observeTransportDiagnostics()

    suspend fun refreshTransportDiagnosticsWhileVisible() {
        while (currentCoroutineContext().isActive) {
            refreshTransportDiagnostics()
            delay(1_000L.milliseconds)
        }
    }

    fun onBackClicked() {
        navigator.popBackStack()
    }

    fun onPreferredNodeClicked() {
        navigator.navigateTo(AppRoute.PreferredNode)
    }
}
