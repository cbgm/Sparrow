package com.cbgm.sparrow.feature.transport.domain.usecase

import com.cbgm.sparrow.feature.transport.domain.gateway.TransportDiagnosticsSource
import com.cbgm.sparrow.feature.transport.domain.model.TransportDiagnostics
import kotlinx.coroutines.flow.StateFlow

/** How the transport is connected and how each node fared, for the developer menu. */
class ObserveTransportDiagnosticsUseCase(
    private val source: TransportDiagnosticsSource
) {
    operator fun invoke(): StateFlow<TransportDiagnostics> = source.diagnostics
}
