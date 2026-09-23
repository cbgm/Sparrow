package com.cbgm.sparrow.feature.settings.presentation.network.mapper

import com.cbgm.sparrow.core.transport.ControlPlaneEndpointStatus
import com.cbgm.sparrow.core.transport.ControlPlaneReachability
import com.cbgm.sparrow.feature.settings.presentation.network.model.ControlPlaneDirectoryError
import com.cbgm.sparrow.feature.settings.presentation.network.model.ControlPlaneSettingsError
import com.cbgm.sparrow.feature.settings.presentation.network.model.ControlPlaneSettingsUiState
import com.cbgm.sparrow.feature.settings.presentation.network.model.ControlPlaneUiModel
import com.cbgm.sparrow.feature.settings.presentation.network.model.ControlPlaneUiSource
import com.cbgm.sparrow.feature.settings.presentation.network.model.ControlPlaneUiStatus

/**
 * A Control Plane may be independently configured by the user AND discovered
 * from a directory. Display one entry in each applicable section; a directory
 * entry never masquerades as a manual entry and manual removal stays available.
 * Omit transient node-advertised hints: they are not signed directory members
 * and they were never added manually by the user.
 */
internal fun List<ControlPlaneEndpointStatus>.toControlPlaneUiModels(
    manual: Set<String>,
    directory: Set<String>,
    jsonList: Set<String>
): List<ControlPlaneUiModel> {
    val statusByUrl = associateBy { it.endpoint.baseUrl }

    fun entry(url: String, source: ControlPlaneUiSource): ControlPlaneUiModel =
        ControlPlaneUiModel(
            url = url,
            status = statusByUrl[url]?.toControlPlaneUiStatus() ?: ControlPlaneUiStatus.CHECKING,
            source = source,
            canRemove = source == ControlPlaneUiSource.MANUAL
        )
    val discovered = directory.map { entry(it, ControlPlaneUiSource.DIRECTORY) } +
        (jsonList - directory).map { entry(it, ControlPlaneUiSource.JSON_LIST) }
    val manuallyAdded = manual.map { entry(it, ControlPlaneUiSource.MANUAL) }
    return (discovered + manuallyAdded).sortedWith(
        compareBy<ControlPlaneUiModel> { it.status.sortOrder() }
            .thenBy(ControlPlaneUiModel::url)
    )
}

internal fun List<ControlPlaneUiModel>.toControlPlaneSettingsUiState(
    showAddDialog: Boolean,
    isEditingDirectory: Boolean,
    newUrl: String,
    addSource: com.cbgm.sparrow.feature.settings.presentation.network.model.ControlPlaneAddSource,
    jsonDirectoryUrl: String,
    addError: ControlPlaneSettingsError?,
    directoryUrl: String,
    directoryDraft: String,
    directoryError: ControlPlaneDirectoryError?,
    directoryFailureDetail: String?,
    isRefreshing: Boolean,
    isDirectorySyncing: Boolean,
    lastDirectoryCount: Int?,
    manualRemovalError: String?
): ControlPlaneSettingsUiState =
    ControlPlaneSettingsUiState(
        entries = this,
        showAddDialog = showAddDialog,
        isEditingDirectory = isEditingDirectory,
        newUrl = newUrl,
        addSource = addSource,
        jsonDirectoryUrl = jsonDirectoryUrl,
        addError = addError,
        directoryUrl = directoryUrl,
        directoryDraft = directoryDraft,
        directoryError = directoryError,
        directoryFailureDetail = directoryFailureDetail,
        isRefreshing = isRefreshing,
        isDirectorySyncing = isDirectorySyncing,
        lastDirectoryCount = lastDirectoryCount,
        manualRemovalError = manualRemovalError
    )

private fun ControlPlaneEndpointStatus.toControlPlaneUiStatus(): ControlPlaneUiStatus =
    when {
        reachability == ControlPlaneReachability.UNREACHABLE -> ControlPlaneUiStatus.UNREACHABLE
        isActive && reachability == ControlPlaneReachability.AVAILABLE -> ControlPlaneUiStatus.ACTIVE
        reachability == ControlPlaneReachability.AVAILABLE -> ControlPlaneUiStatus.AVAILABLE
        else -> ControlPlaneUiStatus.CHECKING
    }

private fun ControlPlaneUiStatus.sortOrder(): Int =
    when (this) {
        ControlPlaneUiStatus.ACTIVE -> 0
        ControlPlaneUiStatus.AVAILABLE -> 1
        ControlPlaneUiStatus.CHECKING -> 2
        ControlPlaneUiStatus.UNREACHABLE -> 3
    }
