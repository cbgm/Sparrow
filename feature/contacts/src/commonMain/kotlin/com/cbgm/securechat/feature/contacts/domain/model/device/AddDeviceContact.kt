package com.cbgm.securechat.feature.contacts.domain.model.device

data class AddDeviceContactRequest(
    val displayName: String?,
    val phoneNumber: String,
    val email: String? = null,
    val company: String? = null
)

sealed interface AddDeviceContactResult {
    data object Added : AddDeviceContactResult

    data object AlreadyExists : AddDeviceContactResult

    data object PermissionDenied : AddDeviceContactResult

    data object InvalidPhoneNumber : AddDeviceContactResult

    data class Failure(
        val throwable: Throwable
    ) : AddDeviceContactResult
}
