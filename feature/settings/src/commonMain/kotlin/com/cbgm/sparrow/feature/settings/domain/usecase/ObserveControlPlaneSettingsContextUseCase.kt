package com.cbgm.sparrow.feature.settings.domain.usecase

import com.cbgm.sparrow.core.transport.ControlPlaneConfiguration
import com.cbgm.sparrow.core.transport.ControlPlaneStatusStore
import com.cbgm.sparrow.feature.settings.domain.model.ControlPlaneSettingsContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

class ObserveControlPlaneSettingsContextUseCase(
    private val configuration: ControlPlaneConfiguration,
    private val statusStore: ControlPlaneStatusStore
) {
    operator fun invoke(): Flow<ControlPlaneSettingsContext> =
        combine(
            statusStore.statuses,
            configuration.manualBaseUrls,
            configuration.directoryBaseUrls,
            configuration.directoryUrl
        ) { statuses, manualBaseUrls, directoryBaseUrls, directoryUrl ->
            ControlPlaneSettingsContext(
                statuses = statuses,
                manualBaseUrls = manualBaseUrls,
                directoryBaseUrls = directoryBaseUrls,
                directoryUrl = directoryUrl.orEmpty()
            )
        }
}
