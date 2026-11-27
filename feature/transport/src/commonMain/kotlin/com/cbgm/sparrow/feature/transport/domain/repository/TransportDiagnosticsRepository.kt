package com.cbgm.sparrow.feature.transport.domain.repository

import com.cbgm.sparrow.feature.transport.domain.model.TransportDiagnostics
import kotlinx.coroutines.flow.StateFlow

interface TransportDiagnosticsRepository {
    val diagnostics: StateFlow<TransportDiagnostics>

    suspend fun refreshDiagnostics()
}
