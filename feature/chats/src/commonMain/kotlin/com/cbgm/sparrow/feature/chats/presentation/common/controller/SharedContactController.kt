package com.cbgm.sparrow.feature.chats.presentation.common.controller

import com.cbgm.sparrow.feature.attachments.domain.model.SharedContact
import com.cbgm.sparrow.feature.contacts.domain.model.device.AddDeviceContactResult
import com.cbgm.sparrow.feature.contacts.domain.usecase.AddDeviceContactUseCase

class SharedContactController(
    private val addDeviceContact: AddDeviceContactUseCase
) {
    suspend fun add(contact: SharedContact): Result<Unit> =
        when (val result = addDeviceContact(contact.displayName, contact.phoneNumber)) {
            AddDeviceContactResult.Added, AddDeviceContactResult.AlreadyExists -> Result.success(
                Unit
            )

            AddDeviceContactResult.PermissionDenied -> failure("Contacts permission is required to add this contact")
            AddDeviceContactResult.InvalidPhoneNumber -> failure("The shared phone number is invalid")
            is AddDeviceContactResult.Failure -> Result.failure(result.throwable)
        }

    private fun failure(message: String): Result<Unit> =
        Result.failure(IllegalStateException(message))
}
