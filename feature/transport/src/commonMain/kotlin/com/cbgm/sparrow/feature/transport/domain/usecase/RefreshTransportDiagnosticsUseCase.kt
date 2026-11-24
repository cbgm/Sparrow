package com.cbgm.sparrow.feature.transport.domain.usecase

import com.cbgm.sparrow.feature.transport.domain.gateway.TransportDiagnosticsSource

/** Fetches the node directory anew so the diagnostics show every node. */
class RefreshTransportDiagnosticsUseCase(
    private val source: TransportDiagnosticsSource
) {
    suspend operator fun invoke() = source.refreshDiagnostics()
}
