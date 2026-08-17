package com.cbgm.sparrow.feature.contacts.data.repository

import com.cbgm.sparrow.feature.contacts.data.datasource.DeviceContactsDataSource
import com.cbgm.sparrow.feature.contacts.domain.model.device.DeviceContact
import com.cbgm.sparrow.feature.contacts.domain.repository.DeviceContactsRepository

class DeviceContactsRepositoryImpl(
    private val dataSource: DeviceContactsDataSource
) : DeviceContactsRepository {
    override suspend fun getContacts(): Result<List<DeviceContact>> = dataSource.getContacts()
}
