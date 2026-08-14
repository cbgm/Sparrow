package com.cbgm.securechat.feature.contacts.domain.repository

import com.cbgm.securechat.feature.contacts.domain.model.device.DeviceContact

interface DeviceContactsRepository {
    /**
     * Returns all contacts visible to SecureChat.
     *
     * The platform implementation is responsible for
     * permission checks.
     */
    suspend fun getContacts(): Result<List<DeviceContact>>
}
