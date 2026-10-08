package com.cbgm.sparrow.feature.settings.presentation.developer.mapper

import com.cbgm.sparrow.feature.settings.domain.model.BuildInfo
import com.cbgm.sparrow.feature.settings.presentation.developer.model.DeveloperMenuUiState
import com.cbgm.sparrow.feature.transport.TransportDiagnostics

internal fun TransportDiagnostics.toDeveloperMenuUiState(
    buildInfo: BuildInfo,
    savedErrorCount: Int,
    isClearingLocalData: Boolean
): DeveloperMenuUiState =
    DeveloperMenuUiState(
        buildInfo = buildInfo,
        transportDiagnostics = this,
        savedErrorCount = savedErrorCount,
        isClearingLocalData = isClearingLocalData
    )
