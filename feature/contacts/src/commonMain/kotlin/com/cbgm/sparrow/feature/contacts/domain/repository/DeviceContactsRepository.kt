package com.cbgm.sparrow.feature.contacts.domain.repository

import com.cbgm.sparrow.feature.contacts.domain.model.device.DeviceContact

interface DeviceContactsRepository {
    /**
     * Returns all contacts visible to Sparrow.
     *
     * The platform implementation is responsible for
     * permission checks.
     */
    suspend fun getContacts(): Result<List<DeviceContact>>
}
