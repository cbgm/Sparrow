package com.cbgm.sparrow.feature.contacts.domain.usecase

import com.cbgm.sparrow.feature.contacts.domain.model.ContactPhoneNumberType
import com.cbgm.sparrow.feature.contacts.domain.model.ImportDeviceContactRequest
import com.cbgm.sparrow.feature.contacts.domain.model.ImportDevicePhoneNumber
import com.cbgm.sparrow.feature.contacts.domain.model.device.DevicePhoneNumberType
import com.cbgm.sparrow.feature.contacts.domain.repository.ContactRepository
import com.cbgm.sparrow.feature.contacts.domain.repository.DeviceContactsRepository

/**
 * Imports every contact exposed by the current device.
 *
 * All usable phone numbers are forwarded to persistence.
 */
class ImportDeviceContactsUseCase(
    private val deviceContactsRepository: DeviceContactsRepository,
    private val repository: ContactRepository
) {
    suspend operator fun invoke(): Result<Unit> {
        return runCatching {
            val deviceContacts = deviceContactsRepository.getContacts().getOrThrow()

            deviceContacts.forEach { deviceContact ->
                val phoneNumbers =
                    deviceContact.phoneNumbers
                        .mapNotNull { phoneNumber ->
                            val normalizedValue =
                                phoneNumber.value.trim().takeIf { it.isNotEmpty() }
                                    ?: return@mapNotNull null

                            ImportDevicePhoneNumber(
                                value = normalizedValue,
                                type = phoneNumber.type.toContactPhoneNumberType(),
                                label = phoneNumber.label?.trim()?.takeIf { it.isNotEmpty() }
                            )
                        }.distinctBy { phoneNumber ->
                            phoneNumber.value to phoneNumber.type
                        }

                /*
                 * The current Sparrow contact workflow is based on
                 * phone-capable contacts. Ignore device entries without
                 * any usable number.
                 */
                if (phoneNumbers.isEmpty()) {
                    return@forEach
                }

                repository
                    .importDeviceContact(
                        request =
                            ImportDeviceContactRequest(
                                deviceContactId = deviceContact.id,
                                displayName = deviceContact.displayName,
                                phoneNumbers = phoneNumbers
                            )
                    ).getOrThrow()
            }
        }
    }

    private fun DevicePhoneNumberType.toContactPhoneNumberType(): ContactPhoneNumberType =
        when (this) {
            DevicePhoneNumberType.MOBILE ->
                ContactPhoneNumberType.MOBILE

            DevicePhoneNumberType.WORK_MOBILE ->
                ContactPhoneNumberType.WORK_MOBILE

            DevicePhoneNumberType.HOME ->
                ContactPhoneNumberType.HOME

            DevicePhoneNumberType.WORK ->
                ContactPhoneNumberType.WORK

            DevicePhoneNumberType.MAIN ->
                ContactPhoneNumberType.MAIN

            DevicePhoneNumberType.CUSTOM ->
                ContactPhoneNumberType.CUSTOM

            DevicePhoneNumberType.OTHER ->
                ContactPhoneNumberType.OTHER
        }
}
