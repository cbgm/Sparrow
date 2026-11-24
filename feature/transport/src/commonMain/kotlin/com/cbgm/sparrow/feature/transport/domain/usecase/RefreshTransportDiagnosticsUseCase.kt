package com.cbgm.sparrow.feature.transport.domain.usecase

import com.cbgm.sparrow.feature.transport.domain.repository.TransportDiagnosticsRepository

/** Fetches the node directory anew so the diagnostics show every node. */
class RefreshTransportDiagnosticsUseCase(
    private val source: TransportDiagnosticsRepository
) {
    suspend operator fun invoke() = source.refreshDiagnostics()
}
