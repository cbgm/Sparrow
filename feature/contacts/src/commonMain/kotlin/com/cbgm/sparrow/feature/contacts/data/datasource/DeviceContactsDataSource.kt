package com.cbgm.sparrow.feature.contacts.data.datasource

import com.cbgm.sparrow.feature.contacts.domain.model.device.DeviceContact

interface DeviceContactsDataSource {
    suspend fun getContacts(): Result<List<DeviceContact>>
}
