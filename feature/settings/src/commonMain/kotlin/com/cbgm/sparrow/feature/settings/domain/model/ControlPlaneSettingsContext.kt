package com.cbgm.sparrow.feature.settings.domain.model

import com.cbgm.sparrow.feature.transport.domain.model.ControlPlaneEndpointStatus

data class ControlPlaneSettingsContext(
    val statuses: List<ControlPlaneEndpointStatus>,
    val manualBaseUrls: Set<String>,
    val directoryBaseUrls: Set<String>,
    val directoryUrl: String?,
    val jsonDirectoryUrl: String?,
    val jsonDirectoryBaseUrls: Set<String>
) {
    companion object {
        val EMPTY =
            ControlPlaneSettingsContext(
                statuses = emptyList(),
                manualBaseUrls = emptySet(),
                directoryBaseUrls = emptySet(),
                directoryUrl = null,
                jsonDirectoryUrl = null,
                jsonDirectoryBaseUrls = emptySet()
            )
    }
}
