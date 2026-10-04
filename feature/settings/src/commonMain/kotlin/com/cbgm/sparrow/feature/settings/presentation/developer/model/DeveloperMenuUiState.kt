package com.cbgm.sparrow.feature.settings.presentation.developer.model

import com.cbgm.sparrow.feature.settings.domain.model.BuildInfo
import com.cbgm.sparrow.feature.transport.TransportDiagnostics

data class DeveloperMenuUiState(
    val buildInfo: BuildInfo = BuildInfo("1.0.0", 1, "release", null),
    val transportDiagnostics: TransportDiagnostics = TransportDiagnostics(),
    val savedErrorCount: Int = 0,
    val isClearingLocalData: Boolean = false
)
