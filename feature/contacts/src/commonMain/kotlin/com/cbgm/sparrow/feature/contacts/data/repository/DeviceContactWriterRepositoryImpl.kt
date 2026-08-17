package com.cbgm.sparrow.feature.contacts.data.repository

import com.cbgm.sparrow.feature.contacts.data.datasource.DeviceContactWriterDataSource
import com.cbgm.sparrow.feature.contacts.domain.model.device.AddDeviceContactRequest
import com.cbgm.sparrow.feature.contacts.domain.model.device.AddDeviceContactResult
import com.cbgm.sparrow.feature.contacts.domain.repository.DeviceContactWriterRepository

class DeviceContactWriterRepositoryImpl(
    private val dataSource: DeviceContactWriterDataSource
) : DeviceContactWriterRepository {
    override suspend fun addIfNotExists(request: AddDeviceContactRequest): AddDeviceContactResult =
        dataSource.addIfNotExists(request)
}
