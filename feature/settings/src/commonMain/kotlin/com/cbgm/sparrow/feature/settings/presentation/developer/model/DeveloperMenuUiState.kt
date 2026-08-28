package com.cbgm.sparrow.feature.settings.presentation.developer.model

import com.cbgm.sparrow.core.transport.TransportDiagnostics
import com.cbgm.sparrow.feature.settings.domain.model.BuildInfo

data class DeveloperMenuUiState(
    val buildInfo: BuildInfo = BuildInfo("1.0.0", 1, "release", null),
    val transportDiagnostics: TransportDiagnostics = TransportDiagnostics(),
    val savedErrorCount: Int = 0,
    val isClearingLocalData: Boolean = false
)
