package com.cbgm.sparrow.feature.transport.domain.usecase

import com.cbgm.sparrow.feature.transport.domain.model.TransportDiagnostics
import com.cbgm.sparrow.feature.transport.domain.repository.TransportDiagnosticsRepository
import kotlinx.coroutines.flow.StateFlow

/** How the transport is connected and how each node fared, for the developer menu. */
class ObserveTransportDiagnosticsUseCase(
    private val source: TransportDiagnosticsRepository
) {
    operator fun invoke(): StateFlow<TransportDiagnostics> = source.diagnostics
}
