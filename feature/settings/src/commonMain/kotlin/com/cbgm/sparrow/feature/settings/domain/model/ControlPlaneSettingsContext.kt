package com.cbgm.sparrow.feature.settings.domain.model

import com.cbgm.sparrow.feature.transport.ControlPlaneEndpointStatus

data class ControlPlaneSettingsContext(
    val statuses: List<ControlPlaneEndpointStatus>,
    val manualBaseUrls: Set<String>,
    val directoryBaseUrls: Set<String>,
    val directoryUrl: String
)
