package com.cbgm.sparrow.feature.contacts.domain.usecase

import com.cbgm.sparrow.feature.contacts.domain.model.device.AddDeviceContactRequest
import com.cbgm.sparrow.feature.contacts.domain.model.device.AddDeviceContactResult
import com.cbgm.sparrow.feature.contacts.domain.repository.DeviceContactWriterRepository

class AddDeviceContactUseCase(
    private val repository: DeviceContactWriterRepository
) {
    suspend operator fun invoke(
        displayName: String?,
        phoneNumber: String
    ): AddDeviceContactResult =
        repository.addIfNotExists(
            AddDeviceContactRequest(
                displayName = displayName,
                phoneNumber = phoneNumber
            )
        )
}
