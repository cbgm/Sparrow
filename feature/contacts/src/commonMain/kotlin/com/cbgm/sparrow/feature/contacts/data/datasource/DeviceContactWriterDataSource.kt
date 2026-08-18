package com.cbgm.sparrow.feature.contacts.data.datasource

import com.cbgm.sparrow.feature.contacts.domain.model.device.AddDeviceContactRequest
import com.cbgm.sparrow.feature.contacts.domain.model.device.AddDeviceContactResult

interface DeviceContactWriterDataSource {
    suspend fun addIfNotExists(request: AddDeviceContactRequest): AddDeviceContactResult
}
