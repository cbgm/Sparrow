package com.cbgm.sparrow.feature.transport.domain.gateway

import com.cbgm.sparrow.feature.transport.domain.model.TransportDiagnostics
import kotlinx.coroutines.flow.StateFlow

interface TransportDiagnosticsSource {
    val diagnostics: StateFlow<TransportDiagnostics>

    suspend fun refreshDiagnostics()
}
