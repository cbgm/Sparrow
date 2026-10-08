package com.cbgm.sparrow.runtime.startup.task

import com.cbgm.sparrow.core.logging.SparrowLog
import com.cbgm.sparrow.presentation.model.AppInitializationDependencies

class SynchronizeDeviceContactsStartupTask(
    initialization: AppInitializationDependencies
) : IdentityReadyStartupTask(initialization) {
    private val logger = SparrowLog.withTag("SynchronizeDeviceContactsStartupTask")

    override val name = "device contact synchronization"

    override suspend fun runAfterIdentityReady() {
        if (!initialization.deviceContactsPermissionChecker.canReadContacts()) {
            logger.info {
                "Device contact sync skipped: READ_CONTACTS is not granted"
            }
            return
        }

        initialization.importDeviceContacts()
            .onSuccess {
                logger.info { "Device contact sync completed" }
            }.onFailure { error ->
                logger.error(error) { "Device contact sync failed" }
            }
    }
}
