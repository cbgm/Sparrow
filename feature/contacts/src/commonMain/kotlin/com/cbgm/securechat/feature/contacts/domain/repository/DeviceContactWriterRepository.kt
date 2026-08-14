package com.cbgm.securechat.feature.contacts.domain.repository

import com.cbgm.securechat.feature.contacts.domain.model.device.AddDeviceContactRequest
import com.cbgm.securechat.feature.contacts.domain.model.device.AddDeviceContactResult

interface DeviceContactWriterRepository {
    /**
     * Adds the contact directly to the phone contacts.
     *
     * If the phone number already exists, nothing is added.
     */
    suspend fun addIfNotExists(request: AddDeviceContactRequest): AddDeviceContactResult
}
