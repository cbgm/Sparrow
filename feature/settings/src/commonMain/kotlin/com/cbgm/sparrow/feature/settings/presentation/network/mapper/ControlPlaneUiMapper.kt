package com.cbgm.sparrow.feature.settings.presentation.network.mapper

import com.cbgm.sparrow.core.transport.ControlPlaneEndpointStatus
import com.cbgm.sparrow.core.transport.ControlPlaneReachability
import com.cbgm.sparrow.feature.settings.presentation.network.model.ControlPlaneDirectoryError
import com.cbgm.sparrow.feature.settings.presentation.network.model.ControlPlaneSettingsError
import com.cbgm.sparrow.feature.settings.presentation.network.model.ControlPlaneSettingsUiState
import com.cbgm.sparrow.feature.settings.presentation.network.model.ControlPlaneUiModel
import com.cbgm.sparrow.feature.settings.presentation.network.model.ControlPlaneUiSource
import com.cbgm.sparrow.feature.settings.presentation.network.model.ControlPlaneUiStatus

internal fun List<ControlPlaneEndpointStatus>.toControlPlaneUiModels(
    manual: Set<String>,
    directory: Set<String>
): List<ControlPlaneUiModel> =
    map { status ->
        ControlPlaneUiModel(
            url = status.endpoint.baseUrl,
            status = status.toControlPlaneUiStatus(),
            source = sourceFor(status.endpoint.baseUrl, manual, directory),
            canRemove = canRemove(status.endpoint.baseUrl, manual, directory)
        )
    }.sortedWith(
        compareBy<ControlPlaneUiModel> { it.status.sortOrder() }
            .thenBy(ControlPlaneUiModel::url)
    )

internal fun List<ControlPlaneUiModel>.toControlPlaneSettingsUiState(
    showAddDialog: Boolean,
    newUrl: String,
    addError: ControlPlaneSettingsError?,
    directoryUrl: String,
    directoryDraft: String,
    directoryError: ControlPlaneDirectoryError?,
    isRefreshing: Boolean,
    isDirectorySyncing: Boolean,
    lastDirectoryCount: Int?
): ControlPlaneSettingsUiState =
    ControlPlaneSettingsUiState(
        entries = this,
        showAddDialog = showAddDialog,
        newUrl = newUrl,
        addError = addError,
        directoryUrl = directoryUrl,
        directoryDraft = directoryDraft,
        directoryError = directoryError,
        isRefreshing = isRefreshing,
        isDirectorySyncing = isDirectorySyncing,
        lastDirectoryCount = lastDirectoryCount
    )

private fun ControlPlaneEndpointStatus.toControlPlaneUiStatus(): ControlPlaneUiStatus =
    when {
        reachability == ControlPlaneReachability.UNREACHABLE -> ControlPlaneUiStatus.UNREACHABLE
        isActive && reachability == ControlPlaneReachability.AVAILABLE -> ControlPlaneUiStatus.ACTIVE
        reachability == ControlPlaneReachability.AVAILABLE -> ControlPlaneUiStatus.AVAILABLE
        else -> ControlPlaneUiStatus.CHECKING
    }

private fun sourceFor(
    url: String,
    manual: Set<String>,
    directory: Set<String>
): ControlPlaneUiSource =
    when {
        url in manual && url in directory -> ControlPlaneUiSource.MANUAL_AND_DIRECTORY
        url in directory -> ControlPlaneUiSource.DIRECTORY
        else -> ControlPlaneUiSource.MANUAL
    }

private fun canRemove(
    url: String,
    manual: Set<String>,
    directory: Set<String>
): Boolean {
    if (url !in manual) return false
    return ((manual - url) + directory).isNotEmpty()
}

private fun ControlPlaneUiStatus.sortOrder(): Int =
    when (this) {
        ControlPlaneUiStatus.ACTIVE -> 0
        ControlPlaneUiStatus.AVAILABLE -> 1
        ControlPlaneUiStatus.CHECKING -> 2
        ControlPlaneUiStatus.UNREACHABLE -> 3
    }
