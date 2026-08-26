package com.cbgm.sparrow.feature.settings.presentation.developer.mapper

import com.cbgm.sparrow.core.transport.TransportDiagnostics
import com.cbgm.sparrow.feature.settings.domain.model.BuildInfo
import com.cbgm.sparrow.feature.settings.presentation.developer.model.DeveloperMenuUiState

internal fun TransportDiagnostics.toUiState(
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
