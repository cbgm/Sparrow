package com.cbgm.sparrow.feature.settings.domain.usecase

import com.cbgm.sparrow.feature.settings.domain.model.ControlPlaneSettingsContext
import com.cbgm.sparrow.feature.transport.domain.gateway.ControlPlaneConfiguration
import com.cbgm.sparrow.feature.transport.domain.gateway.ControlPlaneStatusStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

class ObserveControlPlaneSettingsContextUseCase(
    private val configuration: ControlPlaneConfiguration,
    private val statusStore: ControlPlaneStatusStore
) {
    operator fun invoke(): Flow<ControlPlaneSettingsContext> =
        combine(
            combine(statusStore.statuses, configuration.manualBaseUrls, configuration.directoryBaseUrls, ::Triple),
            configuration.directoryUrl,
            configuration.jsonDirectoryUrl,
            configuration.jsonDirectoryBaseUrls
        ) { (statuses, manualBaseUrls, directoryBaseUrls), directoryUrl, jsonDirectoryUrl, jsonDirectoryBaseUrls ->
            ControlPlaneSettingsContext(
                statuses = statuses,
                manualBaseUrls = manualBaseUrls,
                directoryBaseUrls = directoryBaseUrls,
                directoryUrl = directoryUrl,
                jsonDirectoryUrl = jsonDirectoryUrl,
                jsonDirectoryBaseUrls = jsonDirectoryBaseUrls
            )
        }
}
